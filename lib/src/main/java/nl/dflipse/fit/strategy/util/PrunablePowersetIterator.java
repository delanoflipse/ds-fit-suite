package nl.dflipse.fit.strategy.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PrunablePowersetIterator<T> implements Iterator<Set<T>> {
    private final List<PowersetExpander<T>> toExpand = new ArrayList<>();
    private final List<Set<T>> pruned = new ArrayList<>();

    public record PowersetExpander<T>(Set<T> value, List<T> expansion) {
    }

    public PrunablePowersetIterator(List<T> elements) {
        // this.elements = elements;
        if (elements != null) {
            toExpand.add(new PowersetExpander<>(Set.of(), elements));
        }
    }

    public void expand(PowersetExpander<T> node) {
        if (node.expansion.isEmpty()) {
            return;
        }

        for (int i = 0; i < node.expansion.size(); i++) {
            List<T> newExpansion = node.expansion.subList(i + 1, node.expansion.size());

            Set<T> newValue = new HashSet<>(node.value);
            newValue.add(node.expansion.get(i));

            var newNode = new PowersetExpander<>(newValue, newExpansion);

            // Check if the new value is supposed to be pruned
            var shouldPrune = false;
            for (Set<T> prunedSet : pruned) {
                if (newValue.containsAll(prunedSet)) {
                    shouldPrune = true;
                    break;
                }
            }
            if (shouldPrune) {
                continue;
            }

            for (var prunedSet : pruned) {
                newNode = pruneExtensionsFor(newNode, prunedSet);
            }

            toExpand.add(newNode);
        }
    }

    @Override
    public boolean hasNext() {
        boolean hasMore = !toExpand.isEmpty();
        return hasMore;
    }

    @Override
    public Set<T> next() {
        PowersetExpander<T> node = toExpand.remove(0);
        expand(node);
        var value = node.value;
        return value;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private PowersetExpander<T> pruneExtensionsFor(PowersetExpander<T> node, Set<T> subset) {
        // if the nodes value is all but one element of the subset
        Set<T> min = Sets.difference(node.value, subset);
        if (min.size() == 1) {
            // then we don't have to expand to that value
            T element = Sets.getOnlyElement(min);
            return new PowersetExpander<>(node.value, Lists.minus(node.expansion, element));
        } else {
            return node;
        }
    }

    public void prune(Set<T> subset) {
        pruned.add(subset);

        // Remove pruned nodes
        toExpand.removeIf(expander -> expander.value.containsAll(subset));
        toExpand.replaceAll(node -> pruneExtensionsFor(node, subset));
    }
}
