package nx1125.simulator.windows;

import nx1125.simulator.elastic.LinearElasticSimulator;
import nx1125.simulator.simulation.Simulator;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SlidersDialog extends JDialog {

    private static final double PROPORTION = 1000.0;

    private static final double MIN_CONSTANT = 0;
    private static final double MAX_CONSTANT = 2000;

    private static final double MIN_RADIUS = 0;
    private static final double MAX_RADIUS = 0.5;

    private static final double MIN_FRICTION = 0;
    private static final double MAX_FRICTION = 5;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JSlider mRadius;
    private JSlider mFriction;
    private JSlider mConstant;
    private JCheckBox mChangeFrictionAccordingToConstantCheckBox;
    private JLabel mRadiusLabel;
    private JLabel mConstantLabel;
    private JLabel mFrictionLabel;

    private SimulationDialog mSimulationDialog;

    private LinearElasticSimulator mLinearElasticSimulator;

    public SlidersDialog(SimulationDialog dialog) {
        mSimulationDialog = dialog;

        Simulator simulator = dialog.getSimulator();
        if (simulator instanceof LinearElasticSimulator) {
            mLinearElasticSimulator = (LinearElasticSimulator) simulator;
        } else {
            setVisible(false);
            return;
        }

        setContentPane(contentPane);
        setModal(false);
        getRootPane().setDefaultButton(buttonOK);

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        mConstant.addChangeListener(e -> {
            mLinearElasticSimulator.setConstant(getDoubleOf(mConstant, "elastic constant"));
            mConstantLabel.setText(Double.toString(mLinearElasticSimulator.getElasticConstant()));
        });
        mFriction.addChangeListener(e -> updateFriction());
        mRadius.addChangeListener(e -> {
            mLinearElasticSimulator.setRadius(getDoubleOf(mRadius, "resting distance"));
            mRadiusLabel.setText(Double.toString(mLinearElasticSimulator.getRestingDistance()));
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // mChangeFrictionAccordingToConstantCheckBox.addActionListener(e -> mLinearElasticSimulator.setFriction(Math.sqrt(getDoubleOf(mFriction, "friction") / (4 * getDoubleOf(mConstant, "constant")))));

        setValueOf(MIN_CONSTANT, mLinearElasticSimulator.getElasticConstant(), mConstant, MAX_CONSTANT);
        setValueOf(MIN_FRICTION, mLinearElasticSimulator.getVelocityFriction(), mFriction, MAX_FRICTION);
        setValueOf(MIN_RADIUS, mLinearElasticSimulator.getRestingDistance(), mRadius, MAX_RADIUS);

        pack();

        setLocationRelativeTo(dialog);

        setAlwaysOnTop(true);
    }

    private void updateFriction() {
        if (!mChangeFrictionAccordingToConstantCheckBox.isSelected()) {
            mLinearElasticSimulator.setFriction(getDoubleOf(mFriction, "friction"));
            mFrictionLabel.setText(Double.toString(mLinearElasticSimulator.getVelocityFriction()));
        }
    }

    private void onCancel() {
        // add your code here if necessary
        mSimulationDialog.setVisible(false);

        dispose();
    }

    public static double getDoubleOf(JSlider slider, String type) {
        double value = slider.getValue() / PROPORTION;

        System.out.println("Setting " + type + " to " + value);
        return value;
    }

    public static void setValueOf(double min, double value, JSlider slider, double max) {
        slider.setMinimum((int) (min * PROPORTION));
        slider.setMaximum((int) (max * PROPORTION));

        slider.setValue((int) (value * PROPORTION));

        System.out.println("Slider: [" + slider.getMinimum() + ", " + slider.getMaximum() + "]: " + slider.getValue());
    }
}
