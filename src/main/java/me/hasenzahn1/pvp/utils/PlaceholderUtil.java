package me.hasenzahn1.pvp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Map;

public class PlaceholderUtil {

    public static Component parseTemplate(
            String template,
            Map<String, Component> placeholders
    ) {
        TextComponent.Builder builder = Component.text();

        int index = 0;
        while (index < template.length()) {
            int start = template.indexOf('%', index);
            if (start == -1) {
                builder.append(Component.text(template.substring(index)));
                break;
            }

            int end = template.indexOf('%', start + 1);
            if (end == -1) {
                builder.append(Component.text(template.substring(index)));
                break;
            }

            // Text before placeholder
            if (start > index) {
                builder.append(Component.text(template.substring(index, start)));
            }

            String key = template.substring(start + 1, end);
            Component replacement = placeholders.getOrDefault(key, Component.text("%" + key + "%"));

            builder.append(replacement);
            index = end + 1;
        }

        return builder.build();
    }
}
