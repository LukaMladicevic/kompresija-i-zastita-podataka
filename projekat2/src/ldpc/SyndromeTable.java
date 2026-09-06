package ldpc;

import java.util.Arrays;

public final class SyndromeTable {

    public static final int UNREACHABLE = -1;

    private SyndromeTable() {
    }

    public static int[] build(int[] h, int n, int rows) {
        int[] corrector = new int[Bits.twoTo(rows)];
        Arrays.fill(corrector, UNREACHABLE);

        for (int error = 0; error < Bits.twoTo(n); error++) {
            int syndrome = LinearCode.syndrome(h, error);

            if (corrector[syndrome] == UNREACHABLE
                    || Bits.weight(error) < Bits.weight(corrector[syndrome])) {
                corrector[syndrome] = error;
            }
        }
        return corrector;
    }

    public static int decode(int[] h, int[] corrector, int received) {
        int syndrome = LinearCode.syndrome(h, received);
        return received ^ corrector[syndrome];
    }

    public static int reachable(int[] corrector) {
        int count = 0;

        for (int entry : corrector) {
            if (entry != UNREACHABLE) {
                count++;
            }
        }
        return count;
    }
}
