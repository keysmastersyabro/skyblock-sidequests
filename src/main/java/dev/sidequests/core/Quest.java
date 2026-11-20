package dev.sidequests.core;

import java.util.Locale;

public record Quest(String id, String title, String detail, String category,
                    double current, double target) {
    public double progress() { return Math.max(0, Math.min(1, current / target)); }
    public String remaining() { return String.format(Locale.US, "%,.0f", Math.max(0, target - current)); }
    public String counter() { return String.format(Locale.US, "%,.0f / %,.0f", current, target); }
}
