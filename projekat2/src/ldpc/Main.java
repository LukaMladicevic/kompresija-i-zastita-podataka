package ldpc;

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

    public static void main(String[] args) {
        System.out.println("seed: " + INDEX);
        System.out.println("n = " + N + ", n-k = " + ROWS
                + ", w_r = " + ROW_WEIGHT + ", w_c = " + COLUMN_WEIGHT + "\n");

        long[] h = ParityCheckMatrix.build(N, ROWS, ROW_WEIGHT, COLUMN_WEIGHT, INDEX);
        int[] codewords = LinearCode.codewords(h, N);

        printMatrix(h);
        printWeightCheck(h);
        printRank(h, codewords);
        printDistance(h, codewords);
        printSyndromeTable(h, codewords);
        printGallagerB(h, codewords);
        printFailingError(h, codewords);
    }

    private static void printGallagerB(long[] h, int[] codewords) {
        System.out.println("\nGALLAGER B");
        System.out.println("th_0 = " + THRESHOLD_ZERO + ", th_1 = " + THRESHOLD_ONE
                + ", maxIter = " + MAX_ITERATIONS + "   (prag: 2 od 3)");

        trace(h, ParityCheckMatrix.setBit(0, 0));
        trace(h, firstCorrectableError(h));

        boolean codewordsOk = true;
        for (int codeword : codewords) {
            if (decodeGallager(h, codeword) != codeword) {
                codewordsOk = false;
            }
        }

        boolean alwaysCodeword = true;
        for (int received = 0; received < LinearCode.twoTo(N); received++) {
            int result = decodeGallager(h, received);

            if (result != GallagerB.NOT_DECODED && LinearCode.syndrome(h, result) != 0) {
                alwaysCodeword = false;
            }
        }

        System.out.println("\nprovera: " + (codewordsOk && alwaysCodeword ? "OK" : "GRESKA"));
    }

    private static void printFailingError(long[] h, int[] codewords) {
        int failing = lightestFailingError(h);
        int failWeight = LinearCode.weight(failing);
        int distance = LinearCode.weight(LinearCode.lightestCodeword(codewords));

        System.out.println("\nNAJLAKSA GRESKA KOJU GALLAGER B NE ISPRAVLJA");
        System.out.println("\nw_fail = " + failWeight);
        System.out.println("e = " + bits(failing, N) + "   bitovi " + positions(failing));
        System.out.println("ishod: " + (decodeGallager(h, failing) == GallagerB.NOT_DECODED
                ? "ne konvergira" : "konvergira na pogresnu kodnu rec"));

        System.out.println("\nd(C) = " + distance
                + ", pa kod garantuje ispravljanje (d-1)/2 = " + ((distance - 1) / 2) + " gresaka");
        System.out.println("w_fail = " + failWeight
                + ", pa ni Gallager B ne ispravlja sve greske te tezine");

        printFourCycles(h, failing);
        printFailureCounts(h);

        boolean lighterAllCorrected = true;
        for (int error = 0; error < LinearCode.twoTo(N); error++) {
            if (LinearCode.weight(error) < failWeight && decodeGallager(h, error) != 0) {
                lighterAllCorrected = false;
            }
        }
        boolean reallyFails = decodeGallager(h, failing) != 0;

        System.out.println("\nprovera: " + (lighterAllCorrected && reallyFails ? "OK" : "GRESKA"));
    }

    private static int lightestFailingError(long[] h) {
        for (int weight = 1; weight <= N; weight++) {
            for (int error = 0; error < LinearCode.twoTo(N); error++) {
                if (LinearCode.weight(error) == weight && decodeGallager(h, error) != 0) {
                    return error;
                }
            }
        }
        return 0;
    }

    private static void printFourCycles(long[] h, int failing) {
        int cycles = 0;
        int withoutPartner = 0;

        for (int first = 0; first < N; first++) {
            boolean hasPartner = false;

            for (int second = 0; second < N; second++) {
                if (second != first && ParityCheckMatrix.sharedChecks(h, first, second) >= 2) {
                    hasPartner = true;

                    if (second > first) {
                        cycles++;
                    }
                }
            }

            if (!hasPartner) {
                withoutPartner++;
            }
        }

        System.out.println("\nparova kolona sa >= 2 zajednicke provere (4-ciklusi): " + cycles);
        System.out.println("kolona bez takvog partnera: " + withoutPartner);

        for (int bit = 0; bit < N; bit++) {
            if (ParityCheckMatrix.bit(failing, bit) == 0) {
                continue;
            }

            StringBuilder partners = new StringBuilder();
            for (int other = 0; other < N; other++) {
                if (other != bit && ParityCheckMatrix.sharedChecks(h, bit, other) >= 2) {
                    if (partners.length() > 0) {
                        partners.append(", ");
                    }
                    partners.append(other);
                }
            }
            System.out.println("partneri bita " + bit + " u 4-ciklusu: " + partners);
        }
    }

    private static void printFailureCounts(long[] h) {
        int[] failed = new int[N + 1];
        int[] total = new int[N + 1];

        for (int error = 0; error < LinearCode.twoTo(N); error++) {
            int weight = LinearCode.weight(error);
            total[weight]++;

            if (decodeGallager(h, error) != 0) {
                failed[weight]++;
            }
        }

        System.out.println("\nneispravljene greske po tezini:");
        for (int weight = 0; weight <= N; weight++) {
            System.out.println("tezina " + weight + ": " + failed[weight] + " od " + total[weight]);
        }
    }

    private static void trace(long[] h, int error) {
        System.out.println("\nposlato:   " + bits(0, N));
        System.out.println("greska:    " + bits(error, N)
                + "   tezina " + LinearCode.weight(error)
                + ", bitovi " + positions(error));

        int current = error;
        for (int iteration = 1; iteration <= SHOWN_ITERATIONS; iteration++) {
            if (LinearCode.syndrome(h, current) == 0) {
                break;
            }

            int next = GallagerB.oneIteration(h, N, error, current, THRESHOLD_ZERO, THRESHOLD_ONE);

            System.out.println("iteracija " + iteration + ": " + bits(next, N)
                    + "   prevrnuti bitovi: " + positions(current ^ next));
            current = next;
        }

        int result = decodeGallager(h, error);

        if (result == GallagerB.NOT_DECODED) {
            System.out.println("ishod: ne konvergira ni posle " + MAX_ITERATIONS + " iteracija");
        } else if (result == 0) {
            System.out.println("ishod: ispravljeno, vracena poslata rec");
        } else {
            System.out.println("ishod: konvergirao na pogresnu kodnu rec " + bits(result, N));
        }
    }

    private static int firstCorrectableError(long[] h) {
        for (int error = 1; error < LinearCode.twoTo(N); error++) {
            if (decodeGallager(h, error) == 0) {
                return error;
            }
        }
        return 0;
    }

    private static int decodeGallager(long[] h, int received) {
        return GallagerB.decode(h, N, received, MAX_ITERATIONS, THRESHOLD_ZERO, THRESHOLD_ONE);
    }

    private static void printSyndromeTable(long[] h, int[] codewords) {
        int[] corrector = SyndromeTable.build(h, N, ROWS);

        System.out.println("\ndostiznih sindroma: " + SyndromeTable.reachable(corrector)
                + "   (od " + corrector.length + " mogucih)");

        System.out.println("\nsindrom     korektor          tezina");
        for (int syndrome = 0; syndrome < corrector.length; syndrome++) {
            if (corrector[syndrome] == SyndromeTable.UNREACHABLE) {
                continue;
            }
            System.out.println(bits(syndrome, ROWS) + "   " + bits(corrector[syndrome], N)
                    + "   " + LinearCode.weight(corrector[syndrome]));
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
        for (int received = 0; received < LinearCode.twoTo(N); received++) {
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
                howMany[LinearCode.weight(entry)]++;
            }
        }

        System.out.println("\nkorektori po tezini:");
        for (int weight = 0; weight <= N; weight++) {
            if (howMany[weight] > 0) {
                System.out.println("tezina " + weight + ": " + howMany[weight]);
            }
        }
    }

    private static void printRank(long[] h, int[] codewords) {
        int rank = LinearCode.rank(h, N);
        int k = N - rank;

        System.out.println("\nrank(H) = " + rank + "   (od " + ROWS + " redova)");
        System.out.println("k = n - rank = " + k);
        System.out.println("kodnih reci: " + codewords.length + "   (ocekivano 2^" + k + ")");

        int rowsPerGroup = ROWS / COLUMN_WEIGHT;
        int allOnes = LinearCode.twoTo(N) - 1;
        boolean groupsOk = true;

        System.out.println();
        for (int group = 0; group < COLUMN_WEIGHT; group++) {
            int sum = LinearCode.groupSum(h, group, rowsPerGroup);
            System.out.println("zbir redova grupe " + (group + 1) + ": " + bits(sum, N));
            groupsOk = groupsOk && sum == allOnes;
        }
        System.out.println("sve tri grupe daju isti vektor -> " + (COLUMN_WEIGHT - 1)
                + " zavisnosti, rank = " + ROWS + " - " + (COLUMN_WEIGHT - 1));

        boolean countOk = codewords.length == LinearCode.twoTo(k);

        System.out.println("\nprovera: " + (groupsOk && countOk ? "OK" : "GRESKA"));
    }

    private static void printDistance(long[] h, int[] codewords) {
        int lightest = LinearCode.lightestCodeword(codewords);
        int distance = LinearCode.weight(lightest);

        System.out.println("\nd(C) = " + distance);
        System.out.println("najlaksa nenulta kodna rec: " + bits(lightest, N));
        System.out.println("jedinice na pozicijama:     " + positions(lightest));

        int columnSum = 0;
        for (int column = 0; column < N; column++) {
            if (ParityCheckMatrix.bit(lightest, column) == 1) {
                columnSum = columnSum ^ ParityCheckMatrix.column(h, column);
            }
        }
        System.out.println("XOR tih kolona H:           " + bits(columnSum, ROWS)
                + "   (nula -> kolone su zavisne)");

        System.out.println("\nidenticne kolone H:");
        int duplicates = 0;
        for (int first = 0; first < N; first++) {
            for (int second = first + 1; second < N; second++) {
                if (ParityCheckMatrix.column(h, first) == ParityCheckMatrix.column(h, second)) {
                    System.out.println("kolona " + first + " = kolona " + second
                            + " = " + bits(ParityCheckMatrix.column(h, first), ROWS));
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
        boolean noZeroColumn = distance > 1;

        System.out.println("\nprovera: " + (dependent && noZeroColumn ? "OK" : "GRESKA"));
    }

    private static String bits(int vector, int length) {
        StringBuilder text = new StringBuilder();

        for (int position = 0; position < length; position++) {
            text.append(ParityCheckMatrix.bit(vector, position));
        }
        return text.toString();
    }

    private static String positions(int vector) {
        StringBuilder text = new StringBuilder();

        for (int position = 0; position < N; position++) {
            if (ParityCheckMatrix.bit(vector, position) == 1) {
                if (!text.isEmpty()) {
                    text.append(", ");
                }
                text.append(position);
            }
        }
        return text.toString();
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
