package kompresija;

import java.io.IOException;

public final class Header {

    public static final int SHANNON_FANO = 0;
    public static final int HUFFMAN = 1;
    public static final int LZ77 = 2;
    public static final int LZW = 3;

    public static final int SIZE_IN_BYTES = 11;

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

        for (int shift = 56; shift >= 0; shift -= 8) {
            w.writeBits((int) ((n >>> shift) & 0xFF), 8);
        }
    }

    public static Header read(BitReader r) throws IOException {
        int first = r.readBits(8);
        int second = r.readBits(8);

        if (first != MAGIC_FIRST || second != MAGIC_SECOND) {
            throw new IOException("Ulazni fajl nije KZ format");
        }

        int method = r.readBits(8);

        long n = 0;
        for (int i = 0; i < 8; i++) {
            n = (n << 8) | r.readBits(8);
        }

        return new Header(method, n);
    }
}
