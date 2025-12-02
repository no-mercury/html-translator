package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TranslatorGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TranslatorGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("HTML Translator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 400); // увеличили высоту под логотип и футер
        frame.setResizable(false);
        frame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0; // счетчик строк

        ImageIcon icon = null;
        try {
            icon = new ImageIcon(TranslatorGUI.class.getResource("/logo.png"));
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImg);

            frame.setIconImage(scaledImg);

        } catch (Exception e) {
            System.out.println("Логотип не найден в ресурсах!");
        }

        if(icon != null) {
            JLabel logoLabel = new JLabel(icon);
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.CENTER;
            frame.add(logoLabel, gbc);
        }


        JLabel apiLabel = new JLabel("DeepL API Key:");
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        frame.add(apiLabel, gbc);

        JTextField apiField = new JTextField();
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 2;
        frame.add(apiField, gbc);
        row++;


        JLabel inputLabel = new JLabel("Input File:");
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        frame.add(inputLabel, gbc);

        JTextField inputField = new JTextField();
        gbc.gridx = 1; gbc.gridy = row;
        frame.add(inputField, gbc);

        JButton inputButton = new JButton("Browse...");
        gbc.gridx = 2; gbc.gridy = row;
        frame.add(inputButton, gbc);
        inputButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(frame);
            if(res == JFileChooser.APPROVE_OPTION) {
                inputField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        row++;

        JLabel outputLabel = new JLabel("Output Folder:");
        gbc.gridx = 0; gbc.gridy = row;
        frame.add(outputLabel, gbc);

        JTextField outputField = new JTextField();
        gbc.gridx = 1; gbc.gridy = row;
        frame.add(outputField, gbc);

        // Задаем папку приложения по умолчанию
        String currentDir = new File(".").getAbsoluteFile().getParent();
        outputField.setText(currentDir);

        JButton outputButton = new JButton("Browse...");
        gbc.gridx = 2; gbc.gridy = row;
        frame.add(outputButton, gbc);
        outputButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Output Folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int res = chooser.showOpenDialog(frame);
            if(res == JFileChooser.APPROVE_OPTION) {
                outputField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        row++;

        JLabel langLabel = new JLabel("Target Language:");
        gbc.gridx = 0; gbc.gridy = row;
        frame.add(langLabel, gbc);

        String[] languages = {"EN", "UK", "ES", "RU", "PT"};
        JComboBox<String> langCombo = new JComboBox<>(languages);
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 2;
        frame.add(langCombo, gbc);
        row++;

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        frame.add(progressBar, gbc);
        row++;

        JButton translateButton = new JButton("Translate");
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1;
        frame.add(translateButton, gbc);
        row++;

        translateButton.addActionListener(e -> {
            String apiKey = apiField.getText();
            String input = inputField.getText();
            String outputFolder = outputField.getText();
            String targetLang = (String) langCombo.getSelectedItem();

            if(apiKey.isEmpty() || input.isEmpty() || outputFolder.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Все поля должны быть заполнены!");
                return;
            }

            translateButton.setEnabled(false);

            new Thread(() -> {
                try {
                    String translated = Main.translateHTML(apiKey, input, targetLang, (current, total) -> {
                        int percent = (int)((current*100.0)/total);
                        SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                    });

                    File inFile = new File(input);
                    String outFile = Paths.get(outputFolder, inFile.getName()).toString();
                    Files.write(Paths.get(outFile), translated.getBytes());

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Перевод завершен!\nФайл: " + outFile);
                        progressBar.setValue(0);
                        translateButton.setEnabled(true);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Ошибка: " + ex.getMessage());
                        translateButton.setEnabled(true);
                    });
                }
            }).start();
        });

        JLabel footer = new JLabel("Created by Denwer.", SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        frame.add(footer, gbc);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
