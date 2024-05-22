package org.apache.bookkeeper.bookie;


import io.netty.buffer.ByteBufAllocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;


public class BufferedChannelTest {


    private FileChannel fileChannel;
    private BufferedChannel buffChannel;
    private File file;

    @Before
    public void setUp() throws IOException {
        file = Files.createTempFile("test", ".tmp").toFile();
        fileChannel = FileChannel.open(file.toPath());
        buffChannel = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannel, 1024);
    }

    @After
    public void tearDown() throws IOException {
        buffChannel.close();
        fileChannel.close();
        Files.delete(file.toPath());
    }

    @Test
    public void testPosition(){
        long a = buffChannel.position();
        assertEquals(0, a);

    }

}