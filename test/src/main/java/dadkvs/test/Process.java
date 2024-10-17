package dadkvs.test;

import java.io.*;

public abstract class Process {
    private String processPath;
    private java.lang.Process process;
    private BufferedReader processOutput;
    private BufferedWriter processInput;

    public Process(String processPath) {
        this.processPath = processPath;
    }

    public void start() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(processPath);
        builder.redirectErrorStream(true); // Merges stderr with stdout
        this.process = builder.start();

        // Create a separate stream for reading from stdout
        processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Create a separate stream for writing to stdin
        processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    public void kill() {
        if (process != null) {
            process.destroy();
        }
    }

    public String read() throws IOException {
        if (processOutput != null) {
            return processOutput.readLine(); // Reads one line from the process' stdout
        }
        return null;
    }

    public void write(String input) throws IOException {
        if (processInput != null) {
            processInput.write(input);
            processInput.newLine();
            processInput.flush(); // Ensures the input is written to the process
        }
    }
}
