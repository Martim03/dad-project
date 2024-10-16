package dadkvs.server;

import java.util.Random;

public class DebugModes {
    private static final int MIN_SLEEP_MS = 100;
    private static final int MAX_SLEEP_MS = 1000;

    public DebugModes() {
        
    }

    /**
     * Applies the debug modes to the received requests, e.g. slowmode(random sleep)
     * and freeze(ignore request).
     * 
     * 
     * @return false if the server is unfreeze and should process the request, or
     *         true if the server is freezed and should ignore the request
     */
    public synchronized void applyDebugMode(int debugMode, boolean isClientRequest) {
        if (isClientRequest && debugMode == DebugModeCodes.FREEZE.getCode()) {
            System.out.println("FREEZE: Blocking request unitl UNFREEZE");
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("UNFREEZE: Unblocking requests");            
        } else if (debugMode == DebugModeCodes.SLOW_MODE_ON.getCode()) {
            System.out.println("SLOW_MODE: Slowing down request");
            randomSleep();
        }
    }

    public synchronized void unfreeze() {
        // will wake up all the threads that were block by the wait() of the FREEZE
        notifyAll();
    }

    private static void randomSleep() {
        Random random = new Random();

        // Generate a random sleep duration within the defined range
        int sleepTime = random.nextInt(MAX_SLEEP_MS - MIN_SLEEP_MS + 1) + MIN_SLEEP_MS;

        try {
            Thread.sleep(sleepTime); // Sleep for the random duration
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }
    }
}
