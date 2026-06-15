package net.chamosmp.chamoitemskins.command.suggestions;

import net.strokkur.commands.CustomSuggestion;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@CustomSuggestion
public @interface betterModelIdSuggestions {
}
