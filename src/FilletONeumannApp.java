import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

@SuppressWarnings("ALL")
public class FilletONeumannApp implements FilletONeumannInterface {

    Hashtable<String, Integer> registers;
    int[] memory;
    int pc = 0;
    int temppc = 0;
    int instructioncounter;
    boolean fetchflag;

    int decode;
    boolean decodeflag;
    int[] decodeflags;
    int[] decodedata;

    int execute;
    int[] executedata;
    int[] executeflags;
    boolean executeflag;

    int Insmemory;
    boolean memoryflag;
    int[] memorydata;
    int[] memoryflags;

    int write;
    int[] writedata;
    int[] writeflags;
    boolean writeflag;

    public FilletONeumannApp() throws Exception {

        memory = new int[2048];
        registers = new Hashtable<>();

        fetchflag = true;
        decodeflags = new int[9];
        executeflags = new int[9];
        memoryflags = new int[9];
        writeflags = new int[9];
        executedata = new int[8];
        memorydata = new int[8];
        writedata = new int[8];
        decodedata = new int[8];
        for (int i = 0; i <= 31; i++) {
            registers.put("R" + i, 0);
        }
        parsefile();
        clockmanager();
    }

    public static int getBinaryLength(int n) {
        int length = 0;
        while (n > 0) {
            length += 1;
            n /= 2;
        }
        return length;
    }

    public static int concat(int m, int n) {

        // Find binary length of n
        int length = getBinaryLength(n);

        // left binary shift m and then add n
        return (m << length) + n;
    }

    public static void main(String[] args) throws Exception {
        new FilletONeumannApp();
    }

    //[RegDst , Jump , Branch , MemRead , MemtoReg , ALUOp , MemWrite , ALUSrc , RegWrite ]

    void parsefile() throws Exception {
        String line;
        String splitBy = " ";
        try {
            BufferedReader br = new BufferedReader(new FileReader("src\\code\\code.txt"));
            while ((line = br.readLine()) != null) // returns a Boolean value
            {
                String[] data = line.split(splitBy);
                int Instruction;
                int opcode;
                int R1;
                int R2;
                int R3;
                int SHAMT;
                int immediate;
                int jumpaddress;
                String type;
                switch (data[0].toUpperCase()) {
                    case ("ADD"):
                        opcode = 0;
                        type = "R";
                        break;
                    case ("SUB"):
                        opcode = 1;
                        type = "R";
                        break;
                    case ("MULI"):
                        opcode = 2;
                        type = "I";
                        break;
                    case ("ADDI"):
                        opcode = 3;
                        type = "I";
                        break;
                    case ("BNE"):
                        opcode = 4;
                        type = "I";
                        break;
                    case ("ANDI"):
                        opcode = 5;
                        type = "I";
                        break;
                    case ("ORI"):
                        opcode = 6;
                        type = "I";
                        break;
                    case ("J"):
                        opcode = 7;
                        type = "J";
                        break;
                    case ("SLL"):
                        opcode = 8;
                        type = "R";
                        break;
                    case ("SRL"):
                        opcode = 9;
                        type = "R";
                        break;
                    case ("LW"):
                        opcode = 10;
                        type = "I";
                        break;
                    case ("SW"):
                        opcode = 11;
                        type = "I";
                        break;
                    default:
                        throw new Exception("This operation (" + data[0] + ") is not supported");
                }
                //cheching Format
                if (type.equals("J")) {
                    if (data.length != 2) {
                        throw new Exception("Missing or Extra Data");
                    }
                    jumpaddress = Integer.parseInt(data[1]);
                    Instruction = opcode << 28 | jumpaddress;
                } else if (type.equals("R")) {
                    //Checking R Type
                    if (data.length != 4) {
                        throw new Exception("Missing or Extra Data");
                    }
                    //Checking R1 Format
                    R1 = Integer.parseInt(data[1].substring(1));
                    if ((data[1].charAt(0) != ('R')) && (data[1].charAt(0) != ('r')))
                        throw new Exception("Wrong input");
                    if (R1 < 0 || R1 > 31) throw new Exception("Wrong input");
                    //Checking R2 Format
                    R2 = Integer.parseInt(data[2].substring(1));
                    if ((data[2].charAt(0) != ('R')) && (data[2].charAt(0) != ('r')))
                        throw new Exception("Wrong input");
                    if (R2 < 0 || R2 > 31) throw new Exception("Wrong input");

                    //Checking 	NOT SLL SRL Format
                    if (opcode != 8 && opcode != 9) {
                        R3 = Integer.parseInt(data[3].substring(1));
                        if ((data[3].charAt(0) != ('R')) && (data[3].charAt(0) != ('r')))
                            throw new Exception("Wrong input");
                        if (R3 < 0 || R3 > 31) throw new Exception("Wrong input");
                        Instruction = opcode << 28 | R1 << 23 | R2 << 18 | R3 << 13;
                    } else {
                        SHAMT = Integer.parseInt(data[3]);
                        Instruction = opcode << 28 | R1 << 23 | R2 << 18 | SHAMT;
                    }
                } else {
                    //Checking I Type
                    if (data.length != 4) {
                        throw new Exception("Missing or Extra Data");
                    }
                    R1 = Integer.parseInt(data[1].substring(1));
                    if ((data[1].charAt(0) != ('R')) && (data[1].charAt(0) != ('r')))
                        throw new Exception("Wrong input");
                    if (R1 < 0 || R1 > 31) throw new Exception("Wrong input");

                    R2 = Integer.parseInt(data[2].substring(1));
                    if ((data[2].charAt(0) != ('R')) && (data[2].charAt(0) != ('r')))
                        throw new Exception("Wrong input");
                    if (R2 < 0 || R2 > 31) throw new Exception("Wrong input");
                    //immediate value
                    immediate = 0b00000000000000111111111111111111 & Integer.parseInt(data[3]);
                    Instruction = opcode << 28 | R1 << 23 | R2 << 18 | immediate;
                }
                //putting in memory
                memory[instructioncounter] = Instruction;
                instructioncounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //index:[	  0         1          2           3        4       5          6           7	]
    //Data : [ R1 Name , R2 Value , R3 Value ,Immediate , SHamt , address ,Alu Result , R1 Value ]

    private void clockmanager() throws Exception {
        int cycle = 1;
        while (true) {
            if (pc >= instructioncounter && !decodeflag && !executeflag && !memoryflag && !writeflag) {
                //Finishes
                System.out.print("Registers : PC = " + pc);
                for (int i = 0; i < 31; i++) {
                    System.out.print("  R" + i + " = " + registers.get("R" + i));
                }
                System.out.println("  R31 = " + registers.get("R31"));
                System.out.print("Memory : ");
                for (int i = 0; i < 2048; i++) {
                    System.out.print("  Memory[" + i + "]=" + memory[i]);
                }
                break;
            }

            if (!fetchflag && !decodeflag) {
                //Flushing Occurs Here in case Branch or Jump
                fetchflag = true;
            }
            System.out.print("Cycle " + cycle + ":");

            if (writeflag) {
                System.out.print("  W");
                WriteBack();
            }
            if (memoryflag) {
                System.out.print("  M");
                MemoryReadWrite();
            }
            if (executeflag) {
                System.out.print("  E");
                InstructionExecute(cycle);
            }
            if (decodeflag) {
                System.out.print("  D");
                InstructionDecode(cycle);
            }
            if (fetchflag && cycle % 2 == 1)
                InstructionFetch();
            cycle++;
            System.out.println();

        }

    }

    public void InstructionFetch() {
        if (pc < instructioncounter) {
            int x = memory[pc];
            System.out.print("  F(instr:" + x + ")");
            decode = x;
            temppc = pc;
            pc++;
            decodeflag = true;
            fetchflag = false;
        }
        temppc++;
    }

    public void InstructionDecode(int clock) {
        if (clock % 2 == 0) {
            int[] flag = new int[9];
            int opcode = (decode & 0b11110000000000000000000000000000) >> 28;
            switch (opcode) {
                case 0:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 0;
                    flag[6] = 0;
                    flag[7] = 0;
                    flag[8] = 1;
                    break;
                case 1:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 1;
                    flag[6] = 0;
                    flag[7] = 0;
                    flag[8] = 1;
                    break;

                case 2:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 2;
                    flag[6] = 0;
                    flag[7] = 1;
                    flag[8] = 1;
                    break;
                case 3:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 3;
                    flag[6] = 0;
                    flag[7] = 1;
                    flag[8] = 1;
                    break;

                case 4:
                    flag[0] = -1;
                    flag[1] = 0;
                    flag[2] = 1;
                    flag[3] = 0;
                    flag[4] = -1;
                    flag[5] = 4;
                    flag[6] = 0;
                    flag[7] = 0;
                    flag[8] = 0;
                    break;

                case 5:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 5;
                    flag[6] = 0;
                    flag[7] = 1;
                    flag[8] = 1;
                    break;
                case 6:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 6;
                    flag[6] = 0;
                    flag[7] = 1;
                    flag[8] = 1;
                    break;

                case 7:
                    flag[0] = -1;
                    flag[1] = 1;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = -1;
                    flag[5] = 7;
                    flag[6] = 0;
                    flag[7] = -1;
                    flag[8] = 0;
                    break;

                case -8:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 8;
                    flag[6] = 0;
                    flag[7] = 0;
                    flag[8] = 1;
                    break;
                case -7:
                    flag[0] = 1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = 0;
                    flag[5] = 9;
                    flag[6] = 0;
                    flag[7] = 0;
                    flag[8] = 1;
                    break;
                case -6:
                    flag[0] = 0;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 1;
                    flag[4] = 1;
                    flag[5] = 10;
                    flag[6] = 0;
                    flag[7] = 1;
                    flag[8] = 1;
                    break;
                case -5:
                    flag[0] = -1;
                    flag[1] = 0;
                    flag[2] = 0;
                    flag[3] = 0;
                    flag[4] = -1;
                    flag[5] = 11;
                    flag[6] = 1;
                    flag[7] = 1;
                    flag[8] = 0;
                    break;


                default:
                    System.out.println("Instruction Cannot be executed");

            }
            decodeflags = flag;
            printop(flag, decode);
            int R1 = (decode & 0b00001111100000000000000000000000) >> 23;
            int R2 = (decode & 0b00000000011111000000000000000000) >> 18;
            int R3 = (decode & 0b00000000000000111110000000000000) >> 13;
            String R1Name = "R" + R1;
            String R2Name = "R" + R2;
            String R3Name = "R" + R3;
            int R1Value = registers.get(R1Name);
            int R2Value = registers.get(R2Name);
            int R3Value = registers.get(R3Name);
            int SHAMT = decode & 0b00000000000000000001111111111111;
            int immediate = decode & 0b00000000000000111111111111111111;
            int address = decode & 0b00001111111111111111111111111111;
            int lfb = (immediate & 0b100000000000000000) >> 17;
            if (lfb == 1)
                immediate = immediate | 0b11111111111111000000000000000000;//Signed
            int[] data = new int[8];
            data[0] = R1;
            data[1] = R2Value;
            data[2] = R3Value;
            data[3] = immediate;
            data[4] = SHAMT;
            data[5] = address;
            data[7] = R1Value;
            decodedata = data;
        } else {
            //Dummy one just transferring data
            executedata = decodedata;
            printstatement(); //Printint Operation with data
            decodeflag = false;
            execute = decode;
            executeflag = true;
            fetchflag = true;
            executeflags = decodeflags;
        }
    }

    private void printstatement() {
        switch (decodeflags[5]) {
            case (0):
                System.out.print("(add instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,r3= " + executedata[2] + ")");
                break;
            case (1):
                System.out.print("(sub instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,r3= " + executedata[2] + ")");
                break;
            case (2):
                System.out.print("(muli instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            case (3):
                System.out.print("(addi instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            case (4):
                System.out.print("(bne instr:" + decode + ",r1= " + executedata[7] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            case (5):
                System.out.print("(andi instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            case (6):
                System.out.print("(ori instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            case (7):
                System.out.print("(j instr:" + decode + ",address =" + executedata[5] + ")");
                break;
            case (8):
                System.out.print("(sll instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,SHAMT =" + executedata[4] + ")");
                break;
            case (9):
                System.out.print("(srl instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,SHAMT =" + executedata[4] + ")");
                break;
            case (10):
                System.out.print("(lw instr:" + decode + ",r1= R" + executedata[0] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            case (11):
                System.out.print("(sw instr:" + decode + ",r1= " + executedata[7] + " ,r2= " + executedata[1] + " ,IMM =" + executedata[3] + ")");
                break;
            default:
                break;
        }

    }

    public void InstructionExecute(int clock) {
        if (clock % 2 == 1) {
            //Cycle 2
            int immediate = executedata[3];
            int SHAMT = executedata[4];
            int address = executedata[5];
            switch (executeflags[5]) {
                case (0):
                    executedata[6] = ADD(executedata[1], executedata[2]);
                    break;
                case (1):
                    executedata[6] = SUB(executedata[1], executedata[2]);
                    break;
                case (2):
                    executedata[6] = MULi(executedata[1], immediate);
                    break;
                case (3):
                case (11):
                case (10):
                    executedata[6] = ADDi(executedata[1], immediate);
                    break;
                case (4):
                    BNE(executedata[7], executedata[1], immediate);
                    break;
                case (5):
                    executedata[6] = ANDi(executedata[1], immediate);
                    break;
                case (6):
                    executedata[6] = ORi(executedata[1], immediate);
                    break;
                case (7):
                    J(address);
                    break;
                case (8):
                    executedata[6] = SLL(executedata[1], SHAMT);
                    break;
                case (9):
                    executedata[6] = SRL(executedata[1], SHAMT);
                    break;
                default:
                    break;
            }
            printstatement2();
            memoryflag = true;
            Insmemory = execute;
            memoryflags = executeflags;
            memorydata = executedata;
            executeflag = false;
        } else
            //Dummy Cycle (Cycle 1 )
            printop(executeflags, execute);
    }

    private void printop(int[] flag, int x) {
        switch (flag[5]) {
            case (0):
                System.out.print("(add instr:" + x + ")");
                break;
            case (1):
                System.out.print("(sub instr:" + x + ")");
                break;
            case (2):
                System.out.print("(multi instr:" + x + ")");
                break;
            case (3):
                System.out.print("(addi instr:" + x + ")");
                break;
            case (4):
                System.out.print("(bne instr:" + x + ")");
                break;
            case (5):
                System.out.print("(andi instr:" + x + ")");
                break;
            case (6):
                System.out.print("(ori instr:" + x + ")");
                break;
            case (7):
                System.out.print("(j instr:" + x + ")");
                break;
            case (8):
                System.out.print("(sll instr:" + x + ")");
                break;
            case (9):
                System.out.print("(srl instr:" + x + ")");
                break;
            case (10):
                System.out.print("(lw instr:" + x + ")");
                break;
            case (11):
                System.out.print("(sw instr:" + x + ")");
                break;
            default:
                break;
        }

    }

    private void printstatement2() {
        printop(executeflags, execute);
        //Not Branch or Jump
        if (executeflags[5] != 4 && executeflags[5] != 7) {
            System.out.print("(ALUVAlue= " + executedata[6] + ")");
        }
    }

    public void MemoryReadWrite() throws Exception {
        printop(memoryflags, Insmemory);
        //Load
        if (memoryflags[3] == 1) {
            int ALUValue = memorydata[6];
            if (ALUValue < 1024 || ALUValue > 2047)
                throw new Exception("Address out of bound");
            memorydata[6] = memory[ALUValue];
        }
        //Store
        if (memoryflags[6] == 1) {
            if (memorydata[6] < 1024 || memorydata[6] > 2047) throw new Exception("Address out of bound");
            memory[memorydata[6]] = memorydata[7];
            System.out.print(" (Memory Address " + memorydata[6] + " updated to " + memorydata[7] + ")");
        }
        memoryflag = false;
        write = Insmemory;
        writeflag = true;
        writedata = memorydata;
        writeflags = memoryflags;
    }

    public void WriteBack() {
        String R1Name = "R" + writedata[0];
        printop(writeflags, write);
        if (writeflags[8] == 1 && !R1Name.equals("R0")) {
            registers.replace(R1Name, writedata[6]);
            System.out.print(" (Register " + R1Name + " updated to " + writedata[6] + ")");
        }
        writeflag = false;
    }

    public int ADD(int R2, int R3) {
        return R2 + R3;
    }

    public int SUB(int R2, int R3) {
        return R2 - R3;
    }

    public int MULi(int R2, int IMM) {
        return R2 * IMM;
    }

    public int ADDi(int R2, int IMM) {
        return R2 + IMM;
    }

    public void BNE(int R1, int R2, int IMM) {
        if (R1 - R2 != 0) {
            pc = temppc + IMM - 1; // (-1) in case of fetching the next instruction
            temppc = pc;
            fetchflag = false;
            decodeflag = false;
        }
    }

    public int ANDi(int R2, int IMM) {
        return R2 & IMM;
    }

    public int ORi(int R2, int IMM) {
        return R2 | IMM;
    }

    public void J(int Address) {
        int PC3128 = (temppc - 1) & 0b11110000000000000000000000000000;
        pc = concat(PC3128, Address);
        temppc = pc;
        fetchflag = false;
        decodeflag = false;
    }

    public int SLL(int R2, int SHAMT) {
        return R2 << SHAMT;
    }

    public int SRL(int R2, int SHAMT) {
        return R2 >>> SHAMT;
    }

}
