package net.chamosmp.chamoitemskins.util;

import org.bukkit.Bukkit;

public class LoggerUtil {
    public enum LogType {
        SEVERE("<dark_red>"),
        WARNING("<yellow>"),
        INFO("<white>");

        private final String color;

        LogType(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }


    }

    public static void log(LogType type, String message) {
        Bukkit.getConsoleSender().sendMessage(MessageUtil.parse("<light_purple>ChamoItemSkins</light_purple>| " + type.getColor() + message));
    }
}
