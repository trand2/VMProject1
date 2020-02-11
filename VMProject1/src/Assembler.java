import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class Assembler {
    private static int PC = 3000;
    private static int SIZE_OF_INT = 4;
    private static int SIZE_OF_MEM = 4000;
    private static int SIZE_OF_INSTRUCTION = 12;
//    private static byte[] MEM = new byte[1000];
    private static ByteBuffer MEM = ByteBuffer.allocate(SIZE_OF_MEM);
    private static HashMap<String, Integer> symbolTable = new HashMap<>();
    static int[] Reg = new int[8];
    private static boolean secondPass = false;
    enum Instructions {
        MOV(7),
        STR(9),
        LDR(10),
        LDB(12),
        ADD(13),
        SUB(15),
        MUL(16),
        DIV(17),
        TRP(21);

        public final int opcodeValue;

        private Instructions(int opcodeValue) {
            this.opcodeValue = opcodeValue;
        }
    }
    enum Directives {INT, BYT}



    public static void main(String[] args) throws IOException {
        String fileName = args[0];

        //First pass
        readLine(fileName);
        PC = 0;

        //Second pass
        secondPass = true;
        readLine(fileName);

        for (int i = 0; i < SIZE_OF_MEM; i++) {
            System.out.println(i + ": " + MEM.get(i));
        }

        executeVM();
//
//        System.out.println(MEM.getInt(3060));
//        char test = (char)MEM.get(3076);
//        System.out.println(test);
//        System.out.println(symbolTable);
    }

    private static void readLine(String fileName) throws IOException {
        BufferedReader inputStream = new BufferedReader(new FileReader(fileName));
        String content = "";

        //Read each line until end of file
        while ((content = inputStream.readLine()) != null) {
            parseLine(content);
        }

        inputStream.close();
    }

    private static void parseLine(String content) {
        String[] data;

        //Do not read comments
        if (!content.contains("//")) {

            //split on whitespace
            data = content.split("\\s+");

            //Do not read blank lines
            if (data.length != 1) {

                if (data[0].matches("space")) {
                    data[2] = " ";
                } else if (data[0] == "newLine") {
                    data[2] = String.valueOf('\n');;
                }

                //Find operator
                if (isOperator(data)) {

                    //check if this is the first or second pass
                    if (secondPass) {

                        instToBytecode(data);
                    }
                    else {
//                        storeOperator(data);
                    }

                //Find Directive
                } else if (isDirective(data)) {

                    //check if this is the first or second pass
                    if(secondPass) {
                        dataToMemory(data);
                    } else {
                        storeDirective(data);
                    }

                // Must be a typo somewhere
                } else {
                    System.out.println(Arrays.toString(data) + " is not a valid command.");
                }
            }
        }
    }

    //Check to see if the label is an operator
    private static boolean isOperator(String[] data) {
        if (data[0].matches("STR|LDR|LDB|ADD|SUB|MUL|DIV|TRP|MOV")) {
            return true;
        } else {
            return false;
        }
    }

    // Check to see if the label is a directive
    private static boolean isDirective(String[] data) {
        if (data[1].matches(".BYT|.INT")) {
            return true;
        } else {
            return false;
        }
    }

    //Store the operator label into the symbol table and update PC accordingly
//    private static void storeOperator(String[] operator) {
//
//        PC += SIZE_OF_INSTRUCTION;
//
//    }

    //Store the directive label into the symbol table  and update PC accordingly
    private static void storeDirective(String[] directive) {

        if (!symbolTable.containsKey(directive[0])) {
            symbolTable.put(directive[0], PC);

            //Store 1 byte of space if directive is of type .BYT
            if (directive[1].equals(".BYT")) {
                PC++;

            //Store 4 bytes of space if directive is of type .INT
            } else if (directive[1].equals(".INT")) {
                PC += SIZE_OF_INT;
            }
        }
    }

    // Convert instruction to bytecode and store in memory
    private static void instToBytecode(String[] instruction) {
        byte[] labelByte;
        int regValue;
        int labelAddress;
        byte labelValue;

        if (instruction[0].matches("STR|LDR|LDB")) {
            //Store label into memory
            MEM.position(PC).putInt(Instructions.valueOf(instruction[0]).opcodeValue);
            PC += 4;

            regValue = getRegNum(instruction[1]);

            //Check if it is a valid register (0-7)
            if (isValidReg(regValue)) {
                MEM.position(PC).putInt(regValue);
                PC += 4;
            }

            labelAddress = symbolTable.get(instruction[2]);


            labelValue = MEM.get(labelAddress);


            MEM.position(PC).putInt(labelValue);
            PC += 4;

        } else if (instruction[0].matches("MOV|ADD|SUB|DIV|MUL")) {
            MEM.position(PC).putInt(Instructions.valueOf(instruction[0]).opcodeValue);
            PC += 4;

            // Check both registers are valid
            for (int i = 1; i < 3; i++) {

                regValue = getRegNum(instruction[i]);

                //Check if it is a valid register (0-7)
                if (isValidReg(regValue)) {
                    MEM.position(PC).putInt(regValue);
                    PC += 4;
                }
            }

        } else if (instruction[0].equals("TRP")) {
            int i = Integer.parseInt(instruction[1]);
            MEM.position(PC).putInt(Instructions.valueOf(instruction[0]).opcodeValue);
            PC += 4;
            MEM.putInt(i);
            PC += 8;
        } else {
            System.out.println("Invalid instruction - " + instruction[0]);
        }

        //System.out.println("Store Directive " + Arrays.toString(instruction));

        //instructionByte = instruction[2].getBytes();

        //MEM.put(instructionByte);

    }

    private static void dataToMemory(String[] data) {
        String label;
        String type;
        int intValue;
        String value;
        byte[] charValue = new byte[1];
        int location;

        label = data[0];
        location = symbolTable.get(label);

        type = data[1];

        if (type.matches(".INT")) {
            intValue = Integer.parseInt(data[2]);
            MEM.position(location);
            MEM.putInt(intValue);
        } else if (type.matches(".BYT")) {

            if (!label.matches("newLine")) {
                value = data[2];
                charValue = value.getBytes();
            } else {
                char newLine = 10;
                charValue[0] = (byte)newLine;
            }

            MEM.position(location);
            MEM.put(charValue[0]);
        }
    }

    private static int getRegNum(String register) {
        int regValue;
        String[] splitReg;

        splitReg = register.split("");
        regValue = Integer.parseInt(splitReg[1]);

        return regValue;
    }

    private static boolean isValidReg(int regValue) {
        if (regValue < 8) {
            return true;
        } else {
            System.out.println("Not a valid register number");
            return false;
        }
    }

    private static void executeVM() {
        PC = 0;
        InstructionRegister IR;
        Boolean running = true;


        while (running) {
            IR = fetch(PC);


            switch (IR.opCode) {
                case 7: //MOV
                    Reg[IR.opd1] = Reg[IR.opd2];
                    PC += 12;
                    break;
                case 9: //STR
                    MEM.putInt(IR.opd2, Reg[IR.opd1]);
                    PC += 12;
                    break;
                case 10: //LDR
                    Reg[IR.opd1] = IR.opd2;
//                    Reg[IR.opd1] = MEM.getInt(IR.opd2);
                    PC += 12;
                    break;
                case 12: //LDB
                    Reg[IR.opd1] = IR.opd2;
//                    Reg[IR.opd1] = MEM.get(IR.opd2);
                    PC += 12;
                    break;
                case 13: //ADD
                    Reg[IR.opd1] = Reg[IR.opd1] + Reg[IR.opd2];
                    PC += 12;
                    break;
                case 15: //SUB
                    Reg[IR.opd1] = Reg[IR.opd1] - Reg[IR.opd2];
                    PC += 12;
                    break;
                case 16: //MUL
                    Reg[IR.opd1] = Reg[IR.opd1] * Reg[IR.opd2];
                    PC += 12;
                    break;
                case 17: //DIV
                    Reg[IR.opd1] = Reg[IR.opd1] / Reg[IR.opd2];
                    PC += 12;
                    break;
                case 21: //TRP
                    if (IR.opd1 == 0) { // Execute STOP trap routine. 0, stop program
                        running = false;
                        System.exit(0);
                        PC += 12;
                        break;
                    } else if (IR.opd1 == 1) { //write integer to standard out
                        //TODO
                        break;
                    } else if (IR.opd1 == 2) { //read an integer from standard in
                        //TODO
                        break;
                    } else if (IR.opd1 == 3) { //write single character to standard out
                        System.out.print((char)Reg[3]);
                        PC += 12;
                        break;
                    } else if (IR.opd1 == 4){ //read a single character from standard in
                        //TODO
                        break;
                    } else if (IR.opd1 == 99) { //DEBUG (OPTIONAL)
                        break;
                    } else {
                        System.out.println("Invalid TRP command. Try again");
                        PC += 12;
                        System.exit(0);
                        break;
                    }

            }
        }
    }

    private static InstructionRegister fetch(int pc) {

        InstructionRegister IR;
        int opcode = MEM.getInt(pc);
        int opd1 = MEM.getInt(pc+4);
        int opd2 = 0;

        //If the opcode equals an instruction that contains a label then store the address of the label in opd2
//            if (opcode == 9 || opcode == 10 || opcode == 12) {
//                char value = (char)MEM.getInt(PC+8);
//                String stringValue = String.valueOf(value);
//                opd2 = symbolTable.get(stringValue);
//            } else
            if (opcode != 21) { //If it is not the TRP command then get the next value otherwise leave it at 0
            opd2 = MEM.getInt(pc+8);
        }

        IR = new InstructionRegister(opcode, opd1, opd2);

        return IR;
    }
}

class InstructionRegister {
    int opCode;
    int opd1;
    int opd2;

    InstructionRegister(int opCode, int opd1, int opd2) {
        this.opCode = opCode;
        this.opd1 = opd1;
        this.opd2 = opd2;
    }
}
