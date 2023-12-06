package com.ezasm.gui.toolbar;

import com.ezasm.gui.Window;
import com.ezasm.instructions.implementation.TerminalInstructions;
import com.ezasm.gui.menubar.MenubarFactory;
import com.ezasm.parsing.ParseException;
import com.ezasm.simulation.exception.SimulationException;
import com.ezasm.simulation.exception.SimulationInterruptedException;
import com.ezasm.util.SystemStreams;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

import static com.ezasm.gui.toolbar.ToolbarFactory.*;

/**
 * Possible actions through the GUI which need to be handled.
 */
public class SimulatorGuiActions {

    /**
     * An enumeration of all possible states of this class. IDLE: has not yet run or was just reset RUNNING: currently
     * executing code PAUSED: is not currently executing but can resume STOPPED: is stopped at the end of a program,
     * error state, or after pressing "Stop" STEPPING is while the code in a step is running but not yet finished.
     */
    private enum State {
        IDLE, STOPPED, RUNNING, PAUSED, STEPPING
    }

    /**
     * The number of milliseconds to wait through each iteration in the paused busy-wait loop. The maximum delay in
     * execution time after the resume button is pressed.
     */
    private static final long LOOP_BUSY_WAIT_MS = 50L;

    private static Thread worker;
    private static ExecutorService stepThread = Executors.newSingleThreadExecutor();
    private static volatile State state = State.IDLE;

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

        Window.getInstance().getEditor().setEditable(isDone);
        Window.getInstance().getEditorPanes().setEnabled(state != State.RUNNING);
        MenubarFactory.setRedirectionEnable(isDone);
        startButton.setEnabled(isDone);
        stopButton.setEnabled(state == State.RUNNING || state == State.STEPPING);
        stepButton.setEnabled(state != State.RUNNING && state != State.STEPPING);
        stepBackButton.setEnabled(state == State.PAUSED || state == State.STOPPED);
        pauseButton.setEnabled(state == State.RUNNING);
        resumeButton.setEnabled(state == State.PAUSED);
        resetButton.setEnabled(state != State.IDLE);
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
            try {
                setState(State.STEPPING);
                Window.getInstance().parseText();
                Window.getInstance().getConsole().reset();
                resetStepThread();
                SystemStreams.printlnCurrentOut("** Program starting **");
                startWorker();
            } catch (ParseException e) {
                setState(State.IDLE);
                Window.getInstance().handleParseException(e);
            }
        } else if (state == State.PAUSED) {
            setState(State.STEPPING);
            stepThread.execute(() -> {
                try {
                    runOneLine();
                } catch (SimulationInterruptedException ignored) { // Expected interruption from Stop or Reset
                } catch (SimulationException e) {
                    setState(State.STOPPED);
                    Window.getInstance().handleParseException(e);
                }
            });
        }
    }

    /**
     * Handles if the user requests that the program runs one individual line of code from the current state.
     */
    static void stepBack() {
        try {
            resetStepThread();
            if (state == State.STOPPED) {
                setState(State.PAUSED);
                startWorker();
                SystemStreams.printlnCurrentOut("** Stepping back into stopped program **");
            }
            if (Window.getInstance().getSimulator().undoLastTransformations()) {
                // Some inverse transform was executed
                setState(State.PAUSED);
                Window.getInstance().getEditor().updateHighlight();
                Window.getInstance().updateGraphicInformation();
            } else {
                // No transform was executed; we are done
                setState(State.IDLE);
            }
        } catch (SimulationException e) {
            setState(State.STOPPED);
            Window.getInstance().handleParseException(e);
        }
    }

    /**
     * Handles if the user requests that the program runs automatically until completion, an error occurs, or the user
     * interrupts the execution.
     */
    static void start() {
        try {
            Window.getInstance().parseText();
            setState(State.RUNNING);
            Window.getInstance().getConsole().reset();
            SystemStreams.printlnCurrentOut("** Program starting **");
            startWorker();
        } catch (ParseException e) {
            setState(State.IDLE);
            Window.getInstance().handleParseException(e);
        }
    }

    /**
     * Handles if the user requests that the running program be forcibly stopped.
     */
    static void stop() {
        setState(State.STOPPED);
        Window.getInstance().getEditor().resetHighlighter();
        resetStepThread();
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
        stop();
        Window.getInstance().getSimulator().resetAll();
        Window.getInstance().updateGraphicInformation();
        Window.getInstance().getRegisterTable().removeHighlightValue();
        setState(State.IDLE);
    }

    /**
     * Handles the main simulation loop. Is meant to run on an asynchronous thread as to not impede GUI execution.
     * Handles changing between states and buffering delays between instructions.
     */
    private static void simulationLoop() {
        while (!Window.getInstance().getSimulator().isDone()
                && (state == State.RUNNING || state == State.PAUSED || state == State.STEPPING)) {
            try {
                runOneLine();// TODO figure out why this will occasionally break
            } catch (SimulationInterruptedException ignored) { // Expected interruption from Stop or Reset
                break;
            } catch (SimulationException e) {
                Window.getInstance().handleParseException(e);
                break;
            }
            try {
                LockSupport.parkNanos(instructionDelayMS * 1_000_000);
                while (state == State.PAUSED || state == State.STEPPING) { // busy wait
                    LockSupport.parkNanos(LOOP_BUSY_WAIT_MS * 1_000_000);
                }
            } catch (Exception e) {
                break;
            }
        }
        setState(State.STOPPED);
        Window.getInstance().handleProgramCompletion();
    }

    /**
     * Runs one line from the current simulator. Handles state changes dependent on the outcome.
     *
     * @throws SimulationException            if an error occurs in execution.
     * @throws SimulationInterruptedException if an interrupt occurs while executing.
     */
    private static void runOneLine() throws SimulationException, SimulationInterruptedException {
        Window.getInstance().getEditor().updateHighlight();
        Window.getInstance().getSimulator().executeLineFromPC();
        Window.getInstance().updateGraphicInformation();
        SimulationInterruptedException.handleInterrupts();
        if (state == State.STEPPING) {
            setState(State.PAUSED);
        }
    }

    /**
     * Forcibly resets the step thread.
     */
    private static void resetStepThread() {
        stepThread.shutdownNow();
        stepThread = Executors.newSingleThreadExecutor();
    }

    /**
     * Starts a new worker thread on the current state of the program.
     */
    private static void startWorker() {
        killWorker();
        awaitWorkerTermination();
        Window.getInstance().getEditor().resetHighlighter();
        try {
            TerminalInstructions.streams().resetInputStream();
        } catch (SimulationException e) {
            // TODO handle the case where the file is no longer accessible causing an error
            throw new RuntimeException("There was an error reading from the given input file");
        }
        worker = new Thread(SimulatorGuiActions::simulationLoop);
        worker.start();
    }

    /**
     * Terminates the currently executing worker.
     */
    private static void killWorker() {
        if (worker != null) {
            worker.interrupt();
        }
    }

    /**
     * Waits for the currently executing worker to terminate.
     */
    private static void awaitWorkerTermination() {
        if (worker != null) {
            try {
                worker.join(LOOP_BUSY_WAIT_MS);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
