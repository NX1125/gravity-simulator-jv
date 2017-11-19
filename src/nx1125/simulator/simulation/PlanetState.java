package nx1125.simulator.simulation;

/**
 * Created by guilh on 30/09/2017.
 */

public class PlanetState implements Cloneable {

    public double x;
    public double y;

    public double vx;
    public double vy;

    public double ax;
    public double ay;

    public PlanetState(Planet planet) {
        x = planet.getX();
        y = planet.getY();

        vx = planet.getVx();
        vy = planet.getVy();

        ax = 0;
        ay = 0;
    }

    public PlanetState(PlanetState state) {
        x = state.x;
        y = state.y;

        vx = state.vx;
        vy = state.vy;

        ax = state.ax;
        ay = state.ay;
    }

    public PlanetState(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
    }

    public void setAcceleration(double ax, double ay) {
        this.ax = ax;
        this.ay = ay;
    }

    @Override
    public PlanetState clone() {
        try {
            return (PlanetState) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void clearAcceleration() {
        ax = ay = 0;
    }

    public double distance(PlanetState s1) {
        return Math.hypot(x - s1.x, y - s1.y);
    }

    public float distanceSqr(float x, float y) {
        x -= this.x;
        y -= this.y;
        return x * x + y * y;
    }
}
