from .models import TraceReport, FaultUid


class ReportStore:
    reports: list[TraceReport] = []
    reports_by_span_id: dict[str, TraceReport] = {}
    reports_by_trace_id: dict[str, list[TraceReport]] = {}
    reports_by_trace_by_fault_uid: dict[str, dict[FaultUid, TraceReport]] = {}

    def clear(self):
        self.spans = []
        self.spans_by_span_id = {}
        self.spans_by_trace_id = {}

    def remove_by_trace_id(self, trace_id: str):
        if not trace_id in self.reports_by_trace_id:
            pass

        trace_reports = self.reports_by_trace_id.get(trace_id, [])
        self.reports_by_trace_id.pop(trace_id, None)
        
        for trace_report in trace_reports:
            if trace_report in self.reports:
                self.reports.remove(trace_report)
                self.reports_by_span_id.pop(trace_report.span_id, None)
                self.reports_by_trace_by_fault_uid.get(trace_id, {}).pop(trace_report.uid, None)

    def add(self, report: TraceReport):
        self.reports.append(report)
        self.reports_by_span_id[report.span_id] = report
        self.reports_by_trace_id.setdefault(report.trace_id, []).append(report)
        self.reports_by_trace_by_fault_uid.setdefault(
            report.trace_id, {})[report.uid] = report
        return report

    def has_fault_uid_for_trace(self, trace_id: str, fid: FaultUid) -> bool:
        return trace_id in self.reports_by_trace_by_fault_uid and \
            fid in self.reports_by_trace_by_fault_uid.get(trace_id)

    def get_by_span_id(self, span_id: str) -> TraceReport:
        return self.reports_by_span_id.get(span_id, None)

    def get_by_trace_and_fault_uid(self, trace_id: str, fid: FaultUid) -> TraceReport:
        reports_for_trace_by_fault_uid = self.reports_by_trace_by_fault_uid.get(
            trace_id)
        if reports_for_trace_by_fault_uid is None:
            return None
        return reports_for_trace_by_fault_uid.get(fid, None)

    def get_by_trace_id(self, trace_id: str) -> list[TraceReport]:
        return list(self.reports_by_trace_id.get(trace_id, []))
