package ldpc;

public final class GallagerB {

    public static final int NOT_DECODED = -1;

    private GallagerB() {
    }

    public static int decode(int[] h, int n, int received, int maxIterations,
                             double thresholdZero, double thresholdOne) {
        int current = received;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (isCodeword(h, current)) {
                return current;
            }
            current = oneIteration(h, n, received, current, thresholdZero, thresholdOne);
        }
        return isCodeword(h, current) ? current : NOT_DECODED;
    }

    public static int oneIteration(int[] h, int n, int received, int current,
                                   double thresholdZero, double thresholdOne) {
        int next = 0;

        for (int bit = 0; bit < n; bit++) {
            int votesForZero = 0;
            int votesForOne = 0;

            for (int row : h) {
                if (Bits.get(row, bit) == 0) {
                    continue;
                }

                if (checkOpinion(row, current, bit) == 1) {
                    votesForOne++;
                } else {
                    votesForZero++;
                }
            }

            int channelValue = Bits.get(received, bit);
            int value = decide(votesForZero, votesForOne, channelValue, thresholdZero, thresholdOne);

            if (value == 1) {
                next = Bits.set(next, bit);
            }
        }
        return next;
    }

    private static boolean isCodeword(int[] h, int vector) {
        return LinearCode.syndrome(h, vector) == 0;
    }

    private static int checkOpinion(int row, int current, int bit) {
        return Bits.parity(row & current) ^ Bits.get(current, bit);
    }

    private static int decide(int votesForZero, int votesForOne, int channelValue,
                              double thresholdZero, double thresholdOne) {
        int degree = votesForZero + votesForOne;

        if (votesForZero >= thresholdZero * degree) {
            return 0;
        }

        if (votesForOne >= thresholdOne * degree) {
            return 1;
        }
        return channelValue;
    }
}
