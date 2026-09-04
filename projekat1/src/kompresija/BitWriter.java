package kompresija;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class BitWriter implements Closeable {

    private final OutputStream out;
    private int buffer;
    private int bitCount;

    public BitWriter(OutputStream out) {
        this.out = out;
    }

    public void writeBit(int bit) throws IOException {
        buffer = (buffer << 1) | (bit & 1);
        bitCount++;

        if (bitCount == 8) {
            out.write(buffer);
            buffer = 0;
            bitCount = 0;
        }
    }

    public void writeBits(int value, int count) throws IOException {
        for (int i = count - 1; i >= 0; i--) {
            writeBit((value >>> i) & 1);
        }
    }

    @Override
    public void close() throws IOException {
        if (bitCount > 0) {
            buffer <<= (8 - bitCount);
            out.write(buffer);
            buffer = 0;
            bitCount = 0;
        }
        out.close();
    }
}
