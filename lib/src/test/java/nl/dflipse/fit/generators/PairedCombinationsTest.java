package nl.dflipse.fit.generators;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import nl.dflipse.fit.strategy.util.Combinatorics;
import nl.dflipse.fit.strategy.util.Pair;
import nl.dflipse.fit.strategy.util.PrunablePairedCombinationsIterator;

public class PairedCombinationsTest {

    @Test
    public void testEmptyList() {
        // Given
        List<Integer> xs = List.of();
        List<Integer> ys = List.of();
        List<List<Pair<Integer, Integer>>> expectedResult = List.of(
                List.of());

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        assertEquals(expectedResult, res);

    }

    @Test
    public void testEmptyRhs() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<Integer> ys = List.of();
        List<List<Pair<Integer, Integer>>> expectedResult = List.of(
                List.of());

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        assertEquals(expectedResult, res);
    }

    @Test
    public void testEmptyLhs() {
        // Given
        List<Integer> xs = List.of();
        List<Integer> ys = List.of(1, 2, 3);
        List<List<Pair<Integer, Integer>>> expectedResult = List.of(
                List.of());

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        assertEquals(expectedResult, res);
    }

    @Test
    public void testOneRhs() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a");
        List<List<Pair<Integer, String>>> expectedResult = List.of(
                List.of(new Pair<>(1, "a"), new Pair<>(2, "a"), new Pair<>(3, "a")));

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        assertEquals(expectedResult, res);
    }

    @Test
    public void testOneLhs() {
        // Given
        List<Integer> xs = List.of(1);
        List<String> ys = List.of("a", "b", "c");
        List<List<Pair<Integer, String>>> expectedResult = List.of(
                List.of(new Pair<>(1, "a")),
                List.of(new Pair<>(1, "b")),
                List.of(new Pair<>(1, "c")));

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        assertEquals(expectedResult, res);
    }

    @Test
    public void testComplex() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");
        List<List<Pair<Integer, String>>> expectedResult = List.of(
                List.of(new Pair<>(1, "a"), new Pair<>(2, "a"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "a"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "a"), new Pair<>(3, "c")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "b"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "b"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "b"), new Pair<>(3, "c")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "c"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "c"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "a"), new Pair<>(2, "c"), new Pair<>(3, "c")),

                List.of(new Pair<>(1, "b"), new Pair<>(2, "a"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "a"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "a"), new Pair<>(3, "c")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "b"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "b"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "b"), new Pair<>(3, "c")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "c"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "c"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "b"), new Pair<>(2, "c"), new Pair<>(3, "c")),

                List.of(new Pair<>(1, "c"), new Pair<>(2, "a"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "a"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "a"), new Pair<>(3, "c")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "b"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "b"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "b"), new Pair<>(3, "c")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "c"), new Pair<>(3, "a")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "c"), new Pair<>(3, "b")),
                List.of(new Pair<>(1, "c"), new Pair<>(2, "c"), new Pair<>(3, "c")));

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        assertEquals(expectedResult, res);
    }

    private int pow(int base, int exp) {
        return (int) Math.pow(base, exp);
    }

    @Test
    public void testSize() {
        // Given
        List<Integer> xs = List.of(1, 2, 3, 4, 5, 6, 7);
        List<String> ys = List.of("a", "b", "c", "d", "e");

        // When
        var res = Combinatorics.cartesianCombinations(xs, ys);

        // Then
        int expectedSize = pow(ys.size(), xs.size());
        assert (res.size() == expectedSize);
    }

    @Test
    public void areEqual() {
        // Given
        List<Integer> xs = List.of(1, 2, 3, 4, 5, 6, 7);
        List<String> ys = List.of("a", "b", "c", "d", "e");

        var res1 = Combinatorics.cartesianCombinations(xs, ys);
        var resIt = new PrunablePairedCombinationsIterator<>(xs, ys);

        // Then
        for (var l : res1) {
            boolean hasNext = resIt.hasNext();
            if (!hasNext) {
                assertTrue(hasNext);
            }
            var r = resIt.next();
            assertEquals(l, r);
        }

        assertFalse(resIt.hasNext());
    }

    private long countAll(PrunablePairedCombinationsIterator gen) {
        long count = 0;
        while (gen.hasNext()) {
            gen.next();
            count++;
        }
        return count;
    }

    @Test
    public void testPruneNone() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        long size = countAll(it);

        // Then
        assertEquals(3 * 3 * 3, size);
    }

    @Test
    public void testPruneOne() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(1, "b")));
        long size = countAll(it);

        // Then
        assertEquals(2 * 3 * 3, size);
    }

    @Test
    public void testPruneAllButOneForOne() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(1, "a")));
        it.prune(Set.of(new Pair<>(1, "b")));
        long size = countAll(it);

        // Then
        assertEquals(1 * 3 * 3, size);
    }

    @Test
    public void testPrune2() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(1, "a")));
        it.prune(Set.of(new Pair<>(2, "b")));
        long size = countAll(it);

        // Then
        assertEquals(2 * 2 * 3, size);
    }

    @Test
    public void testPrune3() {
        // Given
        List<Integer> xs = List.of(1, 2, 3, 4, 5);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(2, "b")));
        it.prune(Set.of(new Pair<>(4, "a")));
        long size = countAll(it);

        // Then
        assertEquals(3 * 2 * 3 * 2 * 3, size);
    }

    @Test
    public void testPruneSubset2() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(1, "a"), new Pair<>(2, "b")));
        long size = countAll(it);

        // Then
        assertEquals(3 * 3 * 3 - 3, size);
    }

    @Test
    public void testPruneSubset2v2() {
        // Given
        List<Integer> xs = List.of(1, 2, 3, 4, 5);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(3, "b"), new Pair<>(2, "c")));
        long size = countAll(it);

        // Then
        assertEquals(3 * 3 * 3 * 3 * 3 - 3 * 3 * 3, size);
    }

    @Test
    public void testPruneSubset2v3() {
        // Given
        List<Integer> xs = List.of(1, 2, 3, 4, 5);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(3, "b"), new Pair<>(2, "c")));
        it.prune(Set.of(new Pair<>(2, "a"), new Pair<>(4, "c")));
        long size = countAll(it);

        // Then
        assertEquals(3 * 3 * 3 * 3 * 3 - 2 * 3 * 3 * 3, size);
    }

    @Test
    public void testPruneSubset2withOverlap() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(1, "a"), new Pair<>(2, "b")));
        it.prune(Set.of(new Pair<>(2, "b"), new Pair<>(3, "c")));
        long size = countAll(it);

        // Then
        assertEquals(3 * 3 * 3 - 3 - 3 + 1, size);
    }

    @Test
    public void testPruneSubset2withSubset() {
        // Given
        List<Integer> xs = List.of(1, 2, 3);
        List<String> ys = List.of("a", "b", "c");

        // When
        var it = new PrunablePairedCombinationsIterator<>(xs, ys);
        it.prune(Set.of(new Pair<>(1, "a"), new Pair<>(2, "b"), new Pair<>(3, "c")));
        it.prune(Set.of(new Pair<>(2, "b"), new Pair<>(3, "c")));
        it.prune(Set.of(new Pair<>(1, "a"), new Pair<>(2, "b"), new Pair<>(3, "c")));
        long size = countAll(it);

        // Then
        assertEquals(3 * 3 * 3 - 3, size);
    }

}
