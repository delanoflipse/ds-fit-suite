package nl.dflipse.fit.util;

import java.util.ArrayList;
import java.util.List;

import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.faultmodes.ErrorFault;
import nl.dflipse.fit.faultload.faultmodes.FaultMode;
import nl.dflipse.fit.trace.tree.TraceSpan;
import nl.dflipse.fit.trace.tree.TraceSpanReport;
import nl.dflipse.fit.trace.tree.TraceSpanResponse;
import nl.dflipse.fit.trace.tree.TraceTreeSpan;

public class NodeBuilder {
  private static int spanCounter = 0;

  NodeBuilder parent = null;
  TraceSpan span = new TraceSpan();
  TraceSpanReport report = null;
  List<TraceTreeSpan> children = new ArrayList<>();

  public NodeBuilder() {
    this("");
  }

  public NodeBuilder(NodeBuilder parent) {
    this(parent.span.traceId);
    withParent(parent);
  }

  public NodeBuilder(String traceId) {
    String spanId = "" + spanCounter++;
    span.traceId = traceId;
    span.spanId = spanId;
    span.startTime = 0;
    span.endTime = 0;
    span.name = "span";
    span.traceState = null;
    span.isError = false;
    span.errorMessage = null;
  }

  public NodeBuilder withService(String name) {
    span.serviceName = name;
    return this;
  }

  public NodeBuilder withParent(String parentSpanId) {
    span.parentSpanId = parentSpanId;
    return this;
  }

  public NodeBuilder withParent(NodeBuilder parent) {
    span.parentSpanId = parent.span.spanId;
    this.parent = parent;
    return this;
  }

  public ReportBuilder withReport(String origin, String signature) {
    return new ReportBuilder(this, origin, signature);
  }

  public ReportBuilder withReport(String signature) {
    if (this.parent != null && this.parent.report != null) {
      return new ReportBuilder(this, this.parent.report.faultUid.destination(), signature);
    }

    return new ReportBuilder(this, signature);
  }

  public NodeBuilder withError() {
    span.isError = true;
    return this;
  }

  public NodeBuilder withChildren(TraceTreeSpan... children) {
    for (var child : children) {
      this.withChild(child);
    }

    return this;
  }

  public NodeBuilder withChildren(NodeBuilder... children) {
    for (var child : children) {
      this.withChild(child);
    }

    return this;
  }

  public NodeBuilder withChild(TraceTreeSpan child) {
    this.children.add(child);
    return this;
  }

  public NodeBuilder withChild(NodeBuilder child) {
    this.children.add(child.withParent(this).build());
    return this;
  }

  public TraceTreeSpan build() {
    TraceTreeSpan node = new TraceTreeSpan();
    node.span = span;
    node.report = report;
    node.children = children;
    return node;
  }

  public class ReportBuilder {
    private TraceSpanReport report = new TraceSpanReport();
    private NodeBuilder builder;

    public ReportBuilder(NodeBuilder builder, String origin, String signature) {
      this.builder = builder;
      report.spanId = builder.span.spanId;
      report.traceId = builder.span.traceId;
      report.faultUid = new FaultUid(origin, builder.span.serviceName, signature, "*", 0);
    }

    public ReportBuilder(NodeBuilder builder, String signature) {
      this.builder = builder;
      report.spanId = builder.span.spanId;
      report.traceId = builder.span.traceId;
      // report.isInitial = true;
      report.faultUid = new FaultUid("<none>", builder.span.serviceName, signature, "*", 0);
    }

    public NodeBuilder buildReport() {
      if (report.response == null) {
        withResponse(200, "OK");
      }

      builder.report = report;
      return builder;
    }

    public TraceTreeSpan build() {
      return buildReport().build();
    }

    public ReportBuilder withFault(FaultMode mode) {
      Fault fault = new Fault(report.faultUid, mode);
      String body = "err";
      this.builder.span.errorMessage = body;
      this.builder.span.isError = true;

      if (fault.mode().getType().equals(ErrorFault.FAULT_TYPE)) {
        int statusCode = Integer.parseInt(fault.mode().getArgs().get(0));
        TraceSpanResponse response = new TraceSpanResponse();
        response.status = statusCode;
        response.body = body;
        report.response = response;
      }

      report.injectedFault = fault;
      return this;
    }

    public ReportBuilder withResponse(int status, String body) {
      TraceSpanResponse response = new TraceSpanResponse();
      response.status = status;
      response.body = body;
      report.response = response;
      return this;
    }
  }
}