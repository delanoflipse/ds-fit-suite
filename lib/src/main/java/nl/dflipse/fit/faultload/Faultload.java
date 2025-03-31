package nl.dflipse.fit.faultload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Faultload(Set<Fault> faultSet) {

    public Set<FaultUid> getFaultUids() {
        Set<FaultUid> faultUids = new HashSet<>();
        for (Fault fault : faultSet) {
            faultUids.add(fault.uid());
        }
        return faultUids;
    }

    public Map<FaultUid, Fault> getFaultByFaultUid() {
        return faultSet.stream()
                .collect(Collectors.toMap(Fault::uid, Function.identity()));
    }

    public String readableString() {
        List<String> readableFaults = new ArrayList<>();

        for (Fault fault : faultSet) {
            readableFaults.add(fault.uid().toString() + "(" + fault.mode().getType() + " "
                    + fault.mode().getArgs() + ")");
        }

        return String.join(", ", readableFaults);
    }

    public Set<Fault> ofType(String faultType) {
        return faultSet.stream()
                .filter(fault -> fault.mode().getType().equals(faultType))
                .collect(Collectors.toSet());
    }

    public boolean hasFaultMode(String... faultType) {
        for (Fault fault : faultSet) {
            for (String type : faultType) {
                if (fault.mode().getType().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int size() {
        return faultSet.size();
    }
}
