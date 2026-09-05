package ldpc;

import java.util.Arrays;

public final class SyndromeTable {

    public static final int UNREACHABLE = -1;

    private SyndromeTable() {
    }

    public static int[] build(long[] h, int n, int rows) {
        int[] corrector = new int[LinearCode.twoTo(rows)];
        Arrays.fill(corrector, UNREACHABLE);

        for (int error = 0; error < LinearCode.twoTo(n); error++) {
            int syndrome = LinearCode.syndrome(h, error);

            if (corrector[syndrome] == UNREACHABLE
                    || LinearCode.weight(error) < LinearCode.weight(corrector[syndrome])) {
                corrector[syndrome] = error;
            }
        }
        return corrector;
    }

    public static int decode(long[] h, int[] corrector, int received) {
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
