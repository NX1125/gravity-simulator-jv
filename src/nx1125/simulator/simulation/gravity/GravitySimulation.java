package nx1125.simulator.simulation.gravity;

import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.Simulator;

public class GravitySimulation extends Simulation {

    private double mGravityConstant = 1;
    private double mPermittivityConstant = 1;
    private double mPermeabilityConstant = 1;

    private boolean mCollisionEnabled = false;

    /**
     * Return the constant that affects the force in gravity field.
     */
    public double getGravityConstant() {
        return mGravityConstant;
    }

    public void setGravityConstant(double gravityConstant) {
        mGravityConstant = gravityConstant;
    }

    /**
     * The permittivity is a physics constant that is the vacuum permittivity. It is used to compute
     * the force due to the electric field between two planets.
     */
    public double getPermittivityConstant() {
        return mPermittivityConstant;
    }

    public void setPermittivityConstant(double permittivityConstant) {
        mPermittivityConstant = permittivityConstant;
    }

    /**
     * The permeability is a physics constant that is the vacuum permeability. It is used to compute
     * the force due to the magnetic field between two planets.
     */
    public double getPermeabilityConstant() {
        return mPermeabilityConstant;
    }

    public void setPermeabilityConstant(double permeabilityConstant) {
        mPermeabilityConstant = permeabilityConstant;
    }

    /**
     * Return {@code true} if two planets collide in the planet, then there is a elastic collision
     * between them and the simulator must give both new velocities based on the collision
     * properties.
     */
    public boolean isCollisionEnabled() {
        return mCollisionEnabled;
    }

    public void setCollisionEnabled(boolean collisionEnabled) {
        mCollisionEnabled = collisionEnabled;
    }

    @Override
    public Simulator createSimulator() {
        return new GravitySimulator(this);
    }
}
