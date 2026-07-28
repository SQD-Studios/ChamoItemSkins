package net.chamosmp.chamoitemskins.util;

import org.bukkit.Bukkit;
import org.slf4j.Logger;

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
        Bukkit.getConsoleSender().sendMessage(MessageUtil.parse("<light_purple>ChamoItemSkins| " + type.getColor() + message));
    }
}
