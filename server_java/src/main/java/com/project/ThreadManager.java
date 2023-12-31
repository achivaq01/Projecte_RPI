package com.project;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;


class ThreadManager {
    private final List<JavaCommand> commandQueue;
    private Thread currentThread;
    private Boolean alive;
    private String lastCommand;

    /**
     * class constructor
     * 
     */
    public ThreadManager() {
        super();

        commandQueue = new CopyOnWriteArrayList<>();
        currentThread = new Thread(this::executeCommands);
        alive = true;
    }

    /**
     * thread start
     * 
     */
    public void start() {
        currentThread.start();
    }

    /**
     * code to add command to queue
     * 
     * @param command
     */
    public void addQueue(String command) {
        lastCommand = command;
        commandQueue.add(JavaCommand.getInstance(command));

    }

    /**
     * thread main execution bucle
     * 
     */
    private void executeCommands() {
        while (alive) {
            executeCommand();
            
            try {
            TimeUnit.SECONDS.sleep(2);

            } catch (InterruptedException e) {
                currentThread.interrupt();

            }
        }
    }

    /**
     * thread stop
     * 
     */
    public void stop() {
        alive = false;
    }

    /**
     * thread code to execute queue javaCommands 
     * 
     */
    private void executeCommand() {
        if (commandQueue.isEmpty()) {
            return;
        }

        if (currentThread.isAlive()) {
            interrupt();

        }

        int size = commandQueue.size() - 1;
        currentThread = new Thread(commandQueue.get(size));
        commandQueue.clear();
        currentThread.start();

    }

    /**
     * interrupt thread current process
     * 
     */
    public void interrupt() {
        currentThread.interrupt();

    }

    /**
     * retrieve string from the last command
     * 
     * @return
     */
    public String getLastCommand() {
        return lastCommand;
    }

}
