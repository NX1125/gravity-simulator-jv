package nx1125.simulator.windows;

import nx1125.simulator.FrameRateThread;
import nx1125.simulator.components.SimulatorComponent;
import nx1125.simulator.elastic.ElasticSimulator;
import nx1125.simulator.elastic.LinearElasticSimulator;
import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;
import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.Simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SimulationDialog extends JFrame {

    private final SlidersDialog dialog;

    private JPanel contentPane;

    private JButton buttonCancel;

    private JPanel mSimulationPane;

    private JButton mPlayButton;

    private SimulatorComponent mSimulatorComponent;

    private Simulator mSimulator;

    private int mFramePerSecond;

    private FrameRateThread mFrameRateThread;

    private JFileChooser mFileChooser = new JFileChooser();

    private MainWindow mMainWindow;

    public SimulationDialog(Simulator simulator, MainWindow mainWindow) {
        mSimulator = simulator;
        mFramePerSecond = simulator.getSimulation().getFrameRate();
        mMainWindow = mainWindow;

        if (mSimulator instanceof LinearElasticSimulator) {
            dialog = new SlidersDialog(this);

            dialog.setVisible(true);
        } else {
            dialog = null;
        }

        initMenuBar();

        setContentPane(contentPane);
        // setModal(true);

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

        System.out.println("Creating simulator component");
        mSimulatorComponent = new SimulatorComponent(simulator);

        System.out.println("Registering planets into component");
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

        System.out.println("Creating frame rate thread");
        mFrameRateThread = new FrameRateThread(simulator,
                new FrameRateThread.OnFrameUpdateListener() {

                    @Override
                    public void onPlay() {
                    }

                    @Override
                    public void onPause() {
                    }

                    @Override
                    public void onFrameChanged(FrameRateThread thread, int frameIndex, PlanetState[] states) {
                        mSimulatorComponent.setPlanetStateArray(states);

                        onInvalidate(thread);
                    }

                    @Override
                    public void onInvalidate(FrameRateThread thread) {
                        // System.out.println("onInvalidate in simulation dialog");

                        mSimulatorComponent.repaint();
                    }
                }, this);

        if (mSimulator instanceof ElasticSimulator) {
            System.out.println("Elastic simulation detected. Adding support for locked planets click listener");

            mSimulatorComponent.setOnPlanetClickedListener((index, planet) -> {
                System.out.println("Toggling planet " + planet + " lock state");
                ElasticSimulator s = (ElasticSimulator) mSimulator;

                if (!s.addLockedPlanet(index)) {
                    s.removeLockedPlanet(index);
                }
            });
            mSimulatorComponent.setOnPlanetMouseMotionListener(new SimulatorComponent.OnPlanetMouseMotionListener() {

                private boolean mWasLocked;

                private float mReferenceX;
                private float mReferenceY;

                @Override
                public void onMousePressed(float x, float y, int index, PlanetState planet) {
                    mReferenceX = (float) (x - planet.x);
                    mReferenceY = (float) (y - planet.y);

                    mWasLocked = !((ElasticSimulator) mSimulator).addLockedPlanet(index);
                }

                @Override
                public void onPlanetDragged(float x, float y, int index, PlanetState planet) {
                    while (mFrameRateThread.isProcessingSimulation()) {
                    }

                    planet.x = x - mReferenceX;
                    planet.y = y - mReferenceY;
                }

                @Override
                public void onPlanetReleased(float x, float y, int index, PlanetState planet) {
                    if (!mWasLocked) {
                        ((ElasticSimulator) mSimulator).removeLockedPlanet(index);
                    }

                    onPlanetDragged(x, y, index, planet);
                }
            });

            ElasticSimulator elasticSimulator = (ElasticSimulator) simulator;

            elasticSimulator.addLockedPlanet(0);
            elasticSimulator.addLockedPlanet(elasticSimulator.getPlanetCount() - 1);
        }

        System.out.println("Starting frame rate thread");
        mFrameRateThread.start();

        pack();
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
        dialog.setVisible(false);
    }

    public Simulator getSimulator() {
        return mSimulator;
    }
}
