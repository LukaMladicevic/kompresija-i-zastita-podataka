package kompresija;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BitReader implements Closeable {

    private final InputStream in;
    private int buffer;
    private int bitCount;

    public BitReader(InputStream in) {
        this.in = in;
    }

    public int readBit() throws IOException {
        if (bitCount == 0) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException("EOF");
            }
            buffer = b;
            bitCount = 8;
        }

        bitCount--;
        return (buffer >>> bitCount) & 1;
    }

    public int readBits(int count) throws IOException {
        int value = 0;
        for (int i = 0; i < count; i++) {
            value = (value << 1) | readBit();
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
