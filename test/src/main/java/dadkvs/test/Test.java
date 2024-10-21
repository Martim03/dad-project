package dadkvs.test;

import java.io.IOException;

public abstract class Test {
    private static final int INIT_WAIT_TIME_SECONDS = 5;

    private final ServerProcess[] servers;
    private final ClientProcess[] clients;
    private ConsoleProcess console;
    private final int basePort;
    private final String name;

    public Test(String name, int basePort, int numServers, int numClients) {
        this.name = name;
        servers = new ServerProcess[numServers];
        clients = new ClientProcess[numClients];
        this.basePort = basePort;
    }

    /** Function that will run the test.
     *  @return true if the test PASSED, false if it FAILED.
     */
    public boolean run() {
        init();

        execute();
 
        boolean success = checkSuccess();
        String testStatus = success ? "SUCCESS" : "FAILED";
        System.out.println(this.name + ": " +  testStatus);

        cleanup();

        return success;
    } 

    // Method to initialize the servers, clients, and console
    private void init() {
        try {
            // Initialize servers with different IDs
            for (int i = 0; i < servers.length; i++) {
                servers[i] = new ServerProcess(i, basePort);
                servers[i].start();
            }

            // Initialize clients with different configurations
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new ClientProcess(i + 1, basePort);
                clients[i].start();
            }

            // Initialize and start the admin console
            console = new ConsoleProcess(basePort);
            console.start();

            // Sleep for a few seconds to allow the servers to start
            sleepNSeconds(INIT_WAIT_TIME_SECONDS);

        } catch (IOException e) {
            System.err.println("Error initializing test: " + e.getMessage());
            cleanup();
            System.exit(-1);
        }
    }

    // Method to clean up by killing all servers, clients, and console
    private void cleanup() {
        // Kill all servers
        for (ServerProcess server : servers) {
            if (server != null) {
                server.kill();
            }
        }

        // Kill all clients
        for (ClientProcess client : clients) {
            if (client != null) {
                client.kill();
            }
        }

        // Kill the console
        if (console != null) {
            console.kill();
        }
    }

    // Abstract method to execute the test - to be implemented by subclasses
    protected abstract void execute();

    // Abstract method to check the result of the test - to be implemented by subclasses
    protected abstract boolean checkSuccess();

    // Accessor to retrieve a server by its ID
    protected ServerProcess getServer(int id) {
        if (id >= 0 && id < servers.length) {
            return servers[id];
        }
        throw new IllegalArgumentException("Invalid server ID");
    }

    // Accessor to retrieve a client by its ID
    protected ClientProcess getClient(int id) {
        if (id >= 0 && id < clients.length) {
            return clients[id];
        }
        throw new IllegalArgumentException("Invalid client ID");
    }

    // Accessor to retrieve the console
    protected ConsoleProcess getConsole() {
        return console;
    }

    protected void sleepNSeconds(int n) {
        try {
            Thread.sleep(n * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
