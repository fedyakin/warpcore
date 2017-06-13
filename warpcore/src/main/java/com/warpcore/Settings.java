package com.warpcore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {
    public byte WarpFactor = 2;
    public byte Hue = (byte) 160;
    public byte Saturation = (byte) 255;
    public byte Brightness = (byte) 160;
    public byte Pattern = 1;

    public byte[] Encode() {
        return String.format("<%d,%d,%d,%d,%d>", WarpFactor, Hue, Saturation, Brightness, Pattern).getBytes();
    }

    public boolean ParseString(String line) {
       // String re = "\\s*[*]{2}\\s*Current Settings\\s*[*]{2}\\s*<(\\d),(\\d),(\\d),(\\d),(\\d)>\\s*";
        String re = "\\s*";
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(line);
        if (!m.matches()) {
            return false;
        }

        WarpFactor = parseValue(m.group(0));
        Hue = parseValue(m.group(1));
        Saturation = parseValue(m.group(2));
        Brightness = parseValue(m.group(3));
        Pattern = parseValue(m.group(4));
        return true;
    }

    private static byte parseValue(String value) {
        return ((byte) Integer.parseInt(value));
    }
}
