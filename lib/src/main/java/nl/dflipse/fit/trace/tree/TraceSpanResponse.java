package nl.dflipse.fit.trace.tree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonSerialize
@JsonDeserialize
public class TraceSpanResponse {
    @JsonProperty("status")
    public int status;

    @JsonProperty("body")
    public String body;

    public boolean isErrenous() {
        return !(status >= 200 && status < 300);
    }
}
