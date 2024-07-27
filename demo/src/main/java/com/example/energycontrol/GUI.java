package com.example.energycontrol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;

public class GUI {
    public static void main(String[] args) {

        JFrame frame = new JFrame("Titulo");
        frame.setSize(950, 500);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel1 = new JPanel();
        panel1.setBounds(30, 40, 200, 50);
        panel1.setBackground(Color.red);

        JPanel panel2 = new JPanel();
        panel2.setBounds(30, 130, 870, 200);
        panel2.setBackground(Color.green);

        JPanel panel3 = new JPanel();
        panel3.setBounds(30, 370, 400, 50);
        panel3.setBackground(Color.blue);

        frame.add(panel1);
        frame.add(panel2);
        frame.add(panel3);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
