package io.github.delanoflipse.fit.suite.strategy.components.analyzers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.delanoflipse.fit.suite.strategy.FaultloadResult;
import io.github.delanoflipse.fit.suite.strategy.components.FeedbackContext;
import io.github.delanoflipse.fit.suite.strategy.components.FeedbackHandler;
import io.github.delanoflipse.fit.suite.strategy.components.PruneContext;
import io.github.delanoflipse.fit.suite.strategy.components.Reporter;
import io.github.delanoflipse.fit.suite.trace.tree.TraceReport;

public class HappyPathDetector implements FeedbackHandler, Reporter {

    @Override
    public void handleFeedback(FaultloadResult result, FeedbackContext context) {
        if (!result.isInitial()) {
            // TODO: handle case where alternative paths are also considered happy paths
            // But, this has issues with the check for retries in the happy path
            return;
        }

        for (var report : result.trace.getReports()) {
            if (report.response.isErrenous()) {
                continue;
            }

            var happyPath = context.getHappyPath(report.faultUid);
            if (happyPath != null) {
                continue;
            }

            List<TraceReport> children = result.trace.getChildren(report);
            boolean isHappyPath = children.stream()
                    .allMatch(r -> !r.hasFaultBehaviour());

            if (isHappyPath) {
                context.reportHappyPath(report);
            }
        }
    }

    @Override
    public Map<String, String> report(PruneContext context) {
        Map<String, String> report = new LinkedHashMap<>();
        var happyPath = context.getHappyPaths();

        if (!happyPath.isEmpty()) {
            var i = 0;
            for (var entry : happyPath) {
                i++;
                var response = entry.response;
                report.put("[" + i + "] Point", entry.faultUid.toString());
                String bodyLimited = response.body.replace("\n", "");
                if (response.body.length() > 200) {
                    bodyLimited = response.body.substring(0, 97) + "...";
                }
                report.put("[" + i + "] Response", "[" + response.status + "] " + bodyLimited);
            }
        }

        return report;
    }

}
