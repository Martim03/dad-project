package dadkvs.util;

import java.util.ArrayList;

public class GenericResponseCollector<T> {

    ArrayList<T> collectedResponses;
    int received;
    int pending;
    boolean targetReached;

    public GenericResponseCollector(ArrayList<T> responses, int maxresponses) {
        collectedResponses = responses;
        received = 0;
        pending = maxresponses;
        targetReached = false;
    }

    synchronized public void addResponse(T resp) {
        if (!targetReached) {
            collectedResponses.add(resp);
        }

        received++;
        pending--;
        notifyAll();
    }

    synchronized public void addNoResponse() {
        pending--;
        notifyAll();
    }

    synchronized public void waitForTarget(int target) {
        while ((pending > 0) && (received < target)) {
            try {
                System.out.println("Waiting: pending=" + pending + ", received=" + received + ", target=" + target);
                wait();
            } catch (InterruptedException e) {
            }
        }
        targetReached = true;
        System.out.println("Finished waiting: pending=" + pending + ", received=" + received + ", target=" + target);
    }
}
