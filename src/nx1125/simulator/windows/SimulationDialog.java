package nx1125.simulator.windows;

import nx1125.simulator.FrameRateThread;
import nx1125.simulator.components.SimulatorComponent;
import nx1125.simulator.elastic.ElasticSimulator;
import nx1125.simulator.simulation.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SimulationDialog extends JDialog {

    private JPanel contentPane;

    private JButton buttonCancel;

    private JPanel mSimulationPane;

    private JButton mPlayButton;

    private JSlider mTimeline;

    private SimulatorComponent mSimulatorComponent;

    private Simulator mSimulator;

    private int mFramePerSecond;

    private FrameRateThread mFrameRateThread;

    private JFileChooser mFileChooser = new JFileChooser();

    public SimulationDialog(Simulator simulator) {
        mSimulator = simulator;
        mFramePerSecond = simulator.getSimulation().getFrameRate();

        initMenuBar();

        setContentPane(contentPane);
        setModal(true);

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

        mSimulatorComponent = new SimulatorComponent();

        mSimulatorComponent.setPlanetArray(simulator.getPlanets());

        mSimulationPane.setLayout(new CardLayout(5, 5));
        mSimulationPane.add(mSimulatorComponent);

        mPlayButton.addActionListener(e -> {
            if (mFrameRateThread.isPlaying()) {
                mFrameRateThread.pause();
                mPlayButton.setText("Play");
            } else {
                mFrameRateThread.play();
                mPlayButton.setText("Pause");
            }
        });

        mTimeline.setMaximum(mSimulator.getSimulation().getStatesCount() / mSimulator.getStatesPerCycle() + 1);

        mFrameRateThread = new FrameRateThread(mFramePerSecond, simulator.getStatesPerCycle(), simulator.getSimulation().getStatesCount(),
                new FrameRateThread.OnFrameUpdateListener() {

                    @Override
                    public void onPlay() {
                    }

                    @Override
                    public void onPause() {
                    }

                    @Override
                    public void onFrameChanged(FrameRateThread thread, int frameIndex) {
                        int state = mFrameRateThread.getSimulationState();

                        if (mSimulator.isStateDone(state)) {
                            mSimulatorComponent.setPlanetStateArray(mSimulator.getPlanetStates(state));

                            mTimeline.setValue(state / mSimulator.getStatesPerCycle());

                            onInvalidate(thread);
                        } else {
                            mFrameRateThread.pause();
                        }
                    }

                    @Override
                    public void onInvalidate(FrameRateThread thread) {
                        // System.out.println("onInvalidate in simulation dialog");

                        mSimulatorComponent.repaint();
                    }
                });
        mFrameRateThread.start();

        mSimulator.setOnSimulationEventListener(new Simulator.OnSimulatorEventListener() {
            @Override
            public void onSimulationStarted(Simulator simulator) {
                mSimulatorComponent.setPlanetStateArray(simulator.getPlanetStates(0));
                mSimulatorComponent.repaint();
            }

            @Override
            public void onSimulationFinish(Simulator simulator) {
                mSimulator = new SimulationResults(simulator.getSimulation(),
                        simulator.getPlanetStates(), null, simulator.getPlanets());
            }

            @Override
            public void onSimulationProgressUpdated(Simulator simulator, int state) {
                mTimeline.setExtent((mTimeline.getMaximum() - state) / mSimulator.getStatesPerCycle());

                // System.out.println("Simulation progress: state = " + state);
            }
        });

        mTimeline.addChangeListener(new ChangeListener() {
            private boolean mWasPlaying = false;
            private boolean mComesFromUser = false;

            @Override
            public void stateChanged(ChangeEvent e) {
                if (mTimeline.getValueIsAdjusting()) {
                    mWasPlaying = mFrameRateThread.isPlaying() || mComesFromUser && mWasPlaying;
                    if (mWasPlaying) mFrameRateThread.pause();

                    mFrameRateThread.setCycle(mTimeline.getValue());
                    mSimulatorComponent.setPlanetStateArray(mSimulator.getPlanetStates(mFrameRateThread.getSimulationState()));
                    mComesFromUser = true;
                    System.out.println("Timeline changed to " + mTimeline.getValue());

                    mSimulatorComponent.repaint();
                } else {
                    if (mWasPlaying) {
                        mFrameRateThread.play();
                        mWasPlaying = false;
                    }

                    if (mComesFromUser) {
                        mFrameRateThread.setCycle(mTimeline.getValue());
                        mComesFromUser = false;
                        System.out.println("Timeline changed to " + mTimeline.getValue());
                    }
                }
            }
        });

        if (mSimulator instanceof ElasticSimulator) {
            mSimulatorComponent.setOnPlanetClickedListener((index, planet) -> {
                System.out.println("Toggling planet " + planet + " lock state");
                ElasticSimulator s = (ElasticSimulator) mSimulator;

                if (!s.addLockedPlanet(index)) {
                    s.removeLockedPlanet(index);
                }
            });
        }

        pack();

        mSimulator.start();
    }

    private void initMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");

        JMenuItem saveAsInitialState = new JMenuItem("Save as...");

        saveAsInitialState.addActionListener(e -> {
            int result = mFileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                PlanetState[] states = mSimulatorComponent.getPlanetStates();
                Simulation simulation = (Simulation) mSimulator.getSimulation().clone();

                for (PlanetState state : states) {
                    Planet p = new Planet();

                    p.setLocation(state.x, state.y);
                    p.setVelocity(state.vx, state.vy);
                    p.setCharge(p.getCharge());
                    p.setMass(p.getMass());
                    p.setRadius(p.getRadius());

                    simulation.addPlanet(p);
                }

                try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(mFileChooser.getSelectedFile()))) {
                    output.writeObject(simulation);

                    output.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        bar.add(file);

        file.add(saveAsInitialState);

        setJMenuBar(bar);
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();

        mFrameRateThread.interrupt();
        mSimulator.interrupt();
    }
}
