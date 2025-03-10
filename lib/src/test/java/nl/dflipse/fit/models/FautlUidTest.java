package nl.dflipse.fit.models;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import nl.dflipse.fit.faultload.FaultUid;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class FautlUidTest {

    public static Collection<Object[]> equalUids() {
        return Arrays.asList(new Object[][] {
                { new FaultUid("x", "y", "sign", "#", 0), new FaultUid("x", "y", "sign", "#", 0) },
                { new FaultUid("x", "y", "sign", "*", 0), new FaultUid("x", "y", "sign", "#", 0) },
                { new FaultUid("x", "y", "sign", "#", 0), new FaultUid("x", "y", "sign", "*", 0) },

        });
    }

    @Test
    @ParameterizedTest
    @MethodSource("equalUids")
    public void testMatch(FaultUid f1, FaultUid f2) {
        assert f1.matches(f2);
    }
}
