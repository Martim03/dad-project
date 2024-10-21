package dadkvs.test;

public class ServerProcess extends Process {

    private final static String MVN_EXEC = "mvn exec:java -f ../server/pom.xml";
    
    public ServerProcess(int id, int basePort) {
        super(addElement(MVN_EXEC.split(" "),"-Dexec.args=\"" + basePort + " " + id + "\""));
    }

    // TODO refactor
    private static String[] addElement(String[] original, String element) {
        String[] newArray = new String[original.length + 1];
        System.arraycopy(original, 0, newArray, 0, original.length);
        newArray[original.length] = element;
        return newArray;
    }
}
