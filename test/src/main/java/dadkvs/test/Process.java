package dadkvs.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public abstract class Process {
    private final String command;
    private java.lang.Process process;
    private BufferedReader processError;
    private BufferedWriter processInput;

    public Process(String command) {
        this.command = command;
    }

    public void start() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        builder.redirectErrorStream(false); // Separate stderr from stdout
        this.process = builder.start();

        // Create a separate stream for reading from stderr
        processError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Create a separate stream for writing to stdin
        processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    public void kill() {
        if (process != null) {
            process.destroy();
        }
    }

    public String read() throws IOException {
        if (processError != null) {
            return processError.readLine(); // Reads one line from the process' stderr
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