package kompresija;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class Header {

    public static final int SHANNON_FANO = 0;
    public static final int HUFFMAN = 1;
    public static final int LZ77 = 2;
    public static final int LZW = 3;

    private static final int MAGIC_BYTES = 2;
    private static final int METHOD_BYTES = 1;
    private static final int LENGTH_BYTES = 8;

    public static final int SIZE_IN_BYTES = MAGIC_BYTES + METHOD_BYTES + LENGTH_BYTES;

    private static final int MAGIC_FIRST = 'K';
    private static final int MAGIC_SECOND = 'Z';

    public final int method;
    public final long n;

    public Header(int method, long n) {
        this.method = method;
        this.n = n;
    }

    public void write(BitWriter w) throws IOException {
        w.writeBits(MAGIC_FIRST, 8);
        w.writeBits(MAGIC_SECOND, 8);
        w.writeBits(method, 8);

        byte[] lengthBytes = ByteBuffer.allocate(LENGTH_BYTES).putLong(n).array();
        for (byte b : lengthBytes) {
            w.writeBits(Byte.toUnsignedInt(b), 8);
        }
    }

    public static Header read(BitReader r) throws IOException {
        int first = r.readBits(8);
        int second = r.readBits(8);

        if (first != MAGIC_FIRST || second != MAGIC_SECOND) {
            throw new IOException("Ulazni fajl nije KZ format");
        }

        int method = r.readBits(8);

        byte[] lengthBytes = new byte[LENGTH_BYTES];
        for (int i = 0; i < LENGTH_BYTES; i++) {
            lengthBytes[i] = (byte) r.readBits(8);
        }
        long n = ByteBuffer.wrap(lengthBytes).getLong();

        return new Header(method, n);
    }
}
