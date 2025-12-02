package org.example;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class Main {

    private static final OkHttpClient http = new OkHttpClient();

    public static String translateHTML(String apiKey, String input, String targetLang, ProgressCallback callback) throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(input)), StandardCharsets.UTF_8);

        Map<String, String> placeholders = extractTplVars(html);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            html = html.replace(e.getValue(), e.getKey());
        }

        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false); // сохраняем табы и переносы

        List<TextNode> textNodes = new ArrayList<>();
        collectTextNodes(doc.body(), textNodes);

        int total = textNodes.size();
        for (int i = 0; i < total; i++) {
            TextNode tn = textNodes.get(i);
            String originalText = tn.text().trim();
            if(!originalText.isEmpty()) {
                // Разбиваем на куски, если текст большой
                List<String> chunks = splitText(originalText, 4800);
                StringBuilder translated = new StringBuilder();
                for (String chunk : chunks) {
                    translated.append(translateChunk(apiKey, chunk, targetLang));
                }
                tn.text(translated.toString());
            }
            if (callback != null) callback.onProgress(i + 1, total);
        }

        String translatedHtml = doc.body().html(); // только содержимое body, без <body>
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            translatedHtml = translatedHtml.replace(e.getKey(), e.getValue());
        }

        return translatedHtml;
    }

    // Рекурсивный сбор всех текстовых узлов
    private static void collectTextNodes(Node node, List<TextNode> list) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode) {
                list.add((TextNode) child);
            } else if (!child.nodeName().equals("script") && !child.nodeName().equals("style")) {
                collectTextNodes(child, list);
            }
        }
    }

    // Разбивка текста на куски
    private static List<String> splitText(String text, int maxChunk) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += maxChunk) {
            chunks.add(text.substring(i, Math.min(text.length(), i + maxChunk)));
        }
        return chunks;
    }

    // Метод перевода через DeepL
    public static String translateChunk(String apiKey, String text, String targetLang) throws Exception {
        String url = "https://api.deepl.com/v2/translate";
        RequestBody body = new FormBody.Builder()
                .add("auth_key", apiKey)
                .add("text", text)
                .add("target_lang", targetLang)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = http.newCall(request).execute();
        String json = response.body().string();

        JSONObject obj = new JSONObject(json);
        JSONArray arr = obj.getJSONArray("translations");
        return arr.getJSONObject(0).getString("text");
    }

    // Временное извлечение шаблонных переменных
    public static Map<String, String> extractTplVars(String text) {
        Map<String, String> vars = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile(
                "\\{[^}]+\\}"        // {var}, {if}, {include ...}
                        + "|\\[\\[[^\\]]+]]" // [[var]]
                        + "|\\{%[^%]+%}"     // {% var %}
        );
        Matcher matcher = pattern.matcher(text);
        int i = 0;
        while (matcher.find()) {
            String found = matcher.group();
            String placeholder = "__TPLVAR_" + i++ + "__";
            vars.put(placeholder, found);
        }
        return vars;
    }

    public interface ProgressCallback {
        void onProgress(int current, int total);
    }
}
