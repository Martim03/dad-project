package dadkvs.test;

public class Server extends Process {
    public Server(int id, int basePort) {
        super("mvn exec:java -Dexec.args=\"" + basePort + " " + id + "\"");
    }
}
