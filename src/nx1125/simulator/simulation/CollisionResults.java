package nx1125.simulator.simulation;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by guilh on 01/10/2017.
 */
public class CollisionResults {

    private List<CollisionResult> mCollisionResultList = new LinkedList<>();

    private double mCollisionTime;

    public CollisionResults(double collisionTime) {
        mCollisionTime = collisionTime;
    }

    public double getCollisionTime() {
        return mCollisionTime;
    }

    public void addCollision(CollisionResult result) {
        mCollisionResultList.add(result);
    }

    public CollisionResult[] getCollisionResults() {
        return mCollisionResultList.toArray(new CollisionResult[mCollisionResultList.size()]);
    }
}
