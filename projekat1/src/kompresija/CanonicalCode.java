package kompresija;

import java.io.IOException;

public final class CanonicalCode {

    public static final int LENGTH_BITS = 6;
    public static final int TABLE_BYTES = 256 * LENGTH_BITS / 8;

    private CanonicalCode() {
    }

    public static int maxLength(int[] lengths) {
        int max = 0;
        for (int len : lengths) {
            if (len > max) {
                max = len;
            }
        }
        return max;
    }

    public static int[] buildCodes(int[] lengths) {
        int[] codes = new int[256];
        int maxLen = maxLength(lengths);

        int code = 0;
        for (int len = 1; len <= maxLen; len++) {
            for (int sym = 0; sym < 256; sym++) {
                if (lengths[sym] == len) {
                    codes[sym] = code++;
                }
            }
            code <<= 1;
        }
        return codes;
    }

    public static void writeLengths(BitWriter w, int[] lengths) throws IOException {
        for (int len : lengths) {
            w.writeBits(len, LENGTH_BITS);
        }
    }

    public static int[] readLengths(BitReader r) throws IOException {
        int[] lengths = new int[256];
        for (int i = 0; i < 256; i++) {
            lengths[i] = r.readBits(LENGTH_BITS);
        }
        return lengths;
    }

    public static void encode(BitWriter w, byte[] data, int[] lengths, int[] codes) throws IOException {
        for (byte b : data) {
            int sym = b & 0xFF;
            w.writeBits(codes[sym], lengths[sym]);
        }
    }

    public static byte[] decode(BitReader r, int[] lengths, long n) throws IOException {
        byte[] out = new byte[(int) n];
        if (n == 0) {
            return out;
        }

        int maxLen = maxLength(lengths);

        int[] symbolsPerLength = new int[maxLen + 1];
        for (int len : lengths) {
            if (len > 0) {
                symbolsPerLength[len]++;
            }
        }

        int[] firstCode = new int[maxLen + 1];
        int[] firstIndex = new int[maxLen + 1];
        int code = 0;
        int index = 0;
        for (int len = 1; len <= maxLen; len++) {
            firstCode[len] = code;
            firstIndex[len] = index;
            index += symbolsPerLength[len];
            code = (code + symbolsPerLength[len]) << 1;
        }

        int[] sorted = new int[index];
        int k = 0;
        for (int len = 1; len <= maxLen; len++) {
            for (int sym = 0; sym < 256; sym++) {
                if (lengths[sym] == len) {
                    sorted[k++] = sym;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            int current = 0;
            int symbol = -1;

            for (int len = 1; len <= maxLen; len++) {
                current = (current << 1) | r.readBit();
                int offset = current - firstCode[len];

                if (offset >= 0 && offset < symbolsPerLength[len]) {
                    symbol = sorted[firstIndex[len] + offset];
                    break;
                }
            }

            if (symbol < 0) {
                throw new IOException("Invalid code word, pos: " + i);
            }
            out[i] = (byte) symbol;
        }

        return out;
    }
}
