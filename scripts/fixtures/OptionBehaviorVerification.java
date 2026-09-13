package com.garden.modoptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class OptionBehaviorVerification {
    public static void main(String[] args) {
        final int[] callbacks = {0};
        Runnable changed = new Runnable() {
            @Override public void run() { callbacks[0]++; }
        };

        BooleanOption toggle = new BooleanOption("toggle", "Toggle", false, OptionScope.CLIENT, changed);
        toggle.set(false);
        check(callbacks[0] == 0, "unchanged boolean must not notify");
        toggle.set(true);
        check(callbacks[0] == 1, "changed boolean must notify once");

        IntegerOption integer = new IntegerOption("integer", "Integer", 0, 0, 100, 10,
                OptionScope.CLIENT, changed);
        integer.setNumericValue(24);
        check(integer.get() == 20, "integer slider must snap to step");
        integer.setNumericValue(21);
        check(callbacks[0] == 2, "same snapped integer must not notify twice");

        DoubleOption decimal = new DoubleOption("decimal", "Decimal", 0.0, 0.0, 1.0, 0.25,
                OptionScope.CLIENT, changed);
        decimal.setNumericValue(0.62);
        check(Double.compare(decimal.get(), 0.5) == 0, "double slider must snap to step");
        decimal.setNumericValue(0.60);
        check(callbacks[0] == 3, "same snapped double must not notify twice");

        EnumOption<String> single = new EnumOption<String>("single", "Single",
                new String[] {"only"}, 0, OptionScope.CLIENT, changed);
        single.cycle();
        check(callbacks[0] == 3, "single-value enum must not notify");

        PrintStream originalError = System.err;
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            BooleanOption failing = new BooleanOption("failing", "Failing", false,
                    OptionScope.CLIENT, new Runnable() {
                        @Override public void run() { throw new IllegalStateException("expected"); }
                    });
            failing.set(true);
            check(failing.get(), "callback failure must not reject the UI value");
        } finally {
            System.setErr(originalError);
        }

        System.out.println("Option behavior verification passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private OptionBehaviorVerification() {}
}
