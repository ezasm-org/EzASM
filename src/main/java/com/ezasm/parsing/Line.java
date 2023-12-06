package com.ezasm.parsing;

import com.ezasm.instructions.DispatchInstruction;
import com.ezasm.instructions.InstructionDispatcher;
import com.ezasm.instructions.targets.IAbstractTarget;
import com.ezasm.instructions.targets.input.IAbstractInput;
import com.ezasm.instructions.targets.input.ImmediateInput;
import com.ezasm.instructions.targets.input.LabelReferenceInput;
import com.ezasm.instructions.targets.input.StringInput;
import com.ezasm.instructions.targets.inputoutput.DereferenceInputOutput;
import com.ezasm.instructions.targets.inputoutput.RegisterInputOutput;
import com.ezasm.util.RawData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The representation of a line of code. Consists of an Instruction object and the arguments thereof.
 */
public class Line {

    private final Instruction instruction;
    private final IAbstractTarget[] arguments;
    private final String label;
    private final List<String> stringImmediates;

    /**
     * Creates and validates a line based on the given tokens.
     *
     * @param instruction the String representing the instruction.
     * @param arguments   the variable sized arguments token String list.
     * @throws ParseException if any of the given String tokens cannot be parsed into their corresponding types.
     */
    public Line(String instruction, String[] arguments) throws ParseException {
        if (Lexer.isLabel(instruction)) {
            this.label = instruction.substring(0, instruction.length() - 1);
            this.instruction = null;
            this.arguments = null;
            this.stringImmediates = null;
            if (arguments != null && arguments.length > 0) {
                throw new ParseException(String.format("Unexpected token after label: '%s'", arguments[0]));
            }
            return;
        } else if (!Lexer.isInstruction(instruction)) {
            throw new ParseException("Error parsing instruction '" + instruction + "'");
        }

        this.arguments = new IAbstractTarget[arguments.length];
        this.stringImmediates = new ArrayList<>();
        this.label = null;

        // Determine the type of each argument and create the token respectively
        for (int i = 0; i < arguments.length; ++i) {
            if (Lexer.looksLikeImmediate(arguments[i].toLowerCase())) {
                this.arguments[i] = new ImmediateInput(Lexer.textToBytes(arguments[i].toLowerCase()));
            } else if (Lexer.looksLikeCharacterImmediate(arguments[i])) {
                this.arguments[i] = new ImmediateInput(new RawData(Lexer.getCharacterImmediate(arguments[i])));
            } else if (Lexer.looksLikeStringImmediate(arguments[i])) {
                String input = Lexer.getStringImmediate(arguments[i]);
                this.arguments[i] = new StringInput(input);
                this.stringImmediates.add(input);
            } else if (Lexer.isRegister(arguments[i])) {
                this.arguments[i] = new RegisterInputOutput(arguments[i]);
            } else if (Lexer.looksLikeDereference(arguments[i])) {
                this.arguments[i] = new DereferenceInputOutput(arguments[i]);
            } else if (Lexer.looksLikeLabelReference(arguments[i])) {
                this.arguments[i] = new LabelReferenceInput(arguments[i]);
            } else {
                // The argument did not match any of the given types
                throw new ParseException("Error parsing token '" + arguments[i] + "'");
            }
        }

        if (!InstructionDispatcher.getInstructions().containsKey(instruction)) {
            throw new ParseException(String.format("Invalid instruction: %s", instruction));
        }

        DispatchInstruction dispatchInstruction = InstructionDispatcher.getInstruction(instruction, getArgumentTypes());

        if (dispatchInstruction == null) {
            throw new ParseException(String.format("Instruction %s could not be matched for the given %d argument(s)",
                    instruction, getArgumentTypes().length));
        }

        this.instruction = new Instruction(instruction);
    }

    /**
     * Gets the instruction token of this line.
     *
     * @return the instruction token.
     */
    public Instruction getInstruction() {
        return instruction;
    }

    /**
     * Gets the "right-hand side" token types of this line.
     *
     * @return the "right-hand side" token types.
     */
    public Class<?>[] getArgumentTypes() {
        Class<?>[] types = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            types[i] = arguments[i].getClass();
        }

        return types;
    }

    public void setArgument(IAbstractTarget iat, int pos) {
        arguments[pos] = iat;
    }

    /**
     * Gets the "right-hand side" tokens of this line.
     *
     * @return the "right-hand side" tokens.
     */
    public IAbstractTarget[] getArguments() {
        return arguments;
    }

    /**
     * Check if the line is a label.
     *
     * @return true if the line is a label, false otherwise.
     */
    public boolean isLabel() {
        return label != null;
    }

    /**
     * Gets the label text of that line.
     *
     * @return the label text of that line.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the list of string immediates inside this line.
     *
     * @return the list of string immediates inside this line.
     */
    public List<String> getStringImmediates() {
        return Objects.requireNonNullElse(stringImmediates, new ArrayList<>());
    }

    @Override
    public String toString() {
        return instruction.text();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Line line = (Line) o;
        return Objects.equals(instruction, line.instruction) && Arrays.equals(arguments, line.arguments)
                && Objects.equals(label, line.label);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(instruction, label);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }

}
