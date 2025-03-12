package nl.dflipse.fit.strategy.pruners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.faultload.faultmodes.ErrorFault;
import nl.dflipse.fit.strategy.FaultloadResult;
import nl.dflipse.fit.strategy.FeedbackHandler;

public class DynamicReductionPruner implements Pruner, FeedbackHandler<Void> {

    private Map<FaultUid, Set<FaultUid>> causalMap = new HashMap<>();
    private List<Map<FaultUid, Integer>> behavioursSeen = new ArrayList<>();

    @Override
    public Void handleFeedback(FaultloadResult result) {
        // update behaviours seen
        Map<FaultUid, Integer> behaviourMap = new HashMap<>();
        for (var report : result.trace.getReports()) {
            FaultUid uid = report.faultUid;
            int behaviour = report.response.status;
            behaviourMap.put(uid, behaviour);
        }

        behavioursSeen.add(behaviourMap);

        // update causal map
        for (var parentChild : result.trace.getRelations()) {
            FaultUid parent = parentChild.first();
            FaultUid child = parentChild.second();

            if (!causalMap.containsKey(parent)) {
                causalMap.put(parent, new HashSet<>());
            }

            causalMap.get(parent).add(child);
        }

        return null;
    }

    private boolean hasExpectedOutcome(FaultUid faultUid, Fault fault, int observedOutcome) {
        boolean hasFault = fault != null;
        boolean faultDisturbs = hasFault && fault.getMode().getType() == ErrorFault.FAULT_TYPE;

        // If we are supposed to inject a fault
        if (hasFault && faultDisturbs) {
            int expectedOutcome = Integer.parseInt(fault.getMode().getArgs().get(0));
            if (expectedOutcome != observedOutcome) {
                return false;
            }
        } else {
            // If we are not injecting faults, then we should not see any effects
            if (observedOutcome > 299) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean prune(Faultload faultload) {
        Map<FaultUid, Fault> faultsByFaultUid = faultload.faultSet()
                .stream()
                .collect(Collectors.toMap(Fault::getUid, Function.identity()));

        // for all causes
        for (var cause : causalMap.keySet()) {
            boolean found = false;

            // there should exist an earlier run
            for (var behaviour : behavioursSeen) {
                var allEffectsSeen = true;

                // that has all dependents with the same expected behaviour
                for (var effect : causalMap.get(cause)) {
                    boolean hasObservedEffect = behaviour.containsKey(effect);
                    if (!hasObservedEffect) {
                        allEffectsSeen = false;
                        break;
                    }

                    boolean hasFault = faultsByFaultUid.containsKey(effect);
                    Fault fault = hasFault ? faultsByFaultUid.get(effect) : null;

                    if (!hasExpectedOutcome(cause, fault, behaviour.get(effect))) {
                        allEffectsSeen = false;
                        break;
                    }
                }

                // if we have seen all effects before, then we might prune
                if (allEffectsSeen) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

}
