package nl.dflipse.fit.faultload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.dflipse.fit.trace.TraceParent;
import nl.dflipse.fit.trace.TraceState;

public class Faultload {
    private final Set<Fault> faults;

    private TraceParent traceParent;
    private TraceState traceState;

    public Faultload() {
        this(new HashSet<>());
    }

    public Faultload(Set<Fault> faults) {
        this.faults = faults;
        traceParent = new TraceParent();

        traceState = new TraceState();
        traceState.set("fit", "1");
    }

    public Set<Fault> getFaults() {
        return faults;
    }

    public Set<FaultUid> getFaultUids() {
        Set<FaultUid> faultUids = new HashSet<>();
        for (Fault fault : faults) {
            faultUids.add(fault.getUid());
        }
        return faultUids;
    }

    public String readableString() {
        List<String> readableFaults = new ArrayList<>();

        for (Fault fault : faults) {
            readableFaults.add(fault.getUid().toString() + "(" + fault.getMode().getType() + " "
                    + fault.getMode().getArgs() + ")");
        }

        return String.join(", ", readableFaults);
    }

    public String serializeJson() {
        return FaultloadSerializer.serializeJson(this);
    }

    public String getTraceId() {
        return traceParent.traceId;
    }

    public TraceParent getTraceParent() {
        return traceParent;
    }

    public TraceState getTraceState() {
        return traceState;
    }

    public int size() {
        return faults.size();
    }
}
