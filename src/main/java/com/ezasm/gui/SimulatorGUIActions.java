package com.ezasm.gui;

import com.ezasm.parsing.ParseException;
import com.ezasm.simulation.exception.SimulationException;

import java.util.concurrent.locks.LockSupport;

import static com.ezasm.gui.ToolbarFactory.*;

/**
 * Possible actions through the GUI which need to be handled.
 */
public class SimulatorGUIActions {

    /**
     * An enumeration of all possible states of this class. IDLE: has not yet run or was just reset RUNNING: currently
     * executing code PAUSED: is not currently executing but can resume STOPPED: is stopped at the end of a program,
     * error state, or after pressing "Stop"
     */
    private enum State {
        IDLE, STOPPED, RUNNING, PAUSED
    }

    /**
     * The number of milliseconds to wait through each iteration in the paused busy-wait loop. The maximum delay in
     * execution time after the resume button is pressed.
     */
    private static final long LOOP_BUSY_WAIT_MS = 50L;

    private static Thread worker;
    private static State state = State.IDLE;

    /**
     * The delay between instructions.
     */
    private static long instructionDelayMS = 500L;

    /**
     * Updates the delay between instructions to the given value in milliseconds.
     *
     * @param newDelayMS the new delay in between instructions in milliseconds.
     */
    public static void setInstructionDelayMS(long newDelayMS) {
        instructionDelayMS = newDelayMS;
    }

    /**
     * Sets the new state of the GUI simulator.
     *
     * @param newState the state to become.
     */
    private static void setState(State newState) {
        state = newState;

        boolean isDone = state == State.IDLE || state == State.STOPPED;

        Window.getInstance().setEditable(isDone);
        startButton.setEnabled(isDone);
        stopButton.setEnabled(state == State.RUNNING);
        stepButton.setEnabled(state != State.RUNNING);
        pauseButton.setEnabled(state == State.RUNNING);
        resumeButton.setEnabled(state == State.PAUSED);
        resetButton.setEnabled(state != State.IDLE);
    }

    private static void handleProgramCompletion() {
        if (state != State.IDLE) {
            Window.getInstance().handleProgramCompletion();
        }
    }

    /**
     * Handles if the user requests that the program runs one individual line of code from the current state.
     */
    static void step() {
        if (Window.getInstance().getSimulator().isDone() && state == State.PAUSED) {
            setState(State.STOPPED);
            return;
        }
        if (state == State.IDLE || state == State.STOPPED) {
            setState(State.PAUSED);
            try {
                Window.getInstance().parseText();
                System.out.println("** Program starting **");
                startWorker();
            } catch (ParseException e) {
                setState(State.IDLE);
                Window.getInstance().handleParseException(e);
            }
        } else {
            try {
                Window.updateHighlight();
                Window.getInstance().getSimulator().executeLineFromPC();
                Window.updateRegisters();
            } catch (SimulationException e) {
                setState(State.STOPPED);
                System.err.println(e.getMessage());
                handleProgramCompletion();
            }
        }
    }

    /**
     * Handles if the user requests that the program runs automatically until completion, an error occurs, or the user
     * interrupts the execution.
     */
    static void start() {
        try {
            Window.getInstance().setEditable(false);
            Window.getInstance().parseText();
            setState(State.RUNNING);
            System.out.println("** Program starting **");
            startWorker();
            Window.resetHighlight();
            Window.resetInputStream();
        } catch (ParseException e) {
            setState(State.STOPPED);
            Window.getInstance().handleParseException(e);
        }
    }

    /**
     * Handles if the user requests that the running program be forcibly stopped.
     */
    static void stop() {
        Window.resetHighlight();
        setState(State.STOPPED);
        killWorker();
        awaitWorkerTermination();
    }

    /**
     * Handles if the user requests that the running program be temporarily stopped.
     */
    static void pause() {
        setState(State.PAUSED);
    }

    /**
     * Handles if the user requests that the paused program be resumed.
     */
    static void resume() {
        setState(State.RUNNING);
    }

    /**
     * Handles if the user requests that the state of the emulator be reset.
     */
    static void reset() {
        if (state != State.STOPPED) {
            killWorker();
            awaitWorkerTermination();
        }
        setState(State.IDLE);
        Window.getInstance().getSimulator().resetAll();
        Window.updateRegisters();
        Window.resetHighlight();
    }

    /**
     * Handles the main simulation loop. Is meant to run on an asynchronous thread as to not impede GUI execution.
     * Handles changing between states and buffering delays between instructions.
     */
    private static void simulationLoop() {
        while (!Window.getInstance().getSimulator().isDone() && (state == State.RUNNING || state == State.PAUSED)) {
            try {
                Window.updateHighlight();
                Window.getInstance().getSimulator().executeLineFromPC();
                Window.updateRegisters();
            } catch (SimulationException e) {
                Window.getInstance().handleParseException(e);
                setState(State.STOPPED);
                break;
            }
            try {
                LockSupport.parkNanos(instructionDelayMS * 1_000_000);
                while (state == State.PAUSED) { // busy wait
                    LockSupport.parkNanos(LOOP_BUSY_WAIT_MS * 1_000_000);
                }
            } catch (Exception e) {
                break;
            }
        }
        setState(State.STOPPED);
        handleProgramCompletion();
    }

    /**
     * Starts a new worker thread on the current state of the program.
     */
    private static void startWorker() {
        if (worker != null) {
            killWorker();
        }
        worker = new Thread(SimulatorGUIActions::simulationLoop);
        worker.start();
    }

    /**
     * Terminates the currently executing worker.
     */
    private static void killWorker() {
        worker.interrupt();
    }

    /**
     * Waits for the currently executing worker to terminate.
     */
    private static void awaitWorkerTermination() {
        try {
            worker.join(LOOP_BUSY_WAIT_MS);
        } catch (InterruptedException ignored) {
        }
    }

}
