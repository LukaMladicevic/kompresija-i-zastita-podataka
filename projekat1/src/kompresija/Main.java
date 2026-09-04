package kompresija;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {

    private static final String INPUT_FILE = "test_ravnomeran.bin";

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

        report("Shannon-Fano", Header.SHANNON_FANO, ShannonFano.lengths(counts), data, ".sf");
        report("Huffman", Header.HUFFMAN, Huffman.lengths(counts), data, ".huf");
    }

    private static void report(String name, int method, int[] lengths, byte[] data, String suffix) throws IOException {
        Path encoded = Path.of(INPUT_FILE + suffix);
        long n = data.length;

        long start = System.nanoTime();
        encodeCanonical(data, lengths, method, encoded);
        double encodeSeconds = (System.nanoTime() - start) / 1e9;

        start = System.nanoTime();
        byte[] decoded = decodeCanonical(encoded);
        double decodeSeconds = (System.nanoTime() - start) / 1e9;

        long size = Files.size(encoded);
        int header = Header.SIZE_IN_BYTES + CanonicalCode.TABLE_BYTES;

        System.out.printf("%-14s %9d %7d %11s %9s %8.3f s %10.3f s %9s%n",
                name,
                size,
                header,
                n == 0 ? "-" : String.format("%.4f", size * 8.0 / n),
                n == 0 ? "-" : String.format("%.2f%%", 100.0 * (1 - (double) size / n)),
                encodeSeconds,
                decodeSeconds,
                Arrays.equals(data, decoded) ? "OK" : "GRESKA");
    }

    private static void encodeCanonical(byte[] data, int[] lengths, int method, Path out) throws IOException {
        int[] codes = CanonicalCode.buildCodes(lengths);

        try (BitWriter w = new BitWriter(new BufferedOutputStream(Files.newOutputStream(out)))) {
            new Header(method, data.length).write(w);
            CanonicalCode.writeLengths(w, lengths);
            CanonicalCode.encode(w, data, lengths, codes);
        }
    }

    private static byte[] decodeCanonical(Path in) throws IOException {
        try (BitReader r = new BitReader(new BufferedInputStream(Files.newInputStream(in)))) {
            Header header = Header.read(r);
            int[] lengths = CanonicalCode.readLengths(r);
            return CanonicalCode.decode(r, lengths, header.n);
        }
    }
}
