package bpn.velocity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ColorUtils {

    public static Component translateColorCodes(String message) {
        String translated = message.replaceAll("§([a-fA-F0-9kKlLnNoOrR])", "§$1§r");
        String[] parts = translated.split("§");

        Component builder = Component.text("");

        for (String part : parts) {
            if (part.length() >= 2) {
                char code = part.charAt(1);
                NamedTextColor color = getColorFromCode(code);
                if (color != null) {
                    builder.append(Component.text(part.substring(2)).color(color));
                    continue;
                }
            }
            builder.append(Component.text("§" + part));
        }

        return builder;
    }

    private static NamedTextColor getColorFromCode(char code) {
        switch (code) {
            case '0':
                return NamedTextColor.BLACK;
            case '1':
                return NamedTextColor.DARK_BLUE;
            case '2':
                return NamedTextColor.DARK_GREEN;
            case '3':
                return NamedTextColor.DARK_AQUA;
            case '4':
                return NamedTextColor.DARK_RED;
            case '5':
                return NamedTextColor.DARK_PURPLE;
            case '6':
                return NamedTextColor.GOLD;
            case '7':
                return NamedTextColor.GRAY;
            case '8':
                return NamedTextColor.DARK_GRAY;
            case '9':
                return NamedTextColor.BLUE;
            case 'a':
                return NamedTextColor.GREEN;
            case 'b':
                return NamedTextColor.AQUA;
            case 'c':
                return NamedTextColor.RED;
            case 'd':
                return NamedTextColor.LIGHT_PURPLE;
            case 'e':
                return NamedTextColor.YELLOW;
            case 'f':
                return NamedTextColor.WHITE;
            default:
                return null;
        }
    }
}
