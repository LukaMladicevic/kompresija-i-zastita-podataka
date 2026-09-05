package ldpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ParityCheckMatrix {

    private ParityCheckMatrix() {
    }

    public static long[] build(int n, int rows, int rowWeight, int columnWeight, long seed) {
        int groups = columnWeight;
        int rowsPerGroup = rows / groups;

        Random random = new Random(seed);
        long[] h = new long[rows];

        for (int group = 0; group < groups; group++) {
            List<Integer> columns = columnOrder(n, group, random);

            for (int rowInGroup = 0; rowInGroup < rowsPerGroup; rowInGroup++) {
                long row = 0;

                for (int i = 0; i < rowWeight; i++) {
                    int column = columns.get(rowInGroup * rowWeight + i);
                    row = setBit(row, column);
                }
                h[group * rowsPerGroup + rowInGroup] = row;
            }
        }
        return h;
    }

    private static List<Integer> columnOrder(int n, int group, Random random) {
        List<Integer> columns = new ArrayList<>();

        for (int column = 0; column < n; column++) {
            columns.add(column);
        }

        if (group > 0) {
            Collections.shuffle(columns, random);
        }
        return columns;
    }

    public static long setBit(long row, int column) {
        return row | (1L << column);
    }

    public static int bit(long row, int column) {
        return (int) ((row >>> column) & 1);
    }

    public static int rowWeight(long row) {
        return Long.bitCount(row);
    }

    public static int columnWeight(long[] h, int column) {
        int weight = 0;

        for (long row : h) {
            weight = weight + bit(row, column);
        }
        return weight;
    }
}
