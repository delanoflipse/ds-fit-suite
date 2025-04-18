package nl.dflipse.fit.strategy.components.generators;

import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.strategy.components.FeedbackContext;

public abstract class Generator extends FeedbackContext {
    public abstract Faultload generate();

    public abstract void prune();
}
