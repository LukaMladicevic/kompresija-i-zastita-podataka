package kompresija;

public final class Entropy {

    private static final double LOG2 = Math.log(2);

    private Entropy() {
    }

    public static long[] histogram(byte[] data) {
        long[] counts = new long[256];
        for (byte b : data) {
            counts[b & 0xFF]++;
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
            h -= p * (Math.log(p) / LOG2);
        }
        return h;
    }

    public static int distinct(long[] counts) {
        int d = 0;
        for (long c : counts) {
            if (c > 0) {
                d++;
            }
        }
        return d;
    }
}
