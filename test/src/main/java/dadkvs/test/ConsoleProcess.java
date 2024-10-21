package dadkvs.test;

public class ConsoleProcess extends Process {

    private final static String MVN_EXEC = "mvn exec:java -f ../consoleclient/pom.xml";

    public ConsoleProcess() { // TODO needs to receive basePort , or SHOULD at least
        super(MVN_EXEC);
    }
}
