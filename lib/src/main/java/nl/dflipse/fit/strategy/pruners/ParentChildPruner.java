package nl.dflipse.fit.strategy.pruners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.strategy.FaultloadResult;
import nl.dflipse.fit.strategy.FeedbackContext;
import nl.dflipse.fit.strategy.FeedbackHandler;
import nl.dflipse.fit.strategy.util.Sets;

public class ParentChildPruner implements Pruner, FeedbackHandler {
    private final Map<FaultUid, Set<FaultUid>> parentChildMapping = new HashMap<>();

    @Override
    public void handleFeedback(FaultloadResult result, FeedbackContext context) {
        for (var pair : result.trace.getParentsAndChildren()) {
            var parent = pair.getFirst();
            var child = pair.getSecond();
            parentChildMapping.putIfAbsent(parent, new HashSet<>());
            parentChildMapping.get(parent).add(child);
        }

        for (var pair : parentChildMapping.entrySet()) {
            var parent = pair.getKey();
            var children = pair.getValue();

            for (var child : children) {
                if (parent == null || child == null) {
                    continue;
                }

                // The parent makes the child disappear
                // so we can prune the combination
                context.pruneFaultUidSubset(Set.of(parent, child));
            }

        }

        return;
    }

    private boolean areRelated(FaultUid parent, FaultUid child) {
        Set<FaultUid> children = parentChildMapping.get(parent);
        if (children == null) {
            return false;
        }

        return children.contains(child);
    }

    @Override
    public boolean prune(Faultload faultload) {
        // if an http error is injected, and its children are also injected, it is
        // redundant.

        Set<FaultUid> errorFaults = faultload.getFaultUids();

        boolean isRedundant = Sets.anyPair(errorFaults, (pair) -> {
            var f1 = pair.getFirst();
            var f2 = pair.getSecond();
            return areRelated(f1, f2) || areRelated(f2, f1);
        });

        return isRedundant;
    }

}
