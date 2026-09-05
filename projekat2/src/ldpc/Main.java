package ldpc;

public class Main {

    private static final int INDEX = 69;

    private static final int N = 15;
    private static final int ROWS = 9;
    private static final int ROW_WEIGHT = 5;
    private static final int COLUMN_WEIGHT = 3;

    public static void main(String[] args) {
        System.out.println("seed: " + INDEX);
        System.out.println("n = " + N + ", n-k = " + ROWS
                + ", w_r = " + ROW_WEIGHT + ", w_c = " + COLUMN_WEIGHT + "\n");

        long[] h = ParityCheckMatrix.build(N, ROWS, ROW_WEIGHT, COLUMN_WEIGHT, INDEX);

        printMatrix(h);
        printWeightCheck(h);
    }

    private static void printMatrix(long[] h) {
        System.out.println("H (" + ROWS + " x " + N + "):\n");

        for (int row = 0; row < ROWS; row++) {
            System.out.printf("red %d   ", row);

            for (int column = 0; column < N; column++) {
                System.out.print(ParityCheckMatrix.bit(h[row], column) + " ");
            }
            System.out.println();
        }
    }

    private static void printWeightCheck(long[] h) {
        System.out.print("\ntezine redova:    ");
        boolean rowsOk = true;
        for (long row : h) {
            int weight = ParityCheckMatrix.rowWeight(row);
            System.out.print(weight + " ");
            rowsOk = rowsOk && weight == ROW_WEIGHT;
        }
        System.out.println("  (trazeno w_r = " + ROW_WEIGHT + ")");

        System.out.print("tezine kolona:    ");
        boolean columnsOk = true;
        int total = 0;
        for (int column = 0; column < N; column++) {
            int weight = ParityCheckMatrix.columnWeight(h, column);
            System.out.print(weight + " ");
            total = total + weight;
            columnsOk = columnsOk && weight == COLUMN_WEIGHT;
        }
        System.out.println("  (trazeno w_c = " + COLUMN_WEIGHT + ")");

        System.out.println("ukupno jedinica:  " + total
                + "   (" + ROWS + "*" + ROW_WEIGHT + " = " + N + "*" + COLUMN_WEIGHT + ")");

        System.out.println("\nprovera: " + (rowsOk && columnsOk ? "OK" : "GRESKA"));
    }
}
