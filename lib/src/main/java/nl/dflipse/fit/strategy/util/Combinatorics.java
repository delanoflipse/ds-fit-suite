package nl.dflipse.fit.strategy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Combinatorics {
    public static <T> List<List<T>> generatePowerSet(List<T> originalSet) {
        List<List<T>> powerSet = new ArrayList<>();

        if (originalSet.isEmpty()) {
            powerSet.add(new ArrayList<>());
            return powerSet;
        }

        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);

        List<T> rest = list.subList(1, list.size());

        // P(x :: S) = S ∪ { x ∪ Ps | Ps ∈ S }
        for (List<T> set : generatePowerSet(rest)) {
            // Add the set itself
            powerSet.add(set);

            // add the head to the set, and add that set.
            List<T> newSet = new ArrayList<>();
            newSet.add(head);
            newSet.addAll(set);
            powerSet.add(newSet);
        }

        return powerSet;
    }

    // public static <T> Stream<List<T>> streamPowerSet(List<T> originalSet) {
    // if (originalSet.isEmpty()) {
    // return Stream.of(Collections.emptyList());
    // }

    // T head = originalSet.get(0);
    // List<T> rest = originalSet.subList(1, originalSet.size());

    // // Recursively generate power set using streams
    // return generatePowerSet(rest)
    // .flatMap(subset -> Stream.of(
    // subset, // Without head
    // Stream.concat(Stream.of(head), subset.stream()) // With head
    // .collect(Collectors.toList()) // Convert back to list
    // ));
    // }
}