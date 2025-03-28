package nl.dflipse.fit.strategy;

import java.util.Set;

import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.trace.TraceParent;
import nl.dflipse.fit.trace.TraceState;
import nl.dflipse.fit.util.TaggedTimer;

public class TrackedFaultload {
    private final Faultload faultload;

    private TraceParent traceParent;
    private TraceState traceState;

    public TaggedTimer timer = new TaggedTimer();
    public int getDelayMs = 100;

    public TrackedFaultload() {
        this(new Faultload(Set.of()));
    }

    public TrackedFaultload(Faultload faultload) {
        this.faultload = faultload;

        traceParent = new TraceParent();

        traceState = new TraceState();
        traceState.set("fit", "1");
        traceState.set("init", "1");
    }

    public TrackedFaultload withMaskPayload() {
        traceState.set("mask", "1");
        return this;
    }

    public TrackedFaultload withBodyHashing() {
        traceState.set("hashbody", "1");
        return this;
    }

    public TrackedFaultload withHeaderLog() {
        traceState.set("headerlog", "1");
        return this;
    }

    public TrackedFaultload withGetDelay(int ms) {
        this.getDelayMs = ms;
        return this;
    }

    public Faultload getFaultload() {
        return faultload;
    }

    public Set<FaultUid> getFaultUids() {
        return faultload.getFaultUids();
    }

    public String readableString() {
        return getTraceId() + "-" + faultload.readableString();
    }

    public boolean hasFaultMode(String... faultType) {
        return faultload.hasFaultMode(faultType);
    }

    public String serializeJson() {
        return TrackedFaultloadSerializer.serializeJson(this);
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
        return faultload.size();
    }
}
