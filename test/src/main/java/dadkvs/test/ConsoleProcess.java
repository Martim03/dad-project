package dadkvs.test;

public class ConsoleProcess extends Process {
    public ConsoleProcess() { // TODO needs to receive basePort , or SHOULD at least
        super("mvn exec:java");
    }
}
