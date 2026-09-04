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

        int[] lengths = ShannonFano.lengths(counts);
        Path encoded = Path.of(INPUT_FILE + ".sf");

        long start = System.nanoTime();
        encodeCanonical(data, lengths, Header.SHANNON_FANO, encoded);
        double encodeSeconds = (System.nanoTime() - start) / 1e9;

        start = System.nanoTime();
        byte[] decoded = decodeCanonical(encoded);
        double decodeSeconds = (System.nanoTime() - start) / 1e9;

        long encodedSize = Files.size(encoded);
        int headerSize = Header.SIZE_IN_BYTES + CanonicalCode.TABLE_BYTES;

        System.out.println("SHANNON-FANO");
        System.out.printf("  najduza kodna rec:  %d bita%n", CanonicalCode.maxLength(lengths));
        System.out.printf("  kodiran fajl:       %d bajtova (header %d)%n", encodedSize, headerSize);
        System.out.printf("  bita po bajtu (L):  %.6f%n", n == 0 ? 0.0 : encodedSize * 8.0 / n);
        System.out.printf("  usteda:             %.2f%%%n", n == 0 ? 0.0 : 100.0 * (1 - (double) encodedSize / n));
        System.out.printf("  kodiranje:          %.3f s%n", encodeSeconds);
        System.out.printf("  dekodiranje:        %.3f s%n", decodeSeconds);
        System.out.printf("  verifikacija:       %s%n", Arrays.equals(data, decoded) ? "OK" : "GRESKA");
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
