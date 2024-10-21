package dadkvs.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public abstract class Process {
    private final String[] command;
    private java.lang.Process process;
    private BufferedReader processStdOut;
    private BufferedReader processStdErr;
    private BufferedWriter processInput;

    public Process(String[] command) {
        this.command = command;
    }

    public void start() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false); // Separate stderr from stdout
        this.process = builder.start();

        // Create separate streams for reading from stdout and stderr
        processStdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        processStdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Create a separate stream for writing to stdin
        processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    public void kill() {
        if (process != null) {
            process.destroy();
        }
    }

    public String readStdOutLine() throws IOException {
        if (processStdOut != null) {
            return processStdOut.readLine(); // Reads one line from the process' stdout
        }
        return null;
    }

    public String readStdOutAll() throws IOException {
        if (processStdOut != null) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = processStdOut.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }
            return output.toString();
        }
        return null;
    }

    public String readStdErrLine() throws IOException {
        if (processStdErr != null) {
            return processStdErr.readLine(); // Reads one line from the process' stderr
        }
        return null;
    }

    public String readStdErrAll() throws IOException {
        if (processStdErr != null) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = processStdErr.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }
            return output.toString();
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