package dadkvs.server;

import java.util.Random;

public class DebugModes {
    private final DadkvsServerState SERVER_STATE;

    private final DebugMode CRASH_DEBUGGER = new CrashDebugMode();
    private final DebugMode FREEZE_DEBUGGER = new FreezeDebugMode();
    private final DebugMode UNFREEZE_DEBUGGER = new UnfreezeDebugMode();
    private final DebugMode SLOW_MODE_ON_DEBUGGER = new SlowModeOnDebugMode();
    private final DebugMode SLOW_MODE_OFF_DEBUGGER = new SlowModeOffDebugMode();
    private final DebugMode BLOCK_PHASE_ONE_DEBUGGER = new BlockPhaseOneDebugMode();
    private final DebugMode UNBLOCK_PHASE_ONE_DEBUGGER = new UnblockPhaseOneDebugMode();
    private final DebugMode BLOCK_PHASE_ONE_AND_TWO_DEBUGGER = new BlockPhaseOneAndTwoDebugMode();
    private final DebugMode UNBLOCK_PHASE_ONE_AND_TWO_DEBUGGER = new UnblockPhaseOneAndTwoDebugMode();

    public DebugModes(DadkvsServerState serverState) {
        this.SERVER_STATE = serverState;
    }

    private DebugModeCodes getDebugMode() {
        return DebugModeCodes.values()[SERVER_STATE.getDebugMode() - 1]; // -1 -> Convert to 0-based index
    }

    private DebugMode getDebugger() {
        return switch (getDebugMode()) {
            case CRASH -> CRASH_DEBUGGER;
            case FREEZE -> FREEZE_DEBUGGER;
            case UNFREEZE -> UNFREEZE_DEBUGGER;
            case SLOW_MODE_ON -> SLOW_MODE_ON_DEBUGGER;
            case SLOW_MODE_OFF -> SLOW_MODE_OFF_DEBUGGER;
            case BLOCK_PHASE_ONE -> BLOCK_PHASE_ONE_DEBUGGER;
            case UNBLOCK_PHASE_ONE -> UNBLOCK_PHASE_ONE_DEBUGGER;
            case BLOCK_PHASE_ONE_AND_TWO -> BLOCK_PHASE_ONE_AND_TWO_DEBUGGER;
            case UNBLOCK_PHASE_ONE_AND_TWO -> UNBLOCK_PHASE_ONE_AND_TWO_DEBUGGER;

            default -> throw new AssertionError();
        };
    }

    /**
     * Executes the debug changes that are propagated when the debug mode changes
     * , e.g. crash the server, unfreeze the server, etc.
     */
    public void executeDebugChanges() {
        getDebugger().executeChanges();
    }

    /**
     * Applies the debug modes to the received requests, e.g. slowmode(random sleep)
     * and freeze(ignore request).
     */
    public synchronized void applyDebugMode(RequestTypes requestType) {

        getDebugger().applyRequestEffects(requestType);
    }

    private abstract class DebugMode {
        public abstract void applyRequestEffects(RequestTypes requestType);

        public abstract void executeChanges();
    }

    private class CrashDebugMode extends DebugMode {
        @Override
        public void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: CrashDebugMode");
            // Do nothing
        }

        @Override
        public void executeChanges() {
            // Terminate the server process
            System.out.println("CRASH: Crashing the server");
            System.exit(0);
        }
    }

    private class FreezeDebugMode extends DebugMode {
        @Override
        public synchronized void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: FreezeDebugMode");
            if (requestType != RequestTypes.CLIENT_REQUEST) {
                // if the request is not a client request, dont block it
                return;
            }

            // Block the request
            System.out.println("FREEZE: Blocking request unitl UNFREEZE");
            try {
                this.wait();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("UNFREEZE: Unblocking requests");
        }

        @Override
        public synchronized void executeChanges() {
            // Do nothing
        }

    }

    private class UnfreezeDebugMode extends DebugMode {
        @Override
        public void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: UnfreezeDebugMode");
            // Do nothing
        }

        @Override
        public void executeChanges() {
            // will wake up all the threads that were block by the wait() of the FREEZE
            synchronized (FREEZE_DEBUGGER) {
                FREEZE_DEBUGGER.notifyAll();
            }
        }
    }

    private class SlowModeOnDebugMode extends DebugMode {
        private static final int MIN_SLEEP_MS = 100;
        private static final int MAX_SLEEP_MS = 1000;

        @Override
        public void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: ");
            System.out.println("SLOW_MODE_ON: Slowing down request");
            randomSleep();
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

        @Override
        public void executeChanges() {
            // Do nothing
        }
    }

    private class SlowModeOffDebugMode extends DebugMode {
        @Override
        public void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: SlowModeOffDebugMode");
            // Do nothing
        }

        @Override
        public void executeChanges() {
            // Do nothing
        }
    }

    private class BlockPhaseOneDebugMode extends DebugMode {
        @Override
        public synchronized void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: BlockPhaseOneDebugMode");
            if (requestType != RequestTypes.PHASE_ONE_REQUEST) {
                // if the request is not a phase one request, dont block it
                return;
            }

            System.out.println("BLOCK_PHASE_ONE: Blocking Phase One");
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("UNBLOCK_PHASE_ONE: Unblocking Phase One");
        }

        @Override
        public void executeChanges() {
            // Do nothing
        }
    }

    private class UnblockPhaseOneDebugMode extends DebugMode {
        @Override
        public void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: UnblockPhaseOneDebugMode");
            // Do nothing
        }

        @Override
        public void executeChanges() {
            synchronized (BLOCK_PHASE_ONE_DEBUGGER) {
                BLOCK_PHASE_ONE_DEBUGGER.notifyAll();
            }
        }
    }

    private class BlockPhaseOneAndTwoDebugMode extends DebugMode {
        @Override
        public synchronized void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: BlockPhaseOneAndTwoDebugMode");
            if (requestType != RequestTypes.PHASE_ONE_REQUEST && requestType != RequestTypes.PHASE_TWO_REQUEST) {
                // if the request is not a phase one request nor a phase two request, dont block
                // it
                return;
            }

            System.out.println("BLOCK_PHASE_ONE_AND_TWO: Blocking Phase One And Two");
            try {
                this.wait();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("UNBLOCK_PHASE_ONE_AND_TWO: Unblocking Phase One And Two");
        }

        @Override
        public void executeChanges() {
            // Do nothing
        }
    }

    private class UnblockPhaseOneAndTwoDebugMode extends DebugMode {
        @Override
        public void applyRequestEffects(RequestTypes requestType) {
            System.out.println("DEBUG: UnblockPhaseOneAndTwoDebugMode");
            // Do nothing
        }

        @Override
        public void executeChanges() {
            synchronized (BLOCK_PHASE_ONE_AND_TWO_DEBUGGER) {
                BLOCK_PHASE_ONE_AND_TWO_DEBUGGER.notifyAll();
            }
        }
    }
}
