package com.ezasm.instructions.implementation;

import java.io.*;

import com.ezasm.instructions.targets.inputoutput.IAbstractInputOutput;
import com.ezasm.simulation.Memory;
import com.ezasm.simulation.exception.SimulationInterruptedException;
import com.ezasm.simulation.transform.Transformation;
import com.ezasm.simulation.transform.TransformationSequence;
import com.ezasm.simulation.transform.transformable.FileReadTransformable;
import com.ezasm.simulation.transform.transformable.InputOutputTransformable;
import com.ezasm.simulation.transform.transformable.MemoryTransformable;
import com.ezasm.instructions.Instruction;
import com.ezasm.instructions.targets.input.IAbstractInput;
import com.ezasm.simulation.Simulator;
import com.ezasm.simulation.exception.SimulationException;
import com.ezasm.util.RawData;

import static org.apache.commons.lang3.math.NumberUtils.min;

/**
 * An implementation of standard terminal I/O instructions for simulation.
 */
public class TerminalInstructions {

    public static final InputStream DEFAULT_INPUT_STREAM = System.in;
    public static final OutputStream DEFAULT_OUTPUT_STREAM = System.out;

    private static final StreamManager streams = new StreamManager(DEFAULT_INPUT_STREAM, DEFAULT_OUTPUT_STREAM);

    private final Simulator simulator;

    /**
     * Returns the program I/O streams representation.
     *
     * @return the program I/O streams representation.
     */
    public static StreamManager streams() {
        return streams;
    }

    public TerminalInstructions(Simulator simulator) {
        this.simulator = simulator;
    }

    @Instruction
    public TransformationSequence printi(IAbstractInput input) throws SimulationException {
        streams.write(input.get(simulator).intValue());
        return new TransformationSequence();
    }

    @Instruction
    public TransformationSequence printf(IAbstractInput input) throws SimulationException {
        streams.write(input.get(simulator).floatValue());
        return new TransformationSequence();
    }

    @Instruction
    public TransformationSequence printc(IAbstractInput input) throws SimulationException {
        streams.write((char) input.get(simulator).intValue());
        return new TransformationSequence();
    }

    @Instruction
    public TransformationSequence prints(IAbstractInput input1, IAbstractInput input2) throws SimulationException {
        int address = (int) input1.get(simulator).intValue();
        int index = 0;
        int maxSize = (int) input2.get(simulator).intValue();
        long current = simulator.getMemory().read(address).intValue();

        while (index < maxSize && current != 0) {
            streams.write((char) current);
            index++;
            current = simulator.getMemory().read(address + index * Memory.getWordSize()).intValue();
        }

        return new TransformationSequence();
    }

    @Instruction
    public TransformationSequence prints(IAbstractInput input1) throws SimulationException {
        int address = (int) input1.get(simulator).intValue();
        int index = 0;
        long current = simulator.getMemory().read(address).intValue();

        while (current != 0) {
            streams.write((char) current);
            index++;
            current = simulator.getMemory().read(address + index * Memory.getWordSize()).intValue();
        }

        return new TransformationSequence();
    }

    private interface DataSupplier {
        RawData get() throws SimulationException, SimulationInterruptedException;
    }

    private TransformationSequence read(DataSupplier supplier, IAbstractInputOutput output)
            throws SimulationException, SimulationInterruptedException {
        FileReadTransformable f = new FileReadTransformable(simulator, streams().getCursor());
        InputOutputTransformable io = new InputOutputTransformable(simulator, output);
        RawData data = supplier.get();
        Transformation t1 = f.transformation(new RawData(streams().getCursor()));
        Transformation t2 = io.transformation(data);
        return new TransformationSequence(t1, t2);
    }

    @Instruction
    public TransformationSequence readi(IAbstractInputOutput output)
            throws SimulationException, SimulationInterruptedException {
        return read(() -> new RawData(streams.readLong()), output);
    }

    @Instruction
    public TransformationSequence readf(IAbstractInputOutput output)
            throws SimulationException, SimulationInterruptedException {
        return read(() -> new RawData(streams.readDouble()), output);
    }

    @Instruction
    public TransformationSequence readc(IAbstractInputOutput output)
            throws SimulationException, SimulationInterruptedException {
        return read(() -> new RawData(streams.readChar()), output);
    }

    @Instruction
    public TransformationSequence reads(IAbstractInput input1, IAbstractInput input2)
            throws SimulationException, SimulationInterruptedException {
        int address = (int) input1.get(simulator).intValue();
        int maxSize = (int) input2.get(simulator).intValue();

        FileReadTransformable f = new FileReadTransformable(simulator, streams().getCursor());
        String string = streams.readString();

        if (string.length() >= maxSize) {
            int size = min(maxSize, string.length());

            Transformation[] transformations = new Transformation[size + 1];
            transformations[0] = f.transformation(new RawData(streams().getCursor()));

            for (int i = 1; i < size; ++i) {
                MemoryTransformable m = new MemoryTransformable(simulator, address);
                transformations[i] = m.transformation(new RawData(string.charAt(i - 1)));
                address = address + Memory.getWordSize();
            }
            MemoryTransformable m = new MemoryTransformable(simulator, address);
            transformations[transformations.length - 1] = m.transformation(new RawData('\0'));

            return new TransformationSequence(transformations);
        } // case where input >= size
        else {
            int size = min(maxSize, string.length());

            Transformation[] transformations = new Transformation[size + 1];
            transformations[0] = f.transformation(new RawData(streams().getCursor()));

            for (int i = 0; i < size; ++i) {
                MemoryTransformable m = new MemoryTransformable(simulator, address);
                transformations[i] = m.transformation(new RawData(string.charAt(i)));
                address = address + Memory.getWordSize();
            }
            MemoryTransformable m = new MemoryTransformable(simulator, address);
            transformations[transformations.length - 1] = m.transformation(new RawData('\0'));

            return new TransformationSequence(transformations);
        } // case where input < max size

    }

    @Instruction
    public TransformationSequence reads(IAbstractInput input1)
            throws SimulationException, SimulationInterruptedException {
        int address = (int) input1.get(simulator).intValue();

        FileReadTransformable f = new FileReadTransformable(simulator, streams().getCursor());
        String string = streams.readString();

        int size = string.length();
        Transformation[] transformations = new Transformation[size + 2];
        transformations[0] = f.transformation(new RawData(streams().getCursor()));

        for (int i = 1; i < size + 1; ++i) {
            MemoryTransformable m = new MemoryTransformable(simulator, address);
            transformations[i] = m.transformation(new RawData(string.charAt(i - 1)));
            address = address + Memory.getWordSize();
        }
        MemoryTransformable m = new MemoryTransformable(simulator, address);
        transformations[transformations.length - 1] = m.transformation(new RawData('\0'));

        return new TransformationSequence(transformations);
    }

    @Instruction
    public TransformationSequence readln(IAbstractInput input1, IAbstractInput input2)
            throws SimulationException, SimulationInterruptedException {
        int address = (int) input1.get(simulator).intValue();
        int maxSize = (int) input2.get(simulator).intValue();

        FileReadTransformable f = new FileReadTransformable(simulator, streams().getCursor());
        String string = streams.readLine();

        int size = min(maxSize, string.length());
        Transformation[] transformations = new Transformation[size + 2];
        transformations[0] = f.transformation(new RawData(streams().getCursor()));

        for (int i = 1; i < size + 1; ++i) {
            MemoryTransformable m = new MemoryTransformable(simulator, address);
            transformations[i] = m.transformation(new RawData(string.charAt(i - 1)));
            address = address + Memory.getWordSize();
        }
        MemoryTransformable m = new MemoryTransformable(simulator, address);
        transformations[transformations.length - 1] = m.transformation(new RawData('\0'));

        return new TransformationSequence(transformations);
    }

    @Instruction
    public TransformationSequence readln(IAbstractInput input1)
            throws SimulationException, SimulationInterruptedException {
        int address = (int) input1.get(simulator).intValue();

        FileReadTransformable f = new FileReadTransformable(simulator, streams().getCursor());
        String string = streams.readLine();

        int size = string.length();
        Transformation[] transformations = new Transformation[size + 2];
        transformations[0] = f.transformation(new RawData(streams().getCursor()));

        for (int i = 1; i < size + 1; ++i) {
            MemoryTransformable m = new MemoryTransformable(simulator, address);
            transformations[i] = m.transformation(new RawData(string.charAt(i - 1)));
            address = address + Memory.getWordSize();
        }
        MemoryTransformable m = new MemoryTransformable(simulator, address);
        transformations[transformations.length - 1] = m.transformation(new RawData('\0'));

        return new TransformationSequence(transformations);
    }

}
