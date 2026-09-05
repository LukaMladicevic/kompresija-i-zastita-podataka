package kompresija;

import java.util.Objects;
import java.util.PriorityQueue;

public final class Huffman {

    private static final class Node {
        final long weight;
        final int symbol;
        final Node left;
        final Node right;

        Node(int symbol, long weight) {
            this.symbol = symbol;
            this.weight = weight;
            this.left = null;
            this.right = null;
        }

        Node(Node left, Node right) {
            this.symbol = -1;
            this.weight = left.weight + right.weight;
            this.left = left;
            this.right = right;
        }

        boolean isLeaf() {
            return left == null;
        }
    }

    private Huffman() {
    }

    public static int[] lengths(long[] counts) {
        int[] lengths = new int[256];

        int distinct = Entropy.distinct(counts);

        if (distinct == 0) {
            return lengths;
        }

        if (distinct == 1) {
            for (int sym = 0; sym < 256; sym++) {
                if (counts[sym] > 0) {
                    lengths[sym] = 1;
                }
            }
            return lengths;
        }

        PriorityQueue<Node> queue = new PriorityQueue<>((a, b) -> Long.compare(a.weight, b.weight));
        for (int sym = 0; sym < 256; sym++) {
            if (counts[sym] > 0) {
                queue.add(new Node(sym, counts[sym]));
            }
        }

        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            queue.add(new Node(left, right));
        }

        assignDepths(Objects.requireNonNull(queue.poll()), 0, lengths);
        return lengths;
    }

    private static void assignDepths(Node node, int depth, int[] lengths) {
        if (node.isLeaf()) {
            lengths[node.symbol] = depth;
            return;
        }

        assignDepths(node.left, depth + 1, lengths);
        assignDepths(node.right, depth + 1, lengths);
    }
}
