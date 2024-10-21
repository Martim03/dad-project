package dadkvs.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import dadkvs.test.tests.PaxosBeforeRequestTest;

public class TestExecutor {
    private final static int MAX_SERVERS_NUM = 5;
    private final static int INITIAL_BASE_PORT = 8080;

    private int newBasePort = INITIAL_BASE_PORT;
    private int testsPassed = 0;
    private final List<Test> tests = new ArrayList<>();

    private int getNewBasePort() {
        int basePort = newBasePort;
        newBasePort += MAX_SERVERS_NUM;
        return basePort;
    }

    private void runTests() throws InterruptedException {
        // Create and add tests to the list
        tests.add(new PaxosBeforeRequestTest(getNewBasePort()));
        tests.add(new PaxosBeforeRequestTest(getNewBasePort()));

        /*  TODO  remove this old code
         for (Test test : tests) {
            boolean passed = test.run();
            if (passed){
                synchronized (this) {
                    testsPassed++;
                }
            }
        } */

        // Create a CountDownLatch to wait for all test threads to finish
        CountDownLatch latch = new CountDownLatch(tests.size());

        for (Test test : tests) {
            new Thread(() -> {
                try {
                    boolean passed = test.run();
                    if (passed){
                        synchronized (this) {
                            testsPassed++;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all test threads to finish
        latch.await();
    }

    public static void main(String[] args) {
        TestExecutor executor = new TestExecutor();
        try {
            executor.runTests();
            System.out.println("Tests Passed: " + executor.testsPassed + "/" + executor.tests.size());
        } catch (InterruptedException e) {
            System.out.println("Tests aborted");
            System.exit(-1);
        }
    }
}