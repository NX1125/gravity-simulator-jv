package nx1125.simulator.windows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

public class EnergyStatsDialog extends JFrame {

    private JPanel contentPane;

    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel mInnerPanel;

    private EnergyStatsComponent mEnergyStatsComponent;

    private SimulationDialog mWindow;

    public EnergyStatsDialog(int length, SimulationDialog window) {
        mEnergyStatsComponent = new EnergyStatsComponent(length);

        mWindow = window;

        setContentPane(contentPane);
        // setModal(false);
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
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        mInnerPanel.setLayout(new CardLayout(5, 5));
        mInnerPanel.add(mEnergyStatsComponent);

        mEnergyStatsComponent.setPreferredSize(new Dimension(1000, 500));

        pack();
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    public void onCancel() {
        // add your code here if necessary
        if (isVisible()) {
            mWindow.onCancel();

            dispose();
        }
    }

    public void addValue(double kinetic, double potential) {
        mEnergyStatsComponent.addValue(kinetic, potential);
    }

    private static class EnergyStatsComponent extends JComponent {

        private final double[] mTotalEnergyQueue;
        private final double[] mPotentialQueue;
        private final double[] mKineticQueue;
        private final GeneralPath mGeneralPath;
        private int mQueueStart;
        private float mVerticalOffset;
        private float mScale;

        public EnergyStatsComponent(int queueLength) {
            mTotalEnergyQueue = new double[queueLength];
            mPotentialQueue = new double[queueLength];
            mKineticQueue = new double[queueLength];

            mGeneralPath = new GeneralPath(Path2D.WIND_NON_ZERO, queueLength);

            setForeground(Color.black);
            setBackground(Color.white);
        }

        public void addValue(double kinetic, double potential) {
            int index = mQueueStart + 1;
            if (index >= mTotalEnergyQueue.length) index = 0;

            mQueueStart = index;
            mTotalEnergyQueue[index] = kinetic + potential;
            mKineticQueue[index] = kinetic;
            mPotentialQueue[index] = potential;

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setBackground(getBackground());
            g2.clearRect(0, 0, getWidth(), getHeight());

            int x = getWidth() - 1;
            mVerticalOffset = (getHeight() - 1) / 2;

            mScale = 0;

            updateScaleAndOffset(mKineticQueue);
            updateScaleAndOffset(mPotentialQueue);
            updateScaleAndOffset(mTotalEnergyQueue);

            // System.out.println("Highest energy = " + mScale);

            if (mScale == 0) {
                mScale = 1;
            } else {
                mScale = (0.9f * mVerticalOffset) / mScale;
            }

            createPath(mKineticQueue, mGeneralPath, x);

            g2.setColor(Color.green);
            g2.draw(mGeneralPath);

            createPath(mPotentialQueue, mGeneralPath, x);

            g2.setColor(Color.red);
            g2.draw(mGeneralPath);

            createPath(mTotalEnergyQueue, mGeneralPath, x);

            g2.setColor(Color.black);
            g2.draw(mGeneralPath);
        }

        private void createPath(double[] queue, GeneralPath path, int x) {
            mGeneralPath.reset();

            path.moveTo(x--, transform(queue[mQueueStart]));
            x = addRangeToPath(path, queue, 0, mQueueStart - 1, x);
            addRangeToPath(path, queue, mQueueStart + 1, queue.length - 1, x);
        }

        private void updateScaleAndOffset(double[] queue) {
            for (double v : queue) {
                if (mScale < v)
                    mScale = (float) v;
            }
        }

        private int addRangeToPath(GeneralPath path, double[] queue, int lower, int higher, int x) {
            // assert lower <= higher;
            while (higher >= lower) {
                path.lineTo(x--, transform(queue[higher--]));
            }

            return x;
        }

        private double transform(double y) {
            return mVerticalOffset - y * mScale;
        }
    }
}
