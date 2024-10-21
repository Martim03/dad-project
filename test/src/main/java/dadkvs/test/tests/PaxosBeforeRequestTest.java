package dadkvs.test.tests;

import java.io.IOException;

import dadkvs.test.Test;

public class PaxosBeforeRequestTest extends Test {

    private static final int SERVERS_NUM = 5;
    private static final int CLIENTS_NUM = 1;
    private static final String TEST_NAME = "paxos before client request";
    

    public PaxosBeforeRequestTest(int basePort) {
        super(TEST_NAME, basePort, SERVERS_NUM, CLIENTS_NUM);  
    }

    @Override
    protected void execute() {
        // TODO
        System.out.println("Executing test: " + TEST_NAME);
        try {
            this.getServer(0).write("ola");
            this.getClient(0).write("ola");
        } catch (IOException ex) {
        }
    }

    @Override
    protected boolean checkSuccess() {
        // TODO
        System.out.println("Checking test success: " + TEST_NAME);  
        try {  
            System.out.println("RESULT : " + this.getServer(0).readStdOutLine());
            System.out.println("RESULTERR : " + this.getServer(0).readStdErrLine());

            System.out.println("ABA : " + this.getClient(0).readStdOutAll());
            System.out.println("ABAERR : " + this.getClient(0).readStdErrAll());

        } catch (IOException ex) {
            return false;
        }
        return true;
    }

}
