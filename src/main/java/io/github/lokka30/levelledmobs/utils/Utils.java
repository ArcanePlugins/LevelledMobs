package io.github.lokka30.levelledmobs.utils;

import me.lokka30.microlib.MicroLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.List;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static List<String> getSupportedServerVersions() {
        return Arrays.asList("1.15", "1.16");
    }

    public static final MicroLogger logger = new MicroLogger("&b&lLevelledMobs: &7");

    //This is a method created by Jonik & Mustapha Hadid at StackOverflow.
    //It simply grabs 'value', being a double, and rounds it, leaving 'places' decimal places intact.
    //Created by Jonik & Mustapha Hadid @ stackoverflow
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    //Integer check
    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    //Integer check
    public static boolean isInteger(String s, int radix) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

	public static String replaceEx(String original, String pattern, String replacement) {
		if (original == null || pattern == null) return null;
		
		int count, position0, position1;
		count = position0 = position1 = 0;
		String upperString = original.toUpperCase();
		String upperPattern = pattern.toUpperCase();
        int inc = (original.length() / pattern.length()) *
                (replacement.length() - pattern.length());
        char[] chars = new char[original.length() + Math.max(0, inc)];
        while ((position1 = upperString.indexOf(upperPattern, position0)) != -1) {
            for (int i = position0; i < position1; ++i)
                chars[count++] = original.charAt(i);
            for (int i = 0; i < replacement.length(); ++i)
                chars[count++] = replacement.charAt(i);
            position0 = position1 + pattern.length();
        }
        if (position0 == 0) return original;
        for (int i = position0; i < original.length(); ++i)
            chars[count++] = original.charAt(i);

        return new String(chars, 0, count);
    }

    public static boolean isNullOrEmpty(String str) {
        return (str == null || str.isEmpty());
    }

    public static Object getDefaultIfNull(Object object, Object def) {
        return object == null ? def : object;
    }

    public static int getDefaultIfNull(YamlConfiguration cfg, String path, int def) {
        return cfg.contains(path) ? cfg.getInt(path) : def;
    }
}
