package nl.dflipse.fit.strategy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrunableGenericPowersetTreeIterator<N, E> implements Iterator<Set<N>> {
    private final Logger logger = LoggerFactory.getLogger(PrunableGenericPowersetTreeIterator.class);
    private final List<E> elements;
    private final List<TreeNode<N, E>> toExpand = new ArrayList<>();
    private final List<Set<N>> prunedSubsets = new ArrayList<>();
    private final Map<N, List<Set<N>>> preconditions = new HashMap<>();
    private final Function<E, Set<N>> expandMapper;
    private final Function<N, E> inverseMapper;
    private int maxSize;

    public record TreeNode<N, E>(Set<N> value, List<E> expansion) {
    }

    public PrunableGenericPowersetTreeIterator(List<E> elements, Function<E, Set<N>> extensionMapper,
            Function<N, E> inverseMapper,
            boolean skipEmptySet) {
        this.expandMapper = extensionMapper;
        this.inverseMapper = inverseMapper;
        this.elements = new ArrayList<>();

        if (elements != null && !elements.isEmpty()) {
            this.elements.addAll(elements);
        }

        toExpand.add(new TreeNode<>(Set.of(), List.copyOf(elements)));
        maxSize = 1;

        if (skipEmptySet) {
            this.skip();
        }
    }

    private boolean shouldPrune(TreeNode<N, E> node, Set<N> prunedSubset) {
        return node.value.containsAll(prunedSubset);
    }

    private boolean shouldPrune(TreeNode<N, E> node) {
        // Prune on preconditions
        for (N n : node.value) {
            if (preconditions.containsKey(n)) {
                boolean matches = false;

                // Find if a precondition matches
                for (Set<N> precondition : preconditions.get(n)) {
                    if (Sets.isSubsetOf(precondition, node.value)) {
                        matches = true;
                        break;
                    }
                }

                if (!matches) {
                    logger.debug("Pruning node due to not matching preconditions for {} ({})", n,
                            preconditions.get(n));
                    return true;
                }
            }
        }

        // Prune on subsets
        for (Set<N> prunedSubset : prunedSubsets) {
            if (shouldPrune(node, prunedSubset)) {
                logger.debug("Pruning node due pruned subset {}", prunedSubset);
                return true;
            }
        }

        return false;
    }

    public void expand(TreeNode<N, E> node) {
        if (node == null || node.expansion.isEmpty()) {
            return;
        }

        for (int i = 0; i < node.expansion.size(); i++) {
            E expansionElement = node.expansion.get(i);
            List<E> newExpansion = node.expansion.subList(i + 1, node.expansion.size());

            Set<N> expandsTo = expandMapper.apply(expansionElement);

            for (N additionalElement : expandsTo) {
                Set<N> newValue = Sets.plus(node.value(), additionalElement);
                var newNode = new TreeNode<>(newValue, newExpansion);

                // Check if the new value is supposed to be pruned
                if (shouldPrune(newNode)) {
                    continue;
                }

                toExpand.add(newNode);
            }

        }
    }

    @Override
    public boolean hasNext() {
        boolean hasMore = !toExpand.isEmpty();
        return hasMore;
    }

    public void skip() {
        expand(toExpand.remove(0));
    }

    @Override
    public Set<N> next() {
        TreeNode<N, E> node = toExpand.remove(0);
        expand(node);
        maxSize = Math.max(maxSize, toExpand.size());
        return node.value;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    // Add new element to explore
    public void add(E extension) {
        Set<N> expandsTo = expandMapper.apply(extension);

        for (N additionalElement : expandsTo) {
            Set<N> newValue = Set.of(additionalElement);
            var newNode = new TreeNode<>(newValue, List.copyOf(elements));

            if (shouldPrune(newNode)) {
                continue;
            }

            toExpand.add(newNode);
        }

        elements.add(extension);
    }

    public void addConditional(Set<N> condition, E extension) {
        Set<N> expandsTo = expandMapper.apply(extension);

        // We cannot expand to extensions already in the condition
        Set<E> alreadyExpanded = condition.stream()
                .map(cnd -> inverseMapper.apply(cnd))
                .collect(Collectors.toSet());
        List<E> expansionsLeft = elements.stream()
                .filter(e -> !alreadyExpanded.contains(e))
                .filter(e -> !e.equals(extension))
                .toList();

        for (N additionalElement : expandsTo) {
            // Register precondition
            preconditions
                    .computeIfAbsent(additionalElement, x -> new ArrayList<>())
                    .add(condition);

            // Add new node
            Set<N> newValue = Sets.plus(condition, additionalElement);
            var newNode = new TreeNode<>(newValue, expansionsLeft);

            if (shouldPrune(newNode)) {
                continue;
            }

            toExpand.add(newNode);
        }

        if (!elements.contains(extension)) {
            elements.add(extension);
        }
    }

    public void prune(Set<N> subset) {
        prunedSubsets.add(subset);

        // Remove pruned nodes
        toExpand.removeIf(expander -> shouldPrune(expander, subset));
    }

    public long size(int m) {
        long sum = 0;
        for (var el : toExpand) {
            long contribution = SpaceEstimate.spaceSize(m, el.expansion.size());
            sum += contribution;
        }

        return sum;
    }

    public long size() {
        return size(1);
    }

    public int getMaxQueueSize() {
        return maxSize;
    }

    public int getQueuSize() {
        return toExpand.size();
    }
}
