package com.warpcore;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {
    private static final Pattern SETTING_PATTERN = Pattern.compile("^ [<](\\d)[,] (\\d)[,] (\\d)[,] (\\d)[,] (\\d)[>]");

    public int mWarpFactor = 2;
    public int mHue = (byte) 160;
    public int mSaturation = (byte) 255;
    public int mBrightness = (byte) 160;
    public int mCorePattern = 1;

    public byte[] Encode() {
        return String.format("<%d,%d,%d,%d,%d>", mWarpFactor, mHue, mSaturation, mBrightness, mCorePattern).getBytes();
    }

    public static Settings ParseString(String line) {
        Matcher m = SETTING_PATTERN.matcher(line);
        if (!m.matches()) {
            return null;
        }

        Settings result = new Settings();
        result.mWarpFactor = parseValue(m.group(1));
        result.mHue = parseValue(m.group(2));
        result.mSaturation = parseValue(m.group(3));
        result.mBrightness = parseValue(m.group(4));
        result.mCorePattern = parseValue(m.group(5));
        return result;
    }

    private static int parseValue(String value) {
        return Integer.parseInt(value);
    }
}
