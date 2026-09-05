package kompresija;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

public final class CanonicalCode {

    public static final int LENGTH_BITS = 6;
    public static final int TABLE_BYTES = 256 * LENGTH_BITS / 8;

    private CanonicalCode() {
    }

    public static int maxLength(int[] lengths) {
        return Arrays.stream(lengths).max().orElse(0);
    }

    public static int[] buildCodes(int[] lengths) {
        int[] codes = new int[256];
        int maxLen = maxLength(lengths);

        int code = 0;
        for (int len = 1; len <= maxLen; len++) {
            for (int sym = 0; sym < 256; sym++) {
                if (lengths[sym] == len) {
                    codes[sym] = code;
                    code++;
                }
            }
            code = code * 2;
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
            int sym = Byte.toUnsignedInt(b);
            w.writeBits(codes[sym], lengths[sym]);
        }
    }

    public static byte[] decode(BitReader r, int[] lengths, long n) throws IOException {
        byte[] out = new byte[(int) n];
        if (n == 0) {
            return out;
        }

        int maxLen = maxLength(lengths);
        int[] symbolsPerLength = countSymbolsPerLength(lengths, maxLen);
        int[] firstCode = firstCodePerLength(symbolsPerLength, maxLen);
        int[] firstIndex = firstIndexPerLength(symbolsPerLength, maxLen);
        int[] sorted = symbolsInCanonicalOrder(lengths, maxLen);

        for (int i = 0; i < n; i++) {
            int current = 0;
            int symbol = -1;

            for (int len = 1; len <= maxLen; len++) {
                current = current * 2 + r.readBit();

                int first = firstCode[len];
                int last = first + symbolsPerLength[len] - 1;

                if (current >= first && current <= last) {
                    int offset = current - first;
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

    private static int[] countSymbolsPerLength(int[] lengths, int maxLen) {
        int[] result = new int[maxLen + 1];
        for (int len : lengths) {
            if (len > 0) {
                result[len]++;
            }
        }
        return result;
    }

    private static int[] firstCodePerLength(int[] symbolsPerLength, int maxLen) {
        int[] result = new int[maxLen + 1];

        int code = 0;
        for (int len = 1; len <= maxLen; len++) {
            result[len] = code;
            code = code + symbolsPerLength[len];
            code = code * 2;
        }
        return result;
    }

    private static int[] firstIndexPerLength(int[] symbolsPerLength, int maxLen) {
        int[] result = new int[maxLen + 1];

        int index = 0;
        for (int len = 1; len <= maxLen; len++) {
            result[len] = index;
            index = index + symbolsPerLength[len];
        }
        return result;
    }

    private static int[] symbolsInCanonicalOrder(int[] lengths, int maxLen) {
        return IntStream.rangeClosed(1, maxLen)
                .flatMap(len -> IntStream.range(0, 256).filter(sym -> lengths[sym] == len))
                .toArray();
    }
}
