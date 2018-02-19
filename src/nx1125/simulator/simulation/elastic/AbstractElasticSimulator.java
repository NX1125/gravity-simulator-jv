package nx1125.simulator.simulation.elastic;

import nx1125.simulator.simulation.Simulator;

public interface AbstractElasticSimulator extends Simulator {

    double getRestingDistance();

    void setRestingDistance(double radius);

    double getElasticConstant();

    void setElasticConstant(double constant);

    double getFrictionByVelocity();

    void setFrictionByVelocity(double friction);

    boolean addLockedPlanet(int index);

    void removeLockedPlanet(int index);

    int getPlanetCount();

    void setPlanetLocation(int index, double x, double y);
}
