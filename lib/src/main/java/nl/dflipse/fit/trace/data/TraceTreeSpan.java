package nl.dflipse.fit.trace.data;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonSerialize
@JsonDeserialize
public class TraceTreeSpan {
    @JsonProperty("children")
    public List<TraceTreeSpan> children;

    @JsonProperty("span")
    public TraceSpan span;

    @JsonProperty("report")
    public TraceSpanReport report;
}
