package dadkvs.test;

public class ConsoleProcess extends Process {

    private final static String MVN_EXEC = "mvn exec:java -f ../consoleclient/pom.xml";
    private static final String SERVER_HOST = "localhost"; 

    public ConsoleProcess(int basePort) {
        super(addElement(MVN_EXEC.split(" "), " -Dexec.args=\"" + SERVER_HOST + " " + basePort + "\""));
    }

    // TODO refactor
    private static String[] addElement(String[] original, String element) {
        String[] newArray = new String[original.length + 1];
        System.arraycopy(original, 0, newArray, 0, original.length);
        newArray[original.length] = element;
        return newArray;
    }
}
