package ldpc;

public final class Bits {

    private Bits() {
    }

    public static int get(int vector, int position) {
        return (vector >>> position) & 1;
    }

    public static int set(int vector, int position) {
        return vector | (1 << position);
    }

    public static int weight(int vector) {
        return Integer.bitCount(vector);
    }

    public static int parity(int vector) {
        return Integer.bitCount(vector) % 2;
    }

    public static int twoTo(int exponent) {
        return 1 << exponent;
    }

    public static String toText(int vector, int length) {
        StringBuilder text = new StringBuilder();

        for (int position = 0; position < length; position++) {
            text.append(get(vector, position));
        }
        return text.toString();
    }
}
