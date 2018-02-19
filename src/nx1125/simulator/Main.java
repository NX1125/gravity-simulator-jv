package nx1125.simulator;

import nx1125.simulator.simulation.Planet;
import nx1125.simulator.windows.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {

    private static double x0 = 0.5;
    private static double x1 = 1.5;
    private static double a0 = -5.5;
    private static double a1 = 100;
    private static double h = 1.0;

    private static double sinkForce(double r) {
        return a0 / (r * r);
    }

    private static double backfireForce(double r) {
        return force(r, x0, a1);
    }

    private static double brokenForce(double r) {
        return 0.0; // force(r, x1, -a1);
    }

    private static double force(double r, double x0, double a) {
        // r -= x0;
        return (a * r) / Math.pow(r * r + h * h, 1.5);
    }

    private static boolean testPlotter() {
        Plotter plotter = new Plotter();

        plotter.addFunction(Main::backfireForce, Color.blue);
        plotter.addFunction(Main::sinkForce, Color.green);
//        plotter.addFunction(Main::brokenForce, Color.pink);
        plotter.addFunction(x -> sinkForce(x) + backfireForce(x), Color.black);

        JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(plotter);

        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        return true;
    }

    public static void main(String[] args) {
        // if (testPlotter()) return;

        MainWindow window = new MainWindow();

        window.pack();
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);

        int count = FrameRateThread.INNER_STATES_COUNT;

        int length = 1000;

        float offset = -length / 2.0f;
        float step = (float) length / count;

        float protonMassProportion = 1836;

        double dangle = 2 * Math.PI / count;
        double angle = 0;
        double eRadius = 25;
        double pRadius = 10;
        double nRadius = 5;

        for (int i = 0; i < count; i++) {
            Planet planet = new Planet();

            planet.setLocation(offset, 0);

            planet.setRadius(1);

            planet.setCharge(-1);
            planet.setMass(1);

            window.addPlanet(planet);

            offset += step;
        }

//        for (int i = 0; i < count; i++) {
//            Planet e = new Planet();
//            Planet p = new Planet();
//            Planet n = new Planet();
//
//            double cos = Math.cos(angle);
//            double sin = Math.sin(angle);
//
//            e.setLocation(eRadius * cos, eRadius * sin);
//            p.setLocation(pRadius * cos, pRadius * sin);
//            n.setLocation(nRadius * cos, nRadius * sin);
//
//            e.setCharge(-1);
//            p.setCharge(1);
//            n.setCharge(0);
//
//            e.setMass(1);
//            p.setMass(protonMassProportion);
//            n.setMass(protonMassProportion);
//
//            angle += dangle;
//
//            addPlanet(e);
//            addPlanet(n);
//            addPlanet(p);
//        }
    }
}
