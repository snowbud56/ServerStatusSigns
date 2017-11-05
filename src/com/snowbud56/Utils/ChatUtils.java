package com.snowbud56.Utils;

import org.bukkit.ChatColor;

public class ChatUtils {
    public static String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
