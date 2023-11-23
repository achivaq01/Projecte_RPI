package com.project;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;


class ThreadManager {
    private final List<JavaCommand> commandQueue;
    private Thread currentThread;
    private Boolean alive;

    public ThreadManager() {
        super();

        commandQueue = new CopyOnWriteArrayList<>();
        currentThread = new Thread(this::executeCommands);
        alive = true;
    }

    public void start() {
        currentThread.start();
    }

    public void addQueue(String command) {
        commandQueue.add(JavaCommand.getInstance(command));

    }

    private void executeCommands() {
        while (alive) {
            executeCommand();
            
            try {
            TimeUnit.SECONDS.sleep(5);

            } catch (InterruptedException e) {
                currentThread.interrupt();

            }
        }
    }

    public void stop() {
        alive = false;
    }

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

    public void interrupt() {
        currentThread.interrupt();

    }

}
