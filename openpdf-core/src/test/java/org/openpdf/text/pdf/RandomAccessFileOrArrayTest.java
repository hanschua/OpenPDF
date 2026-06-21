package org.openpdf.text.pdf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileOrArrayTest {

    @Test
    void getFilePointer_fixIntegerOverflow() throws IOException {
        try (RandomAccessFileOrArray rf = new RandomAccessFileOrArray(
                getClass().getResource("/EmptyPage.pdf").getPath(), false, true) {

            @Override
            public void reOpen() throws IOException {
                trf = new RandomAccessFile(filename, "r") {

                    @Override
                    public long getFilePointer() throws IOException {
                        return Long.MAX_VALUE;
                    }

                };
                seek(0);
            }

        }) {
            Assertions.assertEquals(0L, rf.getFilePointer());
            rf.seek(Long.MAX_VALUE);
            Assertions.assertEquals(Long.MAX_VALUE, rf.getFilePointer());
        }
    }

}
