package io.github.delanoflipse.fit.suite.instrument.controller;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.github.delanoflipse.fit.suite.trace.tree.TraceReport;

@JsonDeserialize
public class ControllerResponse {

    @JsonProperty("reports")
    public List<TraceReport> reports;

}
