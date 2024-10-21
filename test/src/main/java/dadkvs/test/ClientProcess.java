package dadkvs.test;

public class ClientProcess extends Process {
    private final static String MVN_EXEC = "mvn exec:java -f ../client/pom.xml";
    
    private static final String SERVER_HOST = "localhost"; 
    private static final String KEY_RANGE = "10";
    private static final String SLEEP_RANGE = "5";
    private static final String LOOP_SIZE = "30";
    private static final boolean INTERACTIVE_MODE = true;
    private static final String[] DELAY_OPTIONS = {"0 0", "1 0", "2 0", "3 0", "4 0"};

    private static final String ARG_HOST = "--host";
    private static final String ARG_PORT = "--port";
    private static final String ARG_KEY_RANGE = "--range";
    private static final String ARG_SLEEP_RANGE = "--sleep";
    private static final String ARG_LOOP_SIZE = "--length";
    private static final String ARG_INTERACTIVE_MODE = "-i";
    private static final String ARG_DELAY = "--delay";

    public ClientProcess(int id, int basePort) {
        super(addElement(MVN_EXEC.split(" "), buildArgs(id, basePort)));
    }

    private static String buildArgs(int id, int basePort) {
        StringBuilder args = new StringBuilder();
        args.append("-Dexec.args='")
               .append(id)
               .append(" ")
               .append("\"").append(ARG_HOST).append(" ").append(SERVER_HOST).append("\"")
               .append(" ")
               .append("\"").append(ARG_PORT).append(" ").append(basePort).append("\"")
               .append(" ")
               .append("\"").append(ARG_KEY_RANGE).append(" ").append(KEY_RANGE).append("\"")
               .append(" ")
               .append("\"").append(ARG_SLEEP_RANGE).append(" ").append(SLEEP_RANGE).append("\"")
               .append(" ")
               .append("\"").append(ARG_LOOP_SIZE).append(" ").append(LOOP_SIZE).append("\"")
               .append(" ")
               .append("\"").append(INTERACTIVE_MODE ? ARG_INTERACTIVE_MODE : "").append("\"");

        for (String delayOption : DELAY_OPTIONS) {
            args.append(" ")
            .append("\"").append(ARG_DELAY).append(" ").append(delayOption).append("\"");
            
        }

        args.append("'");
        System.out.println("ClientArgs: " + args.toString());
        return args.toString();
    }

    // TODO refactor
    private static String[] addElement(String[] original, String element) {
        String[] newArray = new String[original.length + 1];
        System.arraycopy(original, 0, newArray, 0, original.length);
        newArray[original.length] = element;
        return newArray;
    }
}