package me.lokka30.levelledmobs.bukkit.utils;

import org.bukkit.ChatColor;

/*
This utility class contains some temporary constants. These constants are used throughout the code,
but this class will be removed before LM 4 is released.
 */
public final class TempConst {

    private TempConst() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    public static final String PREFIX_INF = "" + ChatColor.WHITE + ChatColor.BOLD + "LM:" + ChatColor.GRAY + " ";

    public static final String PREFIX_WAR = "" + ChatColor.YELLOW + ChatColor.BOLD + "LM:" + ChatColor.GRAY + " ";;

    public static final String PREFIX_SEV = "" + ChatColor.RED + ChatColor.BOLD + "LM:" + ChatColor.GRAY + " ";;

}
