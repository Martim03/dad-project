package dadkvs.test;

public class TestExecutor extends Test {
    
    private int testsPassed = 0;
    private List<Test> tests = new List();


    private void runTests() {
        
        // TODO either do inheritance and create a class for each test
        //  or just make it so Test takes as arguments the functions to exeecute and checkResult
        // example : new Test(,,,() => {
        //  client1.read()} ,() => return x)
        new Test("paxos before client request", 8080, 5, 2)

        // TODO run each test on a different thread and then do an await for all tests  threads
        // use synchronized acess for increasing the testsPassed at the end

    }

    public static int main () {
        runTests();
        System.out.println("Tests Passed: " + testsPassed + "/" + tests.length);
        return testsPassed == test.length ? 0 : -1;
    }


}
