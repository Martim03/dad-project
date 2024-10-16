package dadkvs.server;

public class MainLoop implements Runnable {
    //TODO ASK PROFESSOR WHY?

    DadkvsServerState server_state;
    DebugModes debug;
    private boolean has_work;

    public MainLoop(DadkvsServerState state, DebugModes debug) {
        this.server_state = state;
        this.has_work = false;
        this.debug = debug;
    }

    @Override
    public void run() {
        while (true) {
            this.doWork();
        }
    }

    synchronized public void doWork() {
        System.out.println("Main loop do work start");
        this.has_work = false;
        while (this.has_work == false) {
            System.out.println("Main loop do work: waiting");
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        if (server_state.getDebugMode() == DebugModeCodes.CRASH.getCode()) {
            // Terminate everything with shutdownHook
            System.exit(0);
        }
        else if (server_state.getDebugMode() == DebugModeCodes.UNFREEZE.getCode()){
            debug.unfreeze();
        }
        System.out.println("Main loop do work finish");
    }

    synchronized public void wakeup() {
        this.has_work = true;
        notify();
    }
}
