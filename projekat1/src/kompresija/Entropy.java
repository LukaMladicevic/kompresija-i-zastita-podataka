package kompresija;

import java.util.Arrays;

public final class Entropy {

    private Entropy() {
    }

    public static long[] histogram(byte[] data) {
        long[] counts = new long[256];
        for (byte b : data) {
            counts[Byte.toUnsignedInt(b)]++;
        }
        return counts;
    }

    public static double entropy(long[] counts, long n) {
        if (n == 0) {
            return 0.0;
        }

        double h = 0.0;
        for (long count : counts) {
            if (count == 0) {
                continue;
            }
            double p = (double) count / n;
            h -= p * log2(p);
        }
        return h;
    }

    public static int distinct(long[] counts) {
        return (int) Arrays.stream(counts)
                .filter(count -> count > 0)
                .count();
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
