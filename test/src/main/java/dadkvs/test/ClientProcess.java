package dadkvs.test;

public class ClientProcess extends Process {
    private final String SERVER_HOST = "localhost"; 

    // TODO change this so it only receives the necessary args!!
    public ClientProcess(int id, String serverPort, String keyRange, String sleepRange, String loopSize, String interactiveMode, String delayOptions) {
        super("mvn exec:java -Dexec.args='" + id + " \"" + SERVER_HOST + "\" \"" + serverPort + "\" \"" + keyRange + "\" \"" + sleepRange + "\" \"" + loopSize + "\" \"" + interactiveMode + "\" " + delayOptions + "'");
    }
}
