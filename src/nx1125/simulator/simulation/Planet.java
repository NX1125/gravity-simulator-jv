package nx1125.simulator.simulation;

import java.io.Serializable;

/**
 * Created by guilh on 22/09/2017.
 */

public class Planet implements Serializable {

    private double mX;
    private double mY;

    private double mVx;
    private double mVy;

    private double mMass = 1;
    private double mCharge = 0;

    private float mRadius = 1;
    private float mRadiusSqr;

    public Planet() {
    }

    public double getX() {
        return mX;
    }

    public double getY() {
        return mY;
    }

    public double getVx() {
        return mVx;
    }

    public double getVy() {
        return mVy;
    }

    public void setLocation(double x, double y) {
        mX = x;
        mY = y;
    }

    public void setVelocity(double x, double y) {
        mVx = x;
        mVy = y;
    }

    public double getMass() {
        return mMass;
    }

    public void setMass(double mass) {
        mMass = mass;
    }

    public double getCharge() {
        return mCharge;
    }

    public void setCharge(double charge) {
        mCharge = charge;
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float radius) {
        mRadius = radius;
        mRadiusSqr = radius * radius;
    }

    @Override
    public String toString() {
        return "Planet{" +
                "x=" + mX +
                ", y=" + mY +
                ", vx=" + mVx +
                ", vy=" + mVy +
                ", mass=" + mMass +
                ", charge=" + mCharge +
                ", radius=" + mRadius +
                '}';
    }

    public float getRadiusSqr() {
        return mRadiusSqr;
    }
}
