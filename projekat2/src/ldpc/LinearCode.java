package ldpc;

import java.util.ArrayList;
import java.util.List;

public final class LinearCode {

    private LinearCode() {
    }

    public static int syndrome(long[] h, int vector) {
        int result = 0;

        for (int row = 0; row < h.length; row++) {
            if (parity(h[row] & vector) == 1) {
                result = ParityCheckMatrix.setBit(result, row);
            }
        }
        return result;
    }

    public static int rank(long[] h, int n) {
        long[] rows = h.clone();
        int rank = 0;

        for (int column = 0; column < n; column++) {
            int pivot = findPivot(rows, rank, column);

            if (pivot == -1) {
                continue;
            }

            long temp = rows[rank];
            rows[rank] = rows[pivot];
            rows[pivot] = temp;

            for (int row = 0; row < rows.length; row++) {
                if (row != rank && ParityCheckMatrix.bit(rows[row], column) == 1) {
                    rows[row] = rows[row] ^ rows[rank];
                }
            }
            rank++;
        }
        return rank;
    }

    public static int[] codewords(long[] h, int n) {
        List<Integer> found = new ArrayList<>();

        for (int candidate = 0; candidate < twoTo(n); candidate++) {
            if (syndrome(h, candidate) == 0) {
                found.add(candidate);
            }
        }
        return found.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int lightestCodeword(int[] codewords) {
        int lightest = -1;

        for (int codeword : codewords) {
            if (codeword == 0) {
                continue;
            }

            if (lightest == -1 || weight(codeword) < weight(lightest)) {
                lightest = codeword;
            }
        }
        return lightest;
    }

    public static int weight(int vector) {
        return Integer.bitCount(vector);
    }

    public static int groupSum(long[] h, int group, int rowsPerGroup) {
        int sum = 0;

        for (int rowInGroup = 0; rowInGroup < rowsPerGroup; rowInGroup++) {
            sum = sum ^ (int) h[group * rowsPerGroup + rowInGroup];
        }
        return sum;
    }

    public static int twoTo(int exponent) {
        int value = 1;

        for (int i = 0; i < exponent; i++) {
            value = value * 2;
        }
        return value;
    }

    private static int findPivot(long[] rows, int fromRow, int column) {
        for (int row = fromRow; row < rows.length; row++) {
            if (ParityCheckMatrix.bit(rows[row], column) == 1) {
                return row;
            }
        }
        return -1;
    }

    private static int parity(long value) {
        return Long.bitCount(value) % 2;
    }
}
