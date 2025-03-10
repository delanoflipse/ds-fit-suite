package nl.dflipse.fit.strategy.handlers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.strategy.FaultloadResult;
import nl.dflipse.fit.strategy.FeedbackHandler;
import nl.dflipse.fit.strategy.HistoricStore;
import nl.dflipse.fit.strategy.util.Sets;

public class RedundancyAnalyzer implements FeedbackHandler<Void> {
    private FaultloadResult initialResult;

    private Set<FaultUid> analyzeAppearedFaultUids(FaultloadResult result) {
        var presentFaultUids = result.trace.getFaultUids();
        var appearedFaultUids = Sets.difference(presentFaultUids, initialResult.trace.getFaultUids());

        if (!appearedFaultUids.isEmpty()) {
            System.out.println("New fault points appeared: " + appearedFaultUids);
        }

        return appearedFaultUids;
    }

    private Set<Fault> analyzeDisappearedFaults(FaultloadResult result) {

        // get the intended faults in the faultload
        var intendedFaults = result.faultload.getFaults();
        var injectedFaults = result.trace.getFaults();
        var notInjectedFaults = result.getNotInjectedFaults();

        if (notInjectedFaults.size() == intendedFaults.size()) {
            System.out.println("No faults were injected!");
            System.out.println("There is a high likelyhood of the fault injection not working correctly!");
        } else if (!notInjectedFaults.isEmpty()) {
            System.out.println("Not all faults were injected, missing:" + notInjectedFaults);
            System.out.println("This can be due to redundant faults or a bug in the fault injection!");
        }

        return notInjectedFaults;
    }

    private void detectRandomFaults(Set<FaultUid> appeared, Set<Fault> disappeared) {
        for (var fault : disappeared) {
            var faultWithoutPayload = fault.getUid().asAnyPayload();

            // find appeared faults that match the disappeared fault
            // up to the payload
            List<FaultUid> counterParts = appeared
                    .stream()
                    .filter(f -> f.matches(faultWithoutPayload))
                    .collect(Collectors.toList());

            if (counterParts.isEmpty()) {
                continue;
            }

            System.out.println(
                    "There is a high likelyhood that payloads contain nondeterministic values (either random or time-based)");

            if (counterParts.size() == 1) {
                System.out.println("Fault " + fault + " turned into " + counterParts.get(0));
                continue;
            }

            // if there are multiple appeared faults that match the disappeared fault
            System.out.println("Fault " + fault + " dissapeared, but multiple appeared faults match:");
            for (var appearedFault : counterParts) {
                System.out.println("Matches " + appearedFault);
            }
        }
    }

    @Override
    public Void handleFeedback(FaultloadResult result, HistoricStore history) {
        if (result.isInitial()) {
            initialResult = result;
            return null;
        }

        // ----------------- Analyse the fault injection -----------------
        // Analyse new paths that were not in the original trace
        var appeared = analyzeAppearedFaultUids(result);
        var disappeared = analyzeDisappearedFaults(result);
        detectRandomFaults(appeared, disappeared);

        return null;
    }
}
