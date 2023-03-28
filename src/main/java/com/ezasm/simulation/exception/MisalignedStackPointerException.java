package com.ezasm.simulation.exception;

public class MisalignedStackPointerException extends SimulationException {

    /**
     * Constructs the exception with the given stack pointer.
     *
     * @param sp the stack pointer.
     */
    public MisalignedStackPointerException(long sp) {
        super(String.format("Misaligned stack pointer (sp) value: %d", sp));
    }
}
