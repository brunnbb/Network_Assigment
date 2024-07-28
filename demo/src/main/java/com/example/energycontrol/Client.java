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
    static final int SERVER_PORT = 9876;
    static final String SERVER_ADDRESS = "localhost";
    static HashMap<String, String> hashmap;
    static ObjectMapper mapper;
    static Timer timer;

    public static void main(String[] args) {
        // Reference map
        mapper = new ObjectMapper();
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

        // Frame
        JFrame frame = new JFrame("Energy control");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 500);
        frame.setLayout(null);
        frame.getContentPane().setBackground(new Color(151, 158, 200));
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel middlePanel = createMiddlePanel();
        JPanel timer = createTimer(middlePanel);
        JPanel buttons = createBottomButtons(middlePanel);

        frame.add(timer);
        frame.add(middlePanel);
        frame.add(buttons);
        frame.setVisible(true);

    }

    public static JPanel createTimer(JPanel middlePanel) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(151, 158, 200));
        panel.setBounds(30, 40, 470, 50);
        panel.setOpaque(true);

        JButton timerButton = new JButton("Start Timer");
        timerButton.setPreferredSize(new Dimension(150, 40));
        timerButton.setFocusable(false);

        JButton stopTimerButton = new JButton("Stop Timer");
        stopTimerButton.setPreferredSize(new Dimension(150, 40));
        stopTimerButton.setFocusable(false);
        stopTimerButton.setEnabled(false);

        JButton getAllButton = new JButton("Get all status");
        getAllButton.setPreferredSize(new Dimension(150, 40));
        getAllButton.setFocusable(false);

        timerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (timer != null) {
                    timer.cancel();
                }

                timer = new Timer();

                String input = JOptionPane.showInputDialog(null, "Define timer rate (in seconds)", "Timer", -1);
                if (input == null || input.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "The rate cannot be empty", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    int value = Integer.parseInt(input);
                    if (value < 0) {
                        throw new NumberFormatException();
                    }

                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            System.out.println(value);
                            for (String item : hashmap.keySet()) {
                                getStatus(item, middlePanel);
                            }
                        }
                    };

                    timer.scheduleAtFixedRate(task, value * 1000, value * 1000);
                    stopTimerButton.setEnabled(true);

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "The rate needs to be a posite number", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stopTimerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                timer.cancel();
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

    public static JPanel createMiddlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 3, 10, 10));
        panel.setBounds(30, 130, 870, 200);

        for (String key : hashmap.keySet()) {
            ButtonGroup buttonGroup = new ButtonGroup();
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel(key);
            JRadioButton onButton = new JRadioButton("On");
            JRadioButton offButton = new JRadioButton("Off");
            onButton.setFocusable(false);
            offButton.setFocusable(false);
            onButton.setEnabled(false);
            offButton.setEnabled(false);
            onButton.setActionCommand("on");
            offButton.setActionCommand("off");
            buttonGroup.add(onButton);
            buttonGroup.add(offButton);

            // Inicial default state
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
        panel.setBackground(new Color(151, 158, 200));
        panel.setBounds(30, 370, 460, 50);
        panel.setOpaque(true);

        JButton set = new JButton("Change status");
        set.setPreferredSize(new Dimension(150, 40));
        set.setFocusable(false);

        JButton get = new JButton("Get status");
        get.setPreferredSize(new Dimension(150, 40));
        get.setFocusable(false);

        String[] list = new String[9];
        int i = 0;
        for (String item : hashmap.keySet()) {
            list[i] = item;
            i++;
        }
        JComboBox<String> comboBox = new JComboBox<String>(list);

        set.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String locate = (String) comboBox.getSelectedItem();
                String value = hashmap.get(locate);
                String newValue = value.equals("off") ? "on" : "off";
                hashmap.put(locate, newValue);
                setStatus(locate, newValue, middlePanel);
            }

        });

        get.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String locate = (String) comboBox.getSelectedItem();
                getStatus(locate, middlePanel);

            }

        });

        panel.add(set);
        panel.add(get);
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
            socket.send(sendPacket);

            // To receive server response
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            JsonNode responseNode = mapper.readTree(response);
            String status = responseNode.get("status").asText();

            // Updates GUI
            updateStatusRadioButtons(locate, status, middlePanel);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Magia negra
    public static void updateStatusRadioButtons(String locate, String status, JPanel middlePanel) {
        // Itera sobre os componentes do painel principal
        for (Component component : middlePanel.getComponents()) {
            // Verifica se o componente é um JPanel
            if (component instanceof JPanel) {
                JPanel statusPanel = (JPanel) component;
                // Itera sobre os componentes do painel de status
                for (Component subComponent : statusPanel.getComponents()) {
                    if (subComponent instanceof JLabel) {
                        JLabel label = (JLabel) subComponent;
                        // Verifica se o JLabel contém o texto correspondente à localização
                        if (label.getText().equals(locate)) {
                            // Itera sobre os componentes do painel de status para encontrar os JRadioButton
                            for (Component radioComponent : statusPanel.getComponents()) {
                                if (radioComponent instanceof JRadioButton) {
                                    JRadioButton radioButton = (JRadioButton) radioComponent;
                                    // Atualiza o estado dos JRadioButtons com base no status
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
