package dadkvs.test;

public class ServerProcess extends Process {

    private final static String MVN_EXEC = "mvn exec:java -f ../server/pom.xml";
    
    public ServerProcess(int id, int basePort) {
        super(MVN_EXEC + " -Dexec.args=\"" + basePort + " " + id + "\"");
    }
}
