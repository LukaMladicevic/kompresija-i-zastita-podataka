package kompresija;

import java.io.IOException;

public final class LZ77 {

    private static final int WINDOW_SIZE = 32768;
    private static final int MIN_MATCH = 3;
    private static final int MAX_LENGTH = 10;

    private static final int DISTANCE_BITS = bitsNeeded(WINDOW_SIZE - 1);
    private static final int LENGTH_BITS = bitsNeeded(MAX_LENGTH - MIN_MATCH);

    public static final int PARAMETER_BYTES = 3;

    private LZ77() {
    }

    public static void encode(BitWriter w, byte[] data) throws IOException {
        w.writeBits(DISTANCE_BITS, 8);
        w.writeBits(MAX_LENGTH, 8);
        w.writeBits(MIN_MATCH, 8);

        int position = 0;
        while (position < data.length) {
            int bestLength = 0;
            int bestDistance = 0;

            int windowStart = Math.max(0, position - WINDOW_SIZE);
            for (int start = windowStart; start < position; start++) {
                int length = matchLength(data, start, position);
                if (length > bestLength) {
                    bestLength = length;
                    bestDistance = position - start;
                }
            }

            if (bestLength >= MIN_MATCH) {
                w.writeBit(1);
                w.writeBits(bestDistance - 1, DISTANCE_BITS);
                w.writeBits(bestLength - MIN_MATCH, LENGTH_BITS);
                position = position + bestLength;
            } else {
                w.writeBit(0);
                w.writeBits(Byte.toUnsignedInt(data[position]), 8);
                position = position + 1;
            }
        }
    }

    public static byte[] decode(BitReader r, long n) throws IOException {
        int distanceBits = r.readBits(8);
        int maxLength = r.readBits(8);
        int minMatch = r.readBits(8);
        int lengthBits = bitsNeeded(maxLength - minMatch);

        byte[] out = new byte[(int) n];
        int position = 0;

        while (position < n) {
            int flag = r.readBit();

            if (flag == 0) {
                out[position] = (byte) r.readBits(8);
                position = position + 1;
            } else {
                int distance = r.readBits(distanceBits) + 1;
                int length = r.readBits(lengthBits) + minMatch;
                int start = position - distance;

                for (int i = 0; i < length; i++) {
                    out[position] = out[start + i];
                    position = position + 1;
                }
            }
        }

        return out;
    }

    private static int matchLength(byte[] data, int start, int position) {
        int length = 0;

        while (position + length < data.length
                && length < MAX_LENGTH
                && data[start + length] == data[position + length]) {
            length++;
        }
        return length;
    }

    private static int bitsNeeded(int maxValue) {
        int bits = 1;
        int capacity = 2;

        while (capacity <= maxValue) {
            capacity = capacity * 2;
            bits++;
        }
        return bits;
    }
}
