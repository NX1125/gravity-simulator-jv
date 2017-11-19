package nx1125.simulator.windows;

import nx1125.simulator.elastic.ElasticSimulation;
import nx1125.simulator.gravity.GravitySimulation;
import nx1125.simulator.simulation.Simulation;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SimulationConstantsEditorDialog extends JDialog {

    private final Simulation mSimulation;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField mPermittivity;
    private JTextField mUniverseTime;
    private JTextField mPermeability;
    private JTextField mGravity;
    private JTextField mStatesByCycle;
    private JTextField mFrameRate;
    private JTextField mStatesCount;
    private JTextField mElasticConstant;
    private JTextField mElasticRest;
    private JTextField mFriction;

    public SimulationConstantsEditorDialog(Simulation simulation) {
        mSimulation = simulation;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        if (simulation instanceof GravitySimulation) {
            GravitySimulation gravitySimulation = (GravitySimulation) simulation;

            setValueOf(mGravity, gravitySimulation.getGravityConstant());
            setValueOf(mPermeability, gravitySimulation.getPermeabilityConstant());
            setValueOf(mPermittivity, gravitySimulation.getPermittivityConstant());
        } else if (simulation instanceof ElasticSimulation) {
            ElasticSimulation elasticSimulation = (ElasticSimulation) simulation;

            setValueOf(mElasticConstant, elasticSimulation.getElasticConstant());
            setValueOf(mElasticRest, elasticSimulation.getRestingRadius());
            setValueOf(mFriction, elasticSimulation.getFrictionConstant());
        }

        setValueOf(mUniverseTime, mSimulation.getTimeInterval());
        setValueOf(mStatesByCycle, mSimulation.getStatesPerCycle());
        setValueOf(mFrameRate, mSimulation.getFrameRate());
        setValueOf(mStatesCount, mSimulation.getStatesCount());

        pack();
    }

    private void onOK() {
        // add your code here
        try {
            if (mSimulation instanceof GravitySimulation) {
                GravitySimulation gravitySimulation = (GravitySimulation) mSimulation;

                gravitySimulation.setGravityConstant(getDoubleOf(mGravity));
                gravitySimulation.setPermeabilityConstant(getDoubleOf(mPermeability));
                gravitySimulation.setPermittivityConstant(getDoubleOf(mPermittivity));
            } else if (mSimulation instanceof ElasticSimulation) {
                ElasticSimulation elasticSimulation = (ElasticSimulation) mSimulation;

                elasticSimulation.setElasticConstant(getDoubleOf(mElasticConstant));
                elasticSimulation.setRestingRadius(getDoubleOf(mElasticRest));
                elasticSimulation.setFriction(getDoubleOf(mFriction));
            }

            mSimulation.setTimeInterval(getDoubleOf(mUniverseTime));
            mSimulation.setFrameRate(getIntegerOf(mFrameRate));
            mSimulation.setStatesByCycle(getIntegerOf(mStatesByCycle));
            mSimulation.setStatesCount(getIntegerOf(mStatesCount));

            dispose();
        } catch (ClassCastException | NullPointerException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "There is an invalid field");
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private static void setValueOf(JTextField field, double value) {
        field.setText(Double.toString(value));
    }

    private static void setValueOf(JTextField field, int value) {
        field.setText(Integer.toString(value));
    }

    private static double getDoubleOf(JTextField field) {
        return Double.parseDouble(field.getText());
    }

    private static int getIntegerOf(JTextField field) {
        return Integer.parseInt(field.getText());
    }
}
