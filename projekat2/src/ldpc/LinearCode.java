package ldpc;

import java.util.ArrayList;
import java.util.List;

public final class LinearCode {

    private LinearCode() {
    }

    public static int syndrome(int[] h, int vector) {
        int result = 0;

        for (int row = 0; row < h.length; row++) {
            if (Bits.parity(h[row] & vector) == 1) {
                result = Bits.set(result, row);
            }
        }
        return result;
    }

    public static int rank(int[] h, int n) {
        int[] rows = h.clone();
        int pivotRow = 0;

        for (int column = 0; column < n && pivotRow < rows.length; column++) {
            int pivot = findPivot(rows, pivotRow, column);

            if (pivot == -1) {
                continue;
            }

            swap(rows, pivotRow, pivot);

            for (int row = 0; row < rows.length; row++) {
                if (row != pivotRow && Bits.get(rows[row], column) == 1) {
                    rows[row] = rows[row] ^ rows[pivotRow];
                }
            }
            pivotRow++;
        }
        return pivotRow;
    }

    public static int[] codewords(int[] h, int n) {
        List<Integer> found = new ArrayList<>();

        for (int candidate = 0; candidate < Bits.twoTo(n); candidate++) {
            if (syndrome(h, candidate) == 0) {
                found.add(candidate);
            }
        }
        return found.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int lightestCodeword(int[] codewords) {
        int lightest = -1;
        int lightestWeight = Integer.MAX_VALUE;

        for (int codeword : codewords) {
            if (codeword == 0) {
                continue;
            }

            int weight = Bits.weight(codeword);

            if (weight < lightestWeight) {
                lightest = codeword;
                lightestWeight = weight;
            }
        }
        return lightest;
    }

    public static int groupSum(int[] h, int group, int rowsPerGroup) {
        int sum = 0;

        for (int rowInGroup = 0; rowInGroup < rowsPerGroup; rowInGroup++) {
            sum = sum ^ h[group * rowsPerGroup + rowInGroup];
        }
        return sum;
    }

    private static int findPivot(int[] rows, int fromRow, int column) {
        for (int row = fromRow; row < rows.length; row++) {
            if (Bits.get(rows[row], column) == 1) {
                return row;
            }
        }
        return -1;
    }

    private static void swap(int[] rows, int first, int second) {
        int temp = rows[first];
        rows[first] = rows[second];
        rows[second] = temp;
    }
}
