package ldpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ParityCheckMatrix {

    private ParityCheckMatrix() {
    }

    public static int[] build(int n, int rows, int rowWeight, int columnWeight, long seed) {
        int groups = columnWeight;
        int rowsPerGroup = rows / groups;

        Random random = new Random(seed);
        int[] h = new int[rows];

        for (int group = 0; group < groups; group++) {
            List<Integer> columns = group == 0 ? naturalOrder(n) : shuffledOrder(n, random);

            for (int rowInGroup = 0; rowInGroup < rowsPerGroup; rowInGroup++) {
                int row = 0;

                for (int i = 0; i < rowWeight; i++) {
                    row = Bits.set(row, columns.get(rowInGroup * rowWeight + i));
                }
                h[group * rowsPerGroup + rowInGroup] = row;
            }
        }
        return h;
    }

    public static int checksOfColumn(int[] h, int column) {
        int mask = 0;

        for (int row = 0; row < h.length; row++) {
            if (Bits.get(h[row], column) == 1) {
                mask = Bits.set(mask, row);
            }
        }
        return mask;
    }

    public static int sharedChecks(int[] h, int firstColumn, int secondColumn) {
        return Bits.weight(checksOfColumn(h, firstColumn) & checksOfColumn(h, secondColumn));
    }

    public static int columnWeight(int[] h, int column) {
        return Bits.weight(checksOfColumn(h, column));
    }

    private static List<Integer> naturalOrder(int n) {
        List<Integer> columns = new ArrayList<>();

        for (int column = 0; column < n; column++) {
            columns.add(column);
        }
        return columns;
    }

    private static List<Integer> shuffledOrder(int n, Random random) {
        List<Integer> columns = naturalOrder(n);
        Collections.shuffle(columns, random);
        return columns;
    }
}
