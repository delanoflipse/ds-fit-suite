package nl.dflipse.fit.trace.tree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonSerialize
@JsonDeserialize
public class TraceSpanReport {
    @JsonProperty("trace_id")
    public String traceId;

    @JsonProperty("span_id")
    public String spanId;

    @JsonProperty("uid")
    public FaultUid faultUid;

    @JsonProperty("injected_fault")
    public Fault injectedFault;

    @JsonProperty("is_initial")
    public boolean isInitial;

    @JsonProperty("response")
    public TraceSpanResponse response;
}
