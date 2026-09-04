package kompresija;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        System.out.printf("N*H/8 bajtova:    %.1f%n", n * h / 8.0);
    }
}
