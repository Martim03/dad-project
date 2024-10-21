package dadkvs.test;

import java.util.ArrayList;
import java.util.List;

import dadkvs.test.tests.PaxosBeforeRequestTest;

public class TestExecutor {

    private int testsPassed = 0;
    private final List<Test> tests = new ArrayList<>();

    private void runTests() throws InterruptedException {
        // Create and add tests to the list
        tests.add(new PaxosBeforeRequestTest(8080));
        tests.add(new PaxosBeforeRequestTest(8089));

        for (Test test : tests) {
            boolean passed = test.run();
            if (passed){
                synchronized (this) {
                    testsPassed++;
                }
            }
        }

        // Create a CountDownLatch to wait for all test threads to finish
        /* TODO
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
        latch.await(); */
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