package net.chamosmp.chamoitemskins.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class LoggerUtil {

        private static LoggerUtil logger;

        public LoggerUtil() {
            logger = this;
        }

        public static LoggerUtil getLogger() {
            return logger;
        }

        public static void info(String message, LogType type){
            getLogger().log(message, type);
        }

        public static void info(String message){
            getLogger().log(message, LogType.INFO);
        }

    /**
     * The LogType for {@link LoggerUtil#log(String, LogType)}
     * Based on the enum it start the message with a color <p>
     * {@code ERROR} is dark red <p>
     * {@code INFO} is yellow <p>
     * {@code WARNING} is red <p>
     * {@code SUCCESS} is green <p>
     * I was too lazy to check the legacy colors, so I placed them based on what I thought!
     * (This class, was not stolen by maxlego's zutil in zvoteparty that
     * I forked into a better plugin, ChamoParty
     * <p>
     * I had to put an ad there, didn't I?
     */
    public enum LogType{
            ERROR("<dark_red>"),
            INFO("<yellow>"),
            WARNING("<red>"),
            SUCCESS("<green>");

            private final String color;

            private LogType(String color) {
                this.color = color;
            }

            public String getColor() {
                return color;
            }
        }

        public void log(String message, LogType type){
            MiniMessage mm = MiniMessage.miniMessage();
            Bukkit.getConsoleSender().sendMessage(mm.deserialize(MessageUtil.legacyToMiniMessage(type.getColor() + message)));
        }

        public void log(String message){
            MiniMessage mm = MiniMessage.miniMessage();
            Bukkit.getConsoleSender().sendMessage(mm.deserialize(MessageUtil.legacyToMiniMessage(message)));
        }

        public static void staticLog(String message){
            MiniMessage mm = MiniMessage.miniMessage();
            Bukkit.getConsoleSender().sendMessage(mm.deserialize(MessageUtil.legacyToMiniMessage(message)));
        }

        public void log(String message, Object... args){
            log(String.format(message, args));
        }

        public void log(String message,  LogType type, Object... args){
            log(String.format(message, args), type);
        }

        public void log(String[] messages, LogType type){
            for(String message : messages){
                log(message, type);
            }
        }
}
