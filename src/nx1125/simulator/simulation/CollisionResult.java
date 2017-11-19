package nx1125.simulator.simulation;

/**
 * Created by guilh on 01/10/2017.
 */
public class CollisionResult {

    int mIndex0;
    int mIndex1;
    private Planet mPlanet0;
    private Planet mPlanet1;
    private PlanetState mPlanetState0;
    private PlanetState mPlanetState1;
    private double mCollisionTime;

    public CollisionResult() {
    }

    public CollisionResult(PlanetState state0, PlanetState state1, double collisionTime) {
        mPlanetState0 = state0;
        mPlanetState1 = state1;
        mCollisionTime = collisionTime;
    }

    public void setIndexes(int i0, int i1) {
        mIndex0 = i0;
        mIndex1 = i1;
    }

    public void setCollision(Planet p0, PlanetState s0, Planet p1, PlanetState s1, double collisionTime) {
        mPlanet0 = p0;
        mPlanet1 = p1;

        mPlanetState0 = s0;
        mPlanetState1 = s1;

        mCollisionTime = collisionTime;
    }

    public void clear() {
        mCollisionTime = Double.MAX_VALUE;

        mPlanetState0 = null;
        mPlanetState1 = null;
    }

    public Planet getPlanet0() {
        return mPlanet0;
    }

    public Planet getPlanet1() {
        return mPlanet1;
    }

    public PlanetState getPlanetState0() {
        return mPlanetState0;
    }

    public PlanetState getPlanetState1() {
        return mPlanetState1;
    }

    public double getCollisionTime() {
        return mCollisionTime;
    }

    public int getIndex0() {
        return mIndex0;
    }

    public int getIndex1() {
        return mIndex1;
    }
}
