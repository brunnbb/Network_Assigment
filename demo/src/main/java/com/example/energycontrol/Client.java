package com.example.energycontrol;

import java.util.*;
import java.util.Timer;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.fasterxml.jackson.databind.*;

public class Client {
    private static final int SERVER_PORT = 49300;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int MAX_ATTEMPTS = 4;
    private static final Color BACKGROUND_COLOR = new Color(151, 158, 200);
    private static final int SOCKET_MAX_DELAY = 2000;
    static HashMap<String, String> hashmap;
    static ObjectMapper mapper;
    static Timer timer;

    public static void main(String[] args) {
        mapper = new ObjectMapper();
        initializeHashmap();

        JFrame frame = createFrame();
        JPanel middlePanel = createMiddlePanel();
        JPanel timer = createTimer(middlePanel);
        JPanel buttons = createBottomButtons(middlePanel);

        frame.add(timer);
        frame.add(middlePanel);
        frame.add(buttons);
        frame.setVisible(true);

    }

    private static void initializeHashmap() {
        hashmap = new HashMap<>();
        hashmap.put("luz_guarita", "off");
        hashmap.put("ar_guarita", "off");
        hashmap.put("luz_estacionamento", "off");
        hashmap.put("luz_galpao_externo", "off");
        hashmap.put("luz_galpao_interno", "off");
        hashmap.put("luz_escritorios", "off");
        hashmap.put("ar_escritorios", "off");
        hashmap.put("luz_sala_reunioes", "off");
        hashmap.put("ar_sala_reunioes", "off");
    }

    public static JFrame createFrame() {
        JFrame frame = new JFrame("Energy control");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 500);
        frame.setLayout(null);
        frame.getContentPane().setBackground(BACKGROUND_COLOR);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        return frame;
    }

    private static JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(150, 40));
        button.setFocusable(false);
        return button;
    }

    public static JPanel createTimer(JPanel middlePanel) {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBounds(30, 40, 470, 50);
        panel.setOpaque(true);

        JButton timerButton = createButton("Start Timer");
        JButton stopTimerButton = createButton("Stop Timer");
        stopTimerButton.setEnabled(false);
        JButton getAllButton = createButton("Get all status");

        timerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                String input = JOptionPane.showInputDialog(null, "Define timer rate (in seconds)", "Timer", -1);
                if (input == null || input.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "The rate cannot be empty", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    int value = Integer.parseInt(input);
                    if (value <= 0) {
                        throw new NumberFormatException();
                    }

                    if (timer != null) {
                        timer.cancel();
                    }

                    timer = new Timer();

                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            System.out.println("Timer rate: " + value);
                            for (String item : hashmap.keySet()) {
                                getStatus(item, middlePanel);
                            }
                        }
                    };

                    timer.scheduleAtFixedRate(task, value * 1000, value * 1000);
                    stopTimerButton.setEnabled(true);

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "The rate needs to be a positive number", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stopTimerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (timer != null) {
                    timer.cancel();
                }
                stopTimerButton.setEnabled(false);
            }

        });

        getAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (String item : hashmap.keySet()) {
                    getStatus(item, middlePanel);
                }
            }
        });

        panel.add(timerButton);
        panel.add(stopTimerButton);
        panel.add(getAllButton);
        return panel;
    }

    private static JRadioButton createRadioButton(String text, String actionCommand) {
        JRadioButton radioButton = new JRadioButton(text);
        radioButton.setFocusable(false);
        radioButton.setEnabled(false);
        radioButton.setActionCommand(actionCommand);
        return radioButton;
    }

    public static JPanel createMiddlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 3, 10, 10));
        panel.setBounds(30, 130, 870, 200);

        for (String key : hashmap.keySet()) {
            ButtonGroup buttonGroup = new ButtonGroup();
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel(key);
            JRadioButton onButton = createRadioButton("On", "on");
            JRadioButton offButton = createRadioButton("Off", "off");
            buttonGroup.add(onButton);
            buttonGroup.add(offButton);

            if (hashmap.get(key).equals("on")) {
                onButton.setSelected(true);
            } else {
                offButton.setSelected(true);
            }

            statusPanel.add(onButton);
            statusPanel.add(offButton);
            statusPanel.add(label);
            panel.add(statusPanel);
        }
        return panel;
    }

    public static JPanel createBottomButtons(JPanel middlePanel) {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBounds(30, 370, 460, 50);
        panel.setOpaque(true);

        JButton setButton = createButton("Change status");
        JButton getButton = createButton("Get status");
        JComboBox<String> comboBox = new JComboBox<>(hashmap.keySet().toArray(new String[0]));

        setButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String locate = (String) comboBox.getSelectedItem();
                String value = hashmap.get(locate);
                String newValue = value.equals("off") ? "on" : "off";
                hashmap.put(locate, newValue);
                setStatus(locate, newValue, middlePanel);
            }

        });

        getButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String locate = (String) comboBox.getSelectedItem();
                getStatus(locate, middlePanel);

            }

        });

        panel.add(setButton);
        panel.add(getButton);
        panel.add(comboBox);

        return panel;
    }

    public static void setStatus(String locate, String value, JPanel middlePanel) {
        try (DatagramSocket socket = new DatagramSocket()) {
            JsonNode messageNode = mapper.createObjectNode()
                    .put("command", "set")
                    .put("locate", locate)
                    .put("value", value);
            byte[] sendData = mapper.writeValueAsString(messageNode).getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            socket.send(sendPacket);

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void getStatus(String locate, JPanel middlePanel) {

        try (DatagramSocket socket = new DatagramSocket()) {
            JsonNode messageNode = mapper.createObjectNode()
                    .put("command", "get")
                    .put("locate", locate);
            byte[] sendData = mapper.writeValueAsString(messageNode).getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            boolean responseReceived = false;

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                try {
                    socket.send(sendPacket);

                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    socket.setSoTimeout(SOCKET_MAX_DELAY);

                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    JsonNode responseNode = mapper.readTree(response);
                    String status = responseNode.get("status").asText();
                    responseReceived = true;

                    updateStatusRadioButtons(locate, status, middlePanel);
                    return;

                } catch (SocketTimeoutException e) {
                    System.out.println("No answer received from server");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "I/O error: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

            if (!responseReceived) {
                JOptionPane.showMessageDialog(null, "The server did not respond after " + MAX_ATTEMPTS + " attempts.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void updateStatusRadioButtons(String locate, String status, JPanel middlePanel) {
        for (Component component : middlePanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel statusPanel = (JPanel) component;
                for (Component subComponent : statusPanel.getComponents()) {
                    if (subComponent instanceof JLabel) {
                        JLabel label = (JLabel) subComponent;
                        if (label.getText().equals(locate)) {
                            for (Component radioComponent : statusPanel.getComponents()) {
                                if (radioComponent instanceof JRadioButton) {
                                    JRadioButton radioButton = (JRadioButton) radioComponent;
                                    if (radioButton.getActionCommand().equals(status)) {
                                        radioButton.setSelected(true);
                                    } else {
                                        radioButton.setSelected(false);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

}
