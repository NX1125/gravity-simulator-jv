package nx1125.simulator;

import nx1125.simulator.components.CartesianComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.LinkedList;
import java.util.List;

public class Plotter extends CartesianComponent {

    private final GeneralPath mGeneralPath = new GeneralPath();

    private List<FunctionCache> mFunctionCacheList = new LinkedList<>();

    public Plotter() {
        init();
    }

    private void init() {
        setBackground(Color.white);
        setPreferredSize(new Dimension(500, 500));
    }

    public void addFunction(PlotterFunction function, Color color) {
        mFunctionCacheList.add(new FunctionCache(color, function));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D canvas = (Graphics2D) g;

        canvas.setBackground(getBackground());

        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        super.paintComponent(canvas);

        int width = getWidth();

        double x0 = getLowX();
        double x1 = getHighX();

        double dx = (x1 - x0) / width;

        for (FunctionCache cache : mFunctionCacheList) {
            canvas.setColor(cache.getColor());

            mGeneralPath.moveTo(x0, cache.compute(x0));

            for (int i = 1; i < width; i++) {
                double x = dx * i + x0;
                double y = cache.compute(x);

                mGeneralPath.lineTo(x, -y);
            }

            canvas.draw(mGeneralPath.createTransformedShape(getCartesianMatrix()));

            mGeneralPath.reset();
        }
    }

    public static Plotter createWindow() {
        Plotter plotter = new Plotter();

        JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(plotter);

        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        return plotter;
    }

    private static class FunctionCache {

        private Color mColor;
        private PlotterFunction mFunction;

        public FunctionCache(Color color, PlotterFunction function) {
            mColor = color;
            mFunction = function;
        }

        public Color getColor() {
            return mColor;
        }

        public void setColor(Color color) {
            mColor = color;
        }

        public PlotterFunction getFunction() {
            return mFunction;
        }

        public double compute(double x) {
            return mFunction.compute(x);
        }
    }
}
