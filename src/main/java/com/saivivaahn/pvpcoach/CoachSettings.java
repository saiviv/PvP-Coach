package com.saivivaahn.pvpcoach;

/** Shared settings so client HUD commands do not break a dedicated server. */
public final class CoachSettings {
    public static boolean hudEnabled = true;
    public static int hudX = 10;
    public static int hudY = 10;
    public static int textColor = 0xFFFFFFFF;
    public static boolean showFps = true;
    public static boolean showAccuracy = true;
    public static boolean showKeystrokes = true;

    private CoachSettings() { }
}
