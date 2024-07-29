package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.bookkeeper.bookie.BufferedChannel;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(value = Parameterized.class)
public class BufferedChannelWriteTest {
    public static final Class<? extends Exception> SUCCESS = null;
    /**
     *parameters choice for an easier handle of the buffer
     */
    UnpooledByteBufAllocator allocator = new UnpooledByteBufAllocator(
            /* preferDirect */ true,
            /* useCacheForAllThreads */ false
    );
    /**
     * Category Partitioning for fc is: {validIstance}, {invalidIstance}, {null}, {empty}
     */
    private FileChannel fc;

    /**
     * Category Partitioning for capacity is: {<=0, >0}
     */
    private int capacity;

    /**
     * Category Partitioning for src is: {notEmpty, empty, null, invalidInstance}
     */
    private ByteBuf src;

    /**
     * Category Partitioning for srcSize is: {< capacity, = capacity, > capacity}
     */
    private int srcSize;
    private byte[] data;
    private int numOfExistingBytes;
    private long unpersistedBytesBound;

    private enum STATE_OF_OBJ {
        EMPTY,
        NOT_EMPTY,
        NULL,
        INVALID
    }

    private STATE_OF_OBJ stateOfFc;
    private STATE_OF_OBJ stateOfSrc;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public BufferedChannelWriteTest(WriteInputTuple writeInputTuple) {  // Corretto il nome del costruttore
        this.capacity = writeInputTuple.capacity();
        this.stateOfFc = writeInputTuple.stateOfFc();
        this.stateOfSrc = writeInputTuple.stateOfSrc();
        this.srcSize = writeInputTuple.srcSize();
        this.unpersistedBytesBound = writeInputTuple.unpersistedBytesBound();
        this.numOfExistingBytes = 0;
        if(writeInputTuple.expectedException() != null){
            this.expectedException.expect(writeInputTuple.expectedException());
        }
    }


    @Parameterized.Parameters
    public static Collection<WriteInputTuple> getWriteInputTuples() {
        List<WriteInputTuple> writeInputTupleList = new ArrayList<>();
        //writeInputTupleList.add(new WriteInputTuple(capacity, srcSize,   stateOfFc,          stateOfSrc,             unpersistedBytesBound,  EXPECTED));======================//
        writeInputTupleList.add(new WriteInputTuple(-1, 1, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 0L, Exception.class));     //[1] fault of capacity < 0
        writeInputTupleList.add(new WriteInputTuple(10, 5, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 0L, SUCCESS));             //[2] success
        writeInputTupleList.add(new WriteInputTuple(0, 5, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 0L, Exception.class));      //[3] capacity 0 but no exception is thrown...Timeout throws one -> success, but this is buggy.
        writeInputTupleList.add(new WriteInputTuple(10, 10, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 0L, SUCCESS));            //[4] capacity = src_size, still working
        writeInputTupleList.add(new WriteInputTuple(5, 10, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 0L, SUCCESS));             //[5] forcing flush, success
        writeInputTupleList.add(new WriteInputTuple(10, 0, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NULL, 0L, Exception.class));          //[6] src is null, so we get exception; correct
        writeInputTupleList.add(new WriteInputTuple(10, 5, STATE_OF_OBJ.NOT_EMPTY, STATE_OF_OBJ.NOT_EMPTY, 4L, SUCCESS));         //[7] unpersist–edBytesBound working <br>
        writeInputTupleList.add(new WriteInputTuple(10, 5, STATE_OF_OBJ.NOT_EMPTY, STATE_OF_OBJ.NOT_EMPTY, 6L, SUCCESS));         //[8] correctly, on test 7 and 8
        writeInputTupleList.add(new WriteInputTuple(10, 10, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.INVALID, 0L, Exception.class));      //[9] fault of stateOfSrc == INVALID
        writeInputTupleList.add(new WriteInputTuple(10, 10, STATE_OF_OBJ.NULL, STATE_OF_OBJ.NOT_EMPTY, 0L, Exception.class));     //[10] fault of Fc == null
        writeInputTupleList.add(new WriteInputTuple(10, 10, STATE_OF_OBJ.INVALID, STATE_OF_OBJ.NOT_EMPTY, 0L, Exception.class));  //[11] fault of Fc == INVALID
        writeInputTupleList.add(new WriteInputTuple(10, 0, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.EMPTY, 0L, SUCCESS));                 //[12] src is empty, actually writing nothing

        //after Jacoco report
        //nothing to add, coverage was 100%

        //after badua report
        //writeInputTupleList.add(new WriteInputTuple(10,5,STATE_OF_OBJ.EMPTY,STATE_OF_OBJ.EMPTY,3L,SUCCESS));                       //
        writeInputTupleList.add(new WriteInputTuple(10, 5, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 3L, SUCCESS));            //[14] SUCCESS
        writeInputTupleList.add(new WriteInputTuple(10, 5, STATE_OF_OBJ.EMPTY, STATE_OF_OBJ.NOT_EMPTY, 6L, SUCCESS));            //[15] SUCCESS

        //after pit report



        return writeInputTupleList;
    }

    /**
     * when 0 capacity maybe flush is triggered, not working and tries again creating an infinite loop ?
     **/

    private static final class WriteInputTuple {
        private final int capacity;
        private final int srcSize;
        private final STATE_OF_OBJ stateOfFc;
        private final STATE_OF_OBJ stateOfSrc;
        private final Class<? extends Exception> expectedException;
        private final long unpersistedBytesBound;

        private WriteInputTuple(int capacity,
                                int srcSize,
                                STATE_OF_OBJ stateOfFc,
                                STATE_OF_OBJ stateOfSrc,
                                long unpersistedBytesBound,
                                Class<? extends Exception> expectedException) {
            this.capacity = capacity;
            this.srcSize = srcSize;
            this.stateOfFc = stateOfFc;
            this.stateOfSrc = stateOfSrc;
            this.unpersistedBytesBound = unpersistedBytesBound;
            this.expectedException = expectedException;
        }

        public int capacity() {
            return capacity;
        }

        public int srcSize() {
            return srcSize;
        }

        public STATE_OF_OBJ stateOfFc() {
            return stateOfFc;
        }

        public STATE_OF_OBJ stateOfSrc() {
            return stateOfSrc;
        }

        public Class<? extends Exception> expectedException() {
            return expectedException;
        }

        public long unpersistedBytesBound() {
            return unpersistedBytesBound;
        }
    }

    @BeforeClass
    public static void setUpOnce(){
        File newLogFileDirs = new File("/Users/matteocalzetta/Documents/BufChanWriteTest");
        if(!newLogFileDirs.exists()){
            newLogFileDirs.mkdirs();
        }

        File oldLogFile = new File("/Users/matteocalzetta/Documents/BufChanWriteTest/writeToThisFile.log");
        if(oldLogFile.exists()){
            oldLogFile.delete();
        }
    }

    @Before
    public void setUpEachTime(){
        try {
            Random random = new Random(System.currentTimeMillis());
            if (this.stateOfFc == STATE_OF_OBJ.NOT_EMPTY || this.stateOfFc == STATE_OF_OBJ.EMPTY) {
                if(this.stateOfFc == STATE_OF_OBJ.NOT_EMPTY) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream("/Users/matteocalzetta/Documents/BufChanWriteTest/writeToThisFile.log")) {
                        this.numOfExistingBytes = random.nextInt(10);
                        byte[] alreadyExistingBytes = new byte[this.numOfExistingBytes];
                        random.nextBytes(alreadyExistingBytes);
                        fileOutputStream.write(alreadyExistingBytes);
                    }
                }
                this.fc = openNewFileChannel();
                /*
                 * fc.position(this.fc.size()) is used to set the position of the file channel (fc) to the end of the file.
                 * This operation ensures that any subsequent write operations will append data to the existing content
                 * of the file rather than overwrite it.
                 * (we did this also because StandardOpenOption.READ and .APPEND is not allowed together)
                 */
                this.fc.position(this.fc.size());
                this.data = new byte[this.srcSize];
                if(this.stateOfSrc != STATE_OF_OBJ.EMPTY){
                    random.nextBytes(this.data);
                }else{
                    Arrays.fill(data, (byte) 0);
                }
            } else if (this.stateOfFc == STATE_OF_OBJ.NULL) {
                this.fc = null;
            } else if (this.stateOfFc == STATE_OF_OBJ.INVALID) {
                this.fc = getInvalidFcInstance();
            }
            assignSrc();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assignSrc(){
        this.src = Unpooled.directBuffer(this.srcSize);
        if(this.stateOfSrc == STATE_OF_OBJ.NOT_EMPTY) {
            this.src.writeBytes(this.data);
        } else if (this.stateOfSrc == STATE_OF_OBJ.NULL) {
            this.src = null;
        } else if (this.stateOfSrc == STATE_OF_OBJ.INVALID) {
            this.src = getMockedInvalidSrcInstance();
        }
    }

    private FileChannel getInvalidFcInstance() {
        FileChannel invalidFc;
        try {
            invalidFc = openNewFileChannel();
            invalidFc.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return  invalidFc;
    }

    private static FileChannel openNewFileChannel() throws IOException {
        return FileChannel.open(Paths.get("/Users/matteocalzetta/Documents/BufChanWriteTest/writeToThisFile.log"), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    }

    private ByteBuf getMockedInvalidSrcInstance() {
        ByteBuf invalidByteBuf = mock(ByteBuf.class);
        when(invalidByteBuf.readableBytes()).thenReturn(1);
        when(invalidByteBuf.readerIndex()).thenReturn(-1);
        return invalidByteBuf;
    }

    @After
    public void cleanupEachTime(){
        try {
            if(this.stateOfFc != STATE_OF_OBJ.NULL) {
                this.fc.close();
            }
            File oldLogFile = new File("/Users/matteocalzetta/Documents/BufChanWriteTest/writeToThisFile.log");
            if(oldLogFile.exists()){
                oldLogFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void cleanupOnce(){
        File newLogFileDirs = new File("/Users/matteocalzetta/Documents/BufChanWriteTest");
        deleteDirectoryRecursive(newLogFileDirs);
        File parentDirectory = new File("/Users/matteocalzetta/Documents");
        parentDirectory.delete();
    }

    private static void deleteDirectoryRecursive(File directories) {
        if (directories.exists()) {
            File[] files = directories.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryRecursive(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directories.delete();
        }
    }

    @Test(timeout = 4000)
    public void write() throws IOException {

        BufferedChannel bufferedChannel = new BufferedChannel(this.allocator, this.fc, this.capacity, this.unpersistedBytesBound);
        bufferedChannel.write(this.src);

        int expectedNumOfBytesInWriteBuff = 0;
        if(this.stateOfSrc != STATE_OF_OBJ.EMPTY && capacity!=0) {
            expectedNumOfBytesInWriteBuff = (this.srcSize < this.capacity) ? this.srcSize : this.srcSize % this.capacity;
        }
        int expectedNumOfBytesInFc = 0;
        if(this.unpersistedBytesBound > 0L){
            if(this.unpersistedBytesBound <= this.srcSize){
                expectedNumOfBytesInFc = this.srcSize;
                expectedNumOfBytesInWriteBuff = 0;
            }
        }else{
            expectedNumOfBytesInFc = (this.srcSize < this.capacity) ? 0 : this.srcSize - expectedNumOfBytesInWriteBuff ;
        }
        byte[] actualBytesInWriteBuff = new byte[expectedNumOfBytesInWriteBuff];
        bufferedChannel.writeBuffer.getBytes(0, actualBytesInWriteBuff);

        //We only take expectedNumOfBytesInWriteBuff bytes from this.data because the rest would have been flushed onto the fc
        byte[] expectedBytesInWriteBuff = Arrays.copyOfRange(this.data, this.data.length - expectedNumOfBytesInWriteBuff, this.data.length);
        Assert.assertEquals("BytesInWriteBuff Check Failed", Arrays.toString(actualBytesInWriteBuff), Arrays.toString(expectedBytesInWriteBuff));

        ByteBuffer actualBytesInFc = ByteBuffer.allocate(expectedNumOfBytesInFc);
        this.fc.position(this.numOfExistingBytes);
        this.fc.read(actualBytesInFc);
        //We take everything that has supposedly been flushed onto the fc
        byte[] expectedBytesInFc = Arrays.copyOfRange(this.data, 0, expectedNumOfBytesInFc);
        Assert.assertEquals("BytesInFc Check Failed", Arrays.toString(actualBytesInFc.array()), Arrays.toString(expectedBytesInFc));
        if(this.stateOfSrc == STATE_OF_OBJ.EMPTY){
            Assert.assertEquals("BufferedChannelPosition Check Failed", this.numOfExistingBytes, bufferedChannel.position());

        }else {
            Assert.assertEquals("BufferedChannelPosition Check Failed", this.numOfExistingBytes + this.srcSize, bufferedChannel.position());
        }
    }
}



