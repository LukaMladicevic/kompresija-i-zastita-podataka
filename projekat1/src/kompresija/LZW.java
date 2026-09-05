package kompresija;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class LZW {

    private static final int START_WIDTH = 9;
    private static final int MAX_WIDTH = 16;
    private static final int FIRST_CODE = 256;
    private static final int FREEZE_WHEN_FULL = 0;

    public static final int PARAMETER_BYTES = 3;

    private LZW() {
    }

    public static void encode(BitWriter w, byte[] data) throws IOException {
        w.writeBits(START_WIDTH, 8);
        w.writeBits(MAX_WIDTH, 8);
        w.writeBits(FREEZE_WHEN_FULL, 8);

        if (data.length == 0) {
            return;
        }

        int maxEntries = capacityFor(MAX_WIDTH);
        Map<Integer, Integer> dictionary = new HashMap<>();
        int next = FIRST_CODE;
        int width = START_WIDTH;
        int capacity = capacityFor(START_WIDTH);

        int current = Byte.toUnsignedInt(data[0]);

        for (int i = 1; i < data.length; i++) {
            int b = Byte.toUnsignedInt(data[i]);
            int key = current * 256 + b;
            Integer longer = dictionary.get(key);

            if (longer != null) {
                current = longer;
                continue;
            }

            w.writeBits(current, width);

            if (next < maxEntries) {
                dictionary.put(key, next);
                next++;

                if (next > capacity && width < MAX_WIDTH) {
                    width++;
                    capacity = capacity * 2;
                }
            }

            current = b;
        }

        w.writeBits(current, width);
    }

    public static byte[] decode(BitReader r, long n) throws IOException {
        int startWidth = r.readBits(8);
        int maxWidth = r.readBits(8);
        r.readBits(8);

        byte[] out = new byte[(int) n];
        if (n == 0) {
            return out;
        }

        int maxEntries = capacityFor(maxWidth);
        int[] prefix = new int[maxEntries];
        int[] suffix = new int[maxEntries];
        int[] stack = new int[maxEntries];

        for (int code = 0; code < FIRST_CODE; code++) {
            prefix[code] = -1;
            suffix[code] = code;
        }

        int next = FIRST_CODE;
        int width = startWidth;
        int capacity = capacityFor(startWidth);

        int previousCode = r.readBits(width);
        int position = expand(out, 0, previousCode, prefix, suffix, stack);

        while (position < n) {
            if (next < maxEntries && next + 1 > capacity && width < maxWidth) {
                width++;
                capacity = capacity * 2;
            }

            int code = r.readBits(width);
            int first;

            if (code < next) {
                first = firstByte(code, prefix, suffix);
                position = expand(out, position, code, prefix, suffix, stack);
            } else {
                first = firstByte(previousCode, prefix, suffix);
                position = expand(out, position, previousCode, prefix, suffix, stack);
                out[position] = (byte) first;
                position = position + 1;
            }

            if (next < maxEntries) {
                prefix[next] = previousCode;
                suffix[next] = first;
                next++;
            }

            previousCode = code;
        }

        return out;
    }

    private static int expand(byte[] out, int position, int code, int[] prefix, int[] suffix, int[] stack) {
        int top = 0;

        while (code != -1) {
            stack[top] = suffix[code];
            top++;
            code = prefix[code];
        }

        while (top > 0) {
            top--;
            out[position] = (byte) stack[top];
            position = position + 1;
        }
        return position;
    }

    private static int firstByte(int code, int[] prefix, int[] suffix) {
        while (prefix[code] != -1) {
            code = prefix[code];
        }
        return suffix[code];
    }

    private static int capacityFor(int width) {
        int capacity = 1;

        for (int i = 0; i < width; i++) {
            capacity = capacity * 2;
        }
        return capacity;
    }
}
