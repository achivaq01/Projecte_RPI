package com.project;


import java.io.IOException;

class JavaCommand implements  Runnable{
    private static JavaCommand instance;
    private final String command;
    private final ProcessBuilder builder;
    private Process process;

    /**
     * private constructor of the class
     * 
     * @param command
     */
    private JavaCommand(String command) {
        super();

        this.command = command;
        builder = new ProcessBuilder();
        builder.command("bash", "-c", this.command);
        builder.inheritIO();
    }

    /**
     * singleton method to get the class instance
     * 
     * @param command
     * @return
     */
    public static synchronized JavaCommand getInstance(String command) {
        if(instance == null) {
            instance = new JavaCommand(command);
        }
        if(!instance.getCommand().equals(command)) {
            instance = new JavaCommand(command);
        }
        return instance;
    }

    /**
     * run process
     * 
     */
    @Override
    public void run() {
        try {
            process = builder.start();
            process.waitFor();


        } catch (IOException | InterruptedException ex) {
            process.destroy();
        }
    }

    /**
     * command String getter
     * 
     * @return
     */
    public String getCommand() {
        return command;
    }
}