package kompresija;

import java.util.Arrays;

public final class ShannonFano {

    private ShannonFano() {
    }

    public static int[] lengths(long[] counts) {
        int[] lengths = new int[256];

        int distinct = Entropy.distinct(counts);

        if (distinct == 0) {
            return lengths;
        }

        if (distinct == 1) {
            for (int sym = 0; sym < 256; sym++) {
                if (counts[sym] > 0) {
                    lengths[sym] = 1;
                }
            }
            return lengths;
        }

        Integer[] order = new Integer[distinct];
        int k = 0;
        for (int sym = 0; sym < 256; sym++) {
            if (counts[sym] > 0) {
                order[k++] = sym;
            }
        }
        Arrays.sort(order, (a, b) -> Long.compare(counts[b], counts[a]));

        int[] symbols = new int[distinct];
        for (int i = 0; i < distinct; i++) {
            symbols[i] = order[i];
        }

        split(symbols, counts, 0, distinct - 1, lengths);
        return lengths;
    }

    private static void split(int[] symbols, long[] counts, int lo, int hi, int[] lengths) {
        if (lo >= hi) {
            return;
        }

        long total = 0;
        for (int i = lo; i <= hi; i++) {
            total += counts[symbols[i]];
        }

        int mid = lo;
        long best = Long.MAX_VALUE;
        long left = 0;

        for (int i = lo; i < hi; i++) {
            left = left + counts[symbols[i]];
            long right = total - left;
            long diff = Math.abs(left - right);

            if (diff < best) {
                best = diff;
                mid = i;
            }
        }

        for (int i = lo; i <= hi; i++) {
            lengths[symbols[i]]++;
        }

        split(symbols, counts, lo, mid, lengths);
        split(symbols, counts, mid + 1, hi, lengths);
    }
}
