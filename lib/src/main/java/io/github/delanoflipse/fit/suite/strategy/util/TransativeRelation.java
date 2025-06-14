package io.github.delanoflipse.fit.suite.strategy.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransativeRelation<X> {
    private final Set<X> elements = new LinkedHashSet<>();
    private final Map<X, X> inverseRelation = new LinkedHashMap<>();
    private final Map<X, Set<X>> relation = new LinkedHashMap<>();
    private final Map<X, Set<X>> transitiveRelations = new LinkedHashMap<>();

    public void addRelation(X parent, X child) {
        elements.add(parent);
        elements.add(child);

        if ((parent == null && child == null) || (parent != null && parent.equals(child))) {
            throw new IllegalArgumentException(
                    "Cannot relate two equal items, this will create a circular dependency.");
        }

        if (hasTransativeRelation(child, parent)) {
            throw new IllegalArgumentException("Adding this relation would create a circular dependency.");
        }

        inverseRelation.put(child, parent);
        relation.computeIfAbsent(parent, k -> new LinkedHashSet<>());
        relation.get(parent).add(child);
        var root = getRoot(parent);
        updateTransitiveRelations(root);
    }

    public void removeRelation(X parent, X child) {
        if (!hasDirectRelation(parent, child)) {
            throw new IllegalArgumentException("No such relation exists.");
        }
        X root = getRoot(parent);
        clearTransativeRelation(root);

        inverseRelation.remove(child);
        relation.get(parent).remove(child);
        if (relation.get(parent).isEmpty()) {
            relation.remove(parent);
        }

        updateTransitiveRelations(root);
    }

    private void clearTransativeRelation(X root) {
        transitiveRelations.remove(root);
        for (X descendant : getChildren(root)) {
            clearTransativeRelation(descendant);
        }
    }

    private Set<X> updateTransitiveRelations(X root) {
        // The children of the child are also transative children of the parent
        Set<X> descendants = new LinkedHashSet<>();
        for (X descendant : getChildren(root)) {
            descendants.add(descendant);
            descendants.addAll(updateTransitiveRelations(descendant));
        }

        // Add the transative relation
        transitiveRelations.computeIfAbsent(root, k -> new LinkedHashSet<>());
        transitiveRelations.get(root).addAll(descendants);
        return descendants;
    }

    public boolean hasDirectRelation(X parent, X child) {
        return relation.containsKey(parent) && relation.get(parent).contains(child);
    }

    public boolean hasTransativeRelation(X parent, X child) {
        return transitiveRelations.containsKey(parent) && transitiveRelations.get(parent).contains(child);
    }

    public boolean areRelated(X item1, X item2) {
        return hasTransativeRelation(item1, item2) || hasTransativeRelation(item2, item1);
    }

    public Set<X> getChildren(X parent) {
        return relation.getOrDefault(parent, Set.of());
    }

    public Set<X> getDecendants(X parent) {
        return transitiveRelations.getOrDefault(parent, Set.of());
    }

    public X getParent(X child) {
        return inverseRelation.get(child);
    }

    public X getRootOf(X child) {
        X parent = getParent(child);
        if (parent == null) {
            return child;
        } else {
            return getRootOf(parent);
        }
    }

    public List<Pair<X, X>> getRelations() {
        List<Pair<X, X>> relations = new ArrayList<>();
        for (X parent : relation.keySet()) {
            for (X child : relation.get(parent)) {
                relations.add(new Pair<>(parent, child));
            }
        }
        return relations;
    }

    public List<Pair<X, X>> getTransativeRelations() {
        List<Pair<X, X>> relations = new ArrayList<>();
        for (X parent : transitiveRelations.keySet()) {
            for (X child : transitiveRelations.get(parent)) {
                relations.add(new Pair<>(parent, child));
            }
        }
        return relations;
    }

    public List<X> getParents(X child) {
        List<X> parents = new ArrayList<>();
        X parent = getParent(child);
        while (parent != null) {
            parents.add(parent);
            parent = getParent(parent);
        }

        return parents;
    }

    public X getFirstCommonAncestor(X child1, X child2) {
        List<X> parents1 = getParents(child1);
        List<X> parents2 = getParents(child2);
        parents1.retainAll(parents2);
        return parents1.stream().findFirst().orElse(null);
    }

    public X getRoot(X child) {
        X parent = getParent(child);
        if (parent == null) {
            return child;
        } else {
            return getRoot(parent);
        }
    }

    public Set<X> getElements() {
        return elements;
    }

    public List<X> topologicalOrder() {
        // (this is Kahn's algorithm)
        var roots = elements.stream()
                .filter(x -> getParent(x) == null)
                .toList();

        var edges = getRelations();

        var ordered = new ArrayList<X>();
        var front = new ArrayList<>(roots);

        // no roots, just return them all
        if (roots.isEmpty()) {
            return List.copyOf(elements);
        }

        while (!front.isEmpty()) {
            X el = front.remove(0);
            ordered.add(el);

            var edgesFrom = edges.stream()
                    .filter(x -> x.first().equals(el))
                    .toList();
            for (var edge : edgesFrom) {
                var edgeDestination = edge.second();
                edges.remove(edge);
                var edgesLeft = edges.stream()
                        .filter(x -> x.second().equals(edgeDestination))
                        .count();
                if (edgesLeft == 0) {
                    front.add(edgeDestination);
                }
            }
        }

        return ordered;
    }

}
