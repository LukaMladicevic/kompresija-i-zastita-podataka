package ldpc;

public final class GallagerB {

    public static final int NOT_DECODED = -1;

    private GallagerB() {
    }

    public static int decode(long[] h, int n, int received, int maxIterations,
                             double thresholdZero, double thresholdOne) {
        int current = received;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (LinearCode.syndrome(h, current) == 0) {
                return current;
            }
            current = oneIteration(h, n, received, current, thresholdZero, thresholdOne);
        }

        if (LinearCode.syndrome(h, current) == 0) {
            return current;
        }
        return NOT_DECODED;
    }

    public static int oneIteration(long[] h, int n, int received, int current,
                                   double thresholdZero, double thresholdOne) {
        int next = 0;

        for (int bit = 0; bit < n; bit++) {
            int votesForZero = 0;
            int votesForOne = 0;

            for (int row = 0; row < h.length; row++) {
                if (ParityCheckMatrix.bit(h[row], bit) == 0) {
                    continue;
                }

                if (messageToBit(h[row], current, bit) == 1) {
                    votesForOne++;
                } else {
                    votesForZero++;
                }
            }

            int channelValue = ParityCheckMatrix.bit(received, bit);
            int value = decide(votesForZero, votesForOne, channelValue, thresholdZero, thresholdOne);

            if (value == 1) {
                next = ParityCheckMatrix.setBit(next, bit);
            }
        }
        return next;
    }

    private static int messageToBit(long row, int current, int bit) {
        int rowParity = LinearCode.parity(row & current);
        return rowParity ^ ParityCheckMatrix.bit(current, bit);
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
