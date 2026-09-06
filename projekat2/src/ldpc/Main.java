package ldpc;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int INDEX = 69;

    private static final int N = 15;
    private static final int ROWS = 9;
    private static final int ROW_WEIGHT = 5;
    private static final int COLUMN_WEIGHT = 3;

    private static final double THRESHOLD_ZERO = 0.5;
    private static final double THRESHOLD_ONE = 0.5;
    private static final int MAX_ITERATIONS = 50;
    private static final int SHOWN_ITERATIONS = 4;

    private static final int ALL_VECTORS = Bits.twoTo(N);

    private record GallagerScan(int lightestFailure, int[] failedByWeight, int[] totalByWeight,
                                boolean alwaysReturnsCodeword) {
    }

    public static void main(String[] args) {
        System.out.println("seed: " + INDEX);
        System.out.println("n = " + N + ", n-k = " + ROWS
                + ", w_r = " + ROW_WEIGHT + ", w_c = " + COLUMN_WEIGHT + "\n");

        int[] h = ParityCheckMatrix.build(N, ROWS, ROW_WEIGHT, COLUMN_WEIGHT, INDEX);
        int[] codewords = LinearCode.codewords(h, N);
        GallagerScan scan = scanAllErrors(h);

        printMatrix(h);
        printWeightCheck(h);
        printRank(h, codewords);
        printDistance(h, codewords);
        printSyndromeTable(h, codewords);
        printGallagerB(h, codewords, scan);
        printFailingError(h, codewords, scan);
    }

    private static GallagerScan scanAllErrors(int[] h) {
        int[] failed = new int[N + 1];
        int[] total = new int[N + 1];
        int lightestFailure = -1;
        int lightestWeight = Integer.MAX_VALUE;
        boolean alwaysCodeword = true;

        for (int error = 0; error < ALL_VECTORS; error++) {
            int weight = Bits.weight(error);
            total[weight]++;

            int result = decodeGallager(h, error);

            if (result != GallagerB.NOT_DECODED && LinearCode.syndrome(h, result) != 0) {
                alwaysCodeword = false;
            }

            if (result != 0) {
                failed[weight]++;

                if (weight < lightestWeight) {
                    lightestFailure = error;
                    lightestWeight = weight;
                }
            }
        }
        return new GallagerScan(lightestFailure, failed, total, alwaysCodeword);
    }

    private static void printGallagerB(int[] h, int[] codewords, GallagerScan scan) {
        System.out.println("\nGALLAGER B");
        System.out.println("th_0 = " + THRESHOLD_ZERO + ", th_1 = " + THRESHOLD_ONE
                + ", maxIter = " + MAX_ITERATIONS + "   (prag: 2 od 3)");

        trace(h, Bits.set(0, 0));
        trace(h, firstCorrectableError(h));

        boolean codewordsOk = true;
        for (int codeword : codewords) {
            if (decodeGallager(h, codeword) != codeword) {
                codewordsOk = false;
            }
        }

        System.out.println("\nprovera: "
                + (codewordsOk && scan.alwaysReturnsCodeword() ? "OK" : "GRESKA"));
    }

    private static void printFailingError(int[] h, int[] codewords, GallagerScan scan) {
        int failing = scan.lightestFailure();
        int failWeight = Bits.weight(failing);
        int distance = Bits.weight(LinearCode.lightestCodeword(codewords));

        System.out.println("\nNAJLAKSA GRESKA KOJU GALLAGER B NE ISPRAVLJA");
        System.out.println("\nw_fail = " + failWeight);
        System.out.println("e = " + Bits.toText(failing, N) + "   bitovi " + positions(failing));
        System.out.println("ishod: " + (decodeGallager(h, failing) == GallagerB.NOT_DECODED
                ? "ne konvergira" : "konvergira na pogresnu kodnu rec"));

        System.out.println("\nd(C) = " + distance
                + ", pa kod garantuje ispravljanje (d-1)/2 = " + ((distance - 1) / 2) + " gresaka");
        System.out.println("w_fail = " + failWeight
                + ", pa ni Gallager B ne ispravlja sve greske te tezine");

        printFourCycles(h, failing);
        printFailureCounts(scan);

        boolean lighterAllCorrected = true;
        for (int weight = 0; weight < failWeight; weight++) {
            if (scan.failedByWeight()[weight] != 0) {
                lighterAllCorrected = false;
            }
        }
        boolean reallyFails = decodeGallager(h, failing) != 0;

        System.out.println("\nprovera: " + (lighterAllCorrected && reallyFails ? "OK" : "GRESKA"));
    }

    private static int count4Cycles(int[] h) {
        int cycles = 0;

        for (int first = 0; first < N; first++) {
            for (int second = first + 1; second < N; second++) {
                if (ParityCheckMatrix.sharedChecks(h, first, second) >= 2) {
                    cycles++;
                }
            }
        }
        return cycles;
    }

    private static List<Integer> partnersOf(int[] h, int bit) {
        List<Integer> partners = new ArrayList<>();

        for (int other = 0; other < N; other++) {
            if (other != bit && ParityCheckMatrix.sharedChecks(h, bit, other) >= 2) {
                partners.add(other);
            }
        }
        return partners;
    }

    private static int columnsWithoutPartner(int[] h) {
        int count = 0;

        for (int column = 0; column < N; column++) {
            if (partnersOf(h, column).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void printFourCycles(int[] h, int failing) {
        System.out.println("\nparova kolona sa >= 2 zajednicke provere (4-ciklusi): "
                + count4Cycles(h));
        System.out.println("kolona bez takvog partnera: " + columnsWithoutPartner(h));

        for (int bit = 0; bit < N; bit++) {
            if (Bits.get(failing, bit) == 1) {
                System.out.println("partneri bita " + bit + " u 4-ciklusu: "
                        + join(partnersOf(h, bit)));
            }
        }
    }

    private static void printFailureCounts(GallagerScan scan) {
        System.out.println("\nneispravljene greske po tezini:");

        for (int weight = 0; weight <= N; weight++) {
            System.out.println("tezina " + weight + ": " + scan.failedByWeight()[weight]
                    + " od " + scan.totalByWeight()[weight]);
        }
    }

    private static void trace(int[] h, int error) {
        System.out.println("\nposlato:   " + Bits.toText(0, N));
        System.out.println("greska:    " + Bits.toText(error, N)
                + "   tezina " + Bits.weight(error)
                + ", bitovi " + positions(error));

        int current = error;
        for (int iteration = 1; iteration <= SHOWN_ITERATIONS; iteration++) {
            if (LinearCode.syndrome(h, current) == 0) {
                break;
            }

            int next = GallagerB.oneIteration(h, N, error, current, THRESHOLD_ZERO, THRESHOLD_ONE);

            System.out.println("iteracija " + iteration + ": " + Bits.toText(next, N)
                    + "   prevrnuti bitovi: " + positions(current ^ next));
            current = next;
        }

        int result = decodeGallager(h, error);

        if (result == GallagerB.NOT_DECODED) {
            System.out.println("ishod: ne konvergira ni posle " + MAX_ITERATIONS + " iteracija");
        } else if (result == 0) {
            System.out.println("ishod: ispravljeno, vracena poslata rec");
        } else {
            System.out.println("ishod: konvergirao na pogresnu kodnu rec " + Bits.toText(result, N));
        }
    }

    private static int firstCorrectableError(int[] h) {
        for (int error = 1; error < ALL_VECTORS; error++) {
            if (decodeGallager(h, error) == 0) {
                return error;
            }
        }
        return 0;
    }

    private static int decodeGallager(int[] h, int received) {
        return GallagerB.decode(h, N, received, MAX_ITERATIONS, THRESHOLD_ZERO, THRESHOLD_ONE);
    }

    private static void printSyndromeTable(int[] h, int[] codewords) {
        int[] corrector = SyndromeTable.build(h, N, ROWS);

        System.out.println("\ndostiznih sindroma: " + SyndromeTable.reachable(corrector)
                + "   (od " + corrector.length + " mogucih)");

        System.out.println("\nsindrom     korektor          tezina");
        for (int syndrome = 0; syndrome < corrector.length; syndrome++) {
            if (corrector[syndrome] == SyndromeTable.UNREACHABLE) {
                continue;
            }
            System.out.println(Bits.toText(syndrome, ROWS) + "   "
                    + Bits.toText(corrector[syndrome], N)
                    + "   " + Bits.weight(corrector[syndrome]));
        }

        printCorrectorWeights(corrector);

        boolean leadersOk = true;
        for (int correction : corrector) {
            if (correction == SyndromeTable.UNREACHABLE) {
                continue;
            }

            for (int codeword : codewords) {
                int received = codeword ^ correction;

                if (SyndromeTable.decode(h, corrector, received) != codeword) {
                    leadersOk = false;
                }
            }
        }

        boolean closedOk = true;
        for (int received = 0; received < ALL_VECTORS; received++) {
            if (LinearCode.syndrome(h, SyndromeTable.decode(h, corrector, received)) != 0) {
                closedOk = false;
            }
        }

        System.out.println("\nprovera: " + (leadersOk && closedOk ? "OK" : "GRESKA"));
    }

    private static void printCorrectorWeights(int[] corrector) {
        int[] howMany = new int[N + 1];

        for (int entry : corrector) {
            if (entry != SyndromeTable.UNREACHABLE) {
                howMany[Bits.weight(entry)]++;
            }
        }

        System.out.println("\nkorektori po tezini:");
        for (int weight = 0; weight <= N; weight++) {
            if (howMany[weight] > 0) {
                System.out.println("tezina " + weight + ": " + howMany[weight]);
            }
        }
    }

    private static void printRank(int[] h, int[] codewords) {
        int rank = LinearCode.rank(h, N);
        int k = N - rank;

        System.out.println("\nrank(H) = " + rank + "   (od " + ROWS + " redova)");
        System.out.println("k = n - rank = " + k);
        System.out.println("kodnih reci: " + codewords.length + "   (ocekivano 2^" + k + ")");

        int rowsPerGroup = ROWS / COLUMN_WEIGHT;
        int allOnes = ALL_VECTORS - 1;
        boolean groupsOk = true;

        System.out.println();
        for (int group = 0; group < COLUMN_WEIGHT; group++) {
            int sum = LinearCode.groupSum(h, group, rowsPerGroup);
            System.out.println("zbir redova grupe " + (group + 1) + ": " + Bits.toText(sum, N));
            groupsOk = groupsOk && sum == allOnes;
        }
        System.out.println("sve tri grupe daju isti vektor -> " + (COLUMN_WEIGHT - 1)
                + " zavisnosti, rank = " + ROWS + " - " + (COLUMN_WEIGHT - 1));

        boolean countOk = codewords.length == Bits.twoTo(k);

        System.out.println("\nprovera: " + (groupsOk && countOk ? "OK" : "GRESKA"));
    }

    private static void printDistance(int[] h, int[] codewords) {
        int lightest = LinearCode.lightestCodeword(codewords);
        int distance = Bits.weight(lightest);

        System.out.println("\nd(C) = " + distance);
        System.out.println("najlaksa nenulta kodna rec: " + Bits.toText(lightest, N));
        System.out.println("jedinice na pozicijama:     " + positions(lightest));

        int columnSum = 0;
        for (int column = 0; column < N; column++) {
            if (Bits.get(lightest, column) == 1) {
                columnSum = columnSum ^ ParityCheckMatrix.checksOfColumn(h, column);
            }
        }
        System.out.println("XOR tih kolona H:           " + Bits.toText(columnSum, ROWS)
                + "   (nula -> kolone su zavisne)");

        System.out.println("\nidenticne kolone H:");
        int duplicates = 0;
        for (int first = 0; first < N; first++) {
            for (int second = first + 1; second < N; second++) {
                int firstMask = ParityCheckMatrix.checksOfColumn(h, first);

                if (firstMask == ParityCheckMatrix.checksOfColumn(h, second)) {
                    System.out.println("kolona " + first + " = kolona " + second
                            + " = " + Bits.toText(firstMask, ROWS));
                    duplicates++;
                }
            }
        }
        if (duplicates == 0) {
            System.out.println("nema");
        }

        System.out.println("\nispravlja (d-1)/2 = " + ((distance - 1) / 2) + " gresaka");
        System.out.println("detektuje  d-1    = " + (distance - 1) + " gresaka");

        boolean dependent = columnSum == 0;

        boolean noZeroColumn = true;
        for (int column = 0; column < N; column++) {
            if (ParityCheckMatrix.checksOfColumn(h, column) == 0) {
                noZeroColumn = false;
            }
        }

        System.out.println("\nprovera: " + (dependent && noZeroColumn ? "OK" : "GRESKA"));
    }

    private static String positions(int vector) {
        List<Integer> found = new ArrayList<>();

        for (int position = 0; position < N; position++) {
            if (Bits.get(vector, position) == 1) {
                found.add(position);
            }
        }
        return join(found);
    }

    private static String join(List<Integer> values) {
        StringBuilder text = new StringBuilder();

        for (int value : values) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(value);
        }
        return text.toString();
    }

    private static void printMatrix(int[] h) {
        System.out.println("H (" + ROWS + " x " + N + "):\n");

        for (int row = 0; row < ROWS; row++) {
            System.out.printf("red %d   ", row);

            for (int column = 0; column < N; column++) {
                System.out.print(Bits.get(h[row], column) + " ");
            }
            System.out.println();
        }
    }

    private static void printWeightCheck(int[] h) {
        System.out.print("\ntezine redova:    ");
        boolean rowsOk = true;
        for (int row : h) {
            int weight = Bits.weight(row);
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
