package kompresija;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {

    private static final String INPUT_FILE = "sekspir.txt";

    public static void main(String[] args) throws IOException {
        Path input = Path.of(INPUT_FILE);

        if (!Files.isReadable(input)) {
            System.out.println("Ne mogu da procitam fajl: " + input.toAbsolutePath());
            return;
        }

        byte[] data = Files.readAllBytes(input);
        long n = data.length;

        long[] counts = Entropy.histogram(data);
        double h = Entropy.entropy(counts, n);

        System.out.println("fajl:               " + INPUT_FILE);
        System.out.printf("N - bajtova:        %d%n", n);
        System.out.printf("razlicitih bajtova: %d%n", Entropy.distinct(counts));
        System.out.printf("H - bita po bajtu:  %.6f%n", h);
        System.out.printf("N*H/8 bajtova:      %.1f%n", n * h / 8.0);
        System.out.println();

        System.out.printf("%-14s %9s %7s %11s %9s %10s %12s %9s%n",
                "metod", "kodiran", "header", "bita/bajt", "usteda", "kodiranje", "dekodiranje", "provera");
        System.out.println("-".repeat(90));

        reportCanonical("Shannon-Fano", Header.SHANNON_FANO, ShannonFano.lengths(counts), data, ".sf");
        reportCanonical("Huffman", Header.HUFFMAN, Huffman.lengths(counts), data, ".huf");
        reportLZ77(data);
        reportLZW(data);
    }

    private static void reportLZW(byte[] data) throws IOException {
        Path encoded = Path.of(INPUT_FILE + ".lzw");

        long start = System.nanoTime();
        try (BitWriter w = new BitWriter(new BufferedOutputStream(Files.newOutputStream(encoded)))) {
            new Header(Header.LZW, data.length).write(w);
            LZW.encode(w, data);
        }
        double encodeSeconds = seconds(start);

        start = System.nanoTime();
        byte[] decoded;
        try (BitReader r = new BitReader(new BufferedInputStream(Files.newInputStream(encoded)))) {
            Header header = Header.read(r);
            decoded = LZW.decode(r, header.n);
        }
        double decodeSeconds = seconds(start);

        printRow("LZW", Files.size(encoded), Header.SIZE_IN_BYTES + LZW.PARAMETER_BYTES,
                data, decoded, encodeSeconds, decodeSeconds);
    }

    private static void reportCanonical(String name, int method, int[] lengths, byte[] data, String suffix)
            throws IOException {
        Path encoded = Path.of(INPUT_FILE + suffix);
        int[] codes = CanonicalCode.buildCodes(lengths);

        long start = System.nanoTime();
        try (BitWriter w = new BitWriter(new BufferedOutputStream(Files.newOutputStream(encoded)))) {
            new Header(method, data.length).write(w);
            CanonicalCode.writeLengths(w, lengths);
            CanonicalCode.encode(w, data, lengths, codes);
        }
        double encodeSeconds = seconds(start);

        start = System.nanoTime();
        byte[] decoded;
        try (BitReader r = new BitReader(new BufferedInputStream(Files.newInputStream(encoded)))) {
            Header header = Header.read(r);
            decoded = CanonicalCode.decode(r, CanonicalCode.readLengths(r), header.n);
        }
        double decodeSeconds = seconds(start);

        printRow(name, Files.size(encoded), Header.SIZE_IN_BYTES + CanonicalCode.TABLE_BYTES,
                data, decoded, encodeSeconds, decodeSeconds);
    }

    private static void reportLZ77(byte[] data) throws IOException {
        Path encoded = Path.of(INPUT_FILE + ".lz77");

        long start = System.nanoTime();
        try (BitWriter w = new BitWriter(new BufferedOutputStream(Files.newOutputStream(encoded)))) {
            new Header(Header.LZ77, data.length).write(w);
            LZ77.encode(w, data);
        }
        double encodeSeconds = seconds(start);

        start = System.nanoTime();
        byte[] decoded;
        try (BitReader r = new BitReader(new BufferedInputStream(Files.newInputStream(encoded)))) {
            Header header = Header.read(r);
            decoded = LZ77.decode(r, header.n);
        }
        double decodeSeconds = seconds(start);

        printRow("LZ77", Files.size(encoded), Header.SIZE_IN_BYTES + LZ77.PARAMETER_BYTES,
                data, decoded, encodeSeconds, decodeSeconds);
    }

    private static void printRow(String name, long size, int header, byte[] data, byte[] decoded,
                                 double encodeSeconds, double decodeSeconds) {
        long n = data.length;
        String check = Arrays.equals(data, decoded) ? "OK" : "GRESKA";

        System.out.printf("%-14s %9d %7d %11s %9s %8.3f s %10.3f s %9s%n",
                name, size, header, bitsPerByte(size, n), savings(size, n), encodeSeconds, decodeSeconds, check);
    }

    private static double seconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1e9;
    }

    private static String bitsPerByte(long size, long n) {
        if (n == 0) {
            return "-";
        }
        return String.format("%.4f", size * 8.0 / n);
    }

    private static String savings(long size, long n) {
        if (n == 0) {
            return "-";
        }
        double ratio = (double) size / n;
        return String.format("%.2f%%", 100.0 * (1 - ratio));
    }
}
