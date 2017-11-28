package nx1125.simulator.windows;

import nx1125.simulator.components.EditSimulatorComponent;
import nx1125.simulator.elastic.LinearElasticSimulation;
import nx1125.simulator.gravity.GravitySimulation;
import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.Simulator;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.text.NumberFormat;
import java.util.Arrays;

public class MainWindow extends JFrame {

    private JPanel mSimulationPanel;

    private JList<Planet> mPlanetJList;

    private JButton mAddButton;
    private JButton mRemoveButton;

    private JFormattedTextField mMassField;
    private JFormattedTextField mChargeField;
    private JFormattedTextField mRadiusField;

    private JFormattedTextField mVYField;
    private JFormattedTextField mVXField;

    private JFormattedTextField mXField;
    private JFormattedTextField mYField;

    private JButton mUpdateButton;
    private JButton mRevertButton;

    private JPanel mRootPanel;
    private JButton mSimulateButton;

    private Planet mSelectedPlanet;

    private DefaultListModel<Planet> mPlanetDefaultListModel = new DefaultListModel<>();

    private EditSimulatorComponent mEditSimulatorComponent;

    private float mPlanetTranslateReferenceX;
    private float mPlanetTranslateReferenceY;

    private Simulation mSimulation;

    private File mOriginalFile;

    private JFileChooser mFileChooser = new JFileChooser();

    private boolean mEdited = false;

    public MainWindow() {
        mSimulation = new LinearElasticSimulation();

        System.out.println("Simulation type: " + mSimulation.getClass());

        setContentPane(mRootPanel);

        mEditSimulatorComponent = new EditSimulatorComponent();

        DefaultFormatterFactory decimalFactory = new DefaultFormatterFactory(new NumberFormatter(NumberFormat.getNumberInstance()));

        mMassField.setFormatterFactory(decimalFactory);
        mChargeField.setFormatterFactory(decimalFactory);
        mRadiusField.setFormatterFactory(decimalFactory);

        mVYField.setFormatterFactory(decimalFactory);
        mVXField.setFormatterFactory(decimalFactory);

        mXField.setFormatterFactory(decimalFactory);
        mYField.setFormatterFactory(decimalFactory);

        mPlanetJList.setCellRenderer(new DefaultListCellRenderer());
        mPlanetJList.addListSelectionListener(e -> {
            int index = mPlanetJList.getSelectedIndex();
            if (index != -1) {
                System.out.println("Selecting item at " + index);
                setEditingPlanet(mPlanetJList.getSelectedValue());
            } else {
                mSelectedPlanet = null;
            }
        });
        mPlanetJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        mAddButton.addActionListener(e -> addPlanet(new Planet()));
        mRemoveButton.addActionListener(e -> {
            int index = mPlanetJList.getSelectedIndex();
            if (index != -1) {
                mPlanetDefaultListModel.remove(index);
                mEditSimulatorComponent.removePlanet(index);
                mSimulation.removePlanet(index);
                mSelectedPlanet = null;

                mEdited = true;
            }
        });

        mPlanetJList.setModel(mPlanetDefaultListModel);

        mUpdateButton.addActionListener(e -> commitPlanet());
        mRevertButton.addActionListener(e -> {
            if (mSelectedPlanet != null) {
                setEditingPlanet(mSelectedPlanet);
            }
        });

        mSimulationPanel.setLayout(new CardLayout(5, 5));
        mSimulationPanel.add(mEditSimulatorComponent);

        mEditSimulatorComponent.setOnPlanetPressedListener((planet, x, y) -> {
            mPlanetTranslateReferenceX = (float) (planet.getX() - x);
            mPlanetTranslateReferenceY = (float) (planet.getY() - y);

            int index = mPlanetDefaultListModel.indexOf(planet);

            mEditSimulatorComponent.setOnCaptureClickListener(new EditSimulatorComponent.OnCaptureClickListener() {
                @Override
                public void onDrag(EditSimulatorComponent view, float x, float y) {
                    planet.setLocation(x + mPlanetTranslateReferenceX, y + mPlanetTranslateReferenceY);
                    mEditSimulatorComponent.invalidatePlanet(index);

                    repaint();
                }

                @Override
                public boolean onRelease(EditSimulatorComponent view, float x, float y) {
                    onDrag(view, x, y);

                    mEdited = true;

                    return true;
                }
            });
        });
        mSimulateButton.addActionListener(e -> {
            Simulator simulator = mSimulation.createSimulator();

            SimulationDialog dialog = new SimulationDialog(simulator, this);

            dialog.setVisible(true);
        });
        mEditSimulatorComponent.setOnPlanetClickedListener(planet -> {
            mPlanetJList.setSelectedValue(planet, true);

            System.out.println("Clicked at planet " + planet);
        });

        initMenuBar();

        if (mSimulation instanceof LinearElasticSimulation) {
            // ((LinearElasticSimulation) mSimulation).setRestingRadius(0.5 * step);
//            ((LinearElasticSimulation) mSimulation).setFriction(0.0);
//            ((LinearElasticSimulation) mSimulation).setElasticConstant(100);
            mSimulation.setTimeInterval(0.1);
        }

        pack();
    }

    public void addPlanet(Planet planet) {
        mPlanetDefaultListModel.addElement(planet);
        mEditSimulatorComponent.addPlanet(planet);

        mSimulation.addPlanet(planet);

        mEdited = true;

        System.out.println("Add planet " + planet);
    }

    private void initMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenu constants = new JMenu("Simulation");

        constants.add(new AbstractAction("Edit constants") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimulationConstantsEditorDialog dialog = new SimulationConstantsEditorDialog(mSimulation);

                dialog.setVisible(true);

                mEdited = true;
            }
        });

        file.add(new AbstractAction("New") {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (showWantSavePane()) {
                    case JOptionPane.YES_OPTION:
                        if (!save()) break;
                    case JOptionPane.NO_OPTION:
                        reset();
                    default:
                        break;
                }

                reset();
            }
        });
        file.add(new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent e) {
                s:
                switch (showWantSavePane()) {
                    case JOptionPane.YES_OPTION:
                        if (!save()) break;
                    case JOptionPane.NO_OPTION:
                        int result = mFileChooser.showOpenDialog(MainWindow.this);
                        switch (result) {
                            case JFileChooser.APPROVE_OPTION:
                                try {
                                    File file = mFileChooser.getSelectedFile();
                                    loadFile(file);

                                    mOriginalFile = file;
                                } catch (IOException | ClassNotFoundException e1) {
                                    showErrorMessage(e1);

                                    break s;
                                }
                                break;
                            default:
                                break s;
                        }
                    default: // CANCEL
                        break;
                }
            }
        });
        file.add(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        file.add(new AbstractAction("Save as...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        });
        file.add(new AbstractAction("Merge with") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = mFileChooser.showOpenDialog(MainWindow.this);

                if (result == JFileChooser.APPROVE_OPTION) {
                    try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(mFileChooser.getSelectedFile()))) {
                        Simulation simulation = (Simulation) input.readObject();

                        for (Planet planet : simulation.getPlanets())
                            addPlanet(planet);

                        mOriginalFile = null;
                        mEdited = true;
                    } catch (IOException | ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        bar.add(file);
        bar.add(constants);

        setJMenuBar(bar);
    }

    private int showWantSavePane() {
        return mEdited ? JOptionPane.showConfirmDialog(this, "Want to save the simulation?",
                "Save", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) : JOptionPane.NO_OPTION;
    }

    private boolean save() {
        if (mOriginalFile == null) {
            return saveAs();
        } else {
            try {
                writeFile(mOriginalFile);

                return true;
            } catch (IOException e1) {
                showErrorMessage(e1);

                return false;
            }
        }
    }

    private boolean saveAs() {
        int result = mFileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = mFileChooser.getSelectedFile();

            try {
                writeFile(file);

                mOriginalFile = file;

                return true;
            } catch (IOException e) {
                showErrorMessage(e);

                return false;
            }
        } else {
            return false;
        }
    }

    private void showErrorMessage(String msg) {
        System.err.println("ERROR: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showErrorMessage(Throwable ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void writeFile(File file) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(mSimulation);

            mEdited = false;
        }
    }

    private void loadFile(File file) throws IOException, ClassNotFoundException {
        System.out.println("Loading from file " + file);

        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            mSimulation = (Simulation) input.readObject();

            mPlanetDefaultListModel.clear();
            mEditSimulatorComponent.clearPlanets();

            mSelectedPlanet = null;

            Planet[] planets = mSimulation.getPlanets();

            mPlanetDefaultListModel.ensureCapacity(planets.length);

            for (Planet planet : planets) {
                mPlanetDefaultListModel.addElement(planet);
            }

            mEditSimulatorComponent.setPlanets(Arrays.asList(planets));

            mEdited = false;
        }
    }

    private void reset() {
        mSimulation = new GravitySimulation();

        mPlanetDefaultListModel.clear();
        mEditSimulatorComponent.clearPlanets();

        mSelectedPlanet = null;

        mEdited = false;
    }

    private void setEditingPlanet(Planet planet) {
        mMassField.setValue(planet.getMass());
        mRadiusField.setValue(planet.getRadius());
        mChargeField.setValue(planet.getCharge());

        mVXField.setValue(planet.getVx());
        mVYField.setValue(planet.getVy());

        mXField.setValue(planet.getX());
        mYField.setValue(planet.getY());

        mSelectedPlanet = planet;
    }

    private void commitPlanet() {
        if (mSelectedPlanet != null) {
            mSelectedPlanet.setMass(getDoubleFrom(mMassField));
            mSelectedPlanet.setCharge(getDoubleFrom(mChargeField));
            mSelectedPlanet.setRadius((float) getDoubleFrom(mRadiusField));

            mSelectedPlanet.setLocation(getDoubleFrom(mXField), getDoubleFrom(mYField));
            mSelectedPlanet.setVelocity(getDoubleFrom(mVXField), getDoubleFrom(mVYField));

            mEdited = true;
        } else {
            System.err.println("No planet to commit data");
        }
    }

    private static double getDoubleFrom(JFormattedTextField field) {
        Object value = field.getValue();
        if (value instanceof Number) return ((Number) value).doubleValue();

        return 0.0d;
    }
}
