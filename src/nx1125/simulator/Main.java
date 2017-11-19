package nx1125.simulator;

import nx1125.simulator.windows.MainWindow;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        MainWindow window = new MainWindow();

        window.pack();
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}
