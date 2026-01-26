package me.hasenzahn1.pvp.commands.lookup;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupFilter {

    // Filter shortcuts mapping (shortcut -> full name)
    private static final Map<String, String> SHORTCUTS = Map.of(
            "u", "user",
            "a", "attacker",
            "c", "cause",
            "w", "world",
            "r", "radius",
            "m", "mode",
            "ty", "type",
            "t", "time"
    );

    // Duration pattern: 1d2h30m or 2h or 30m or 1d etc.
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?");

    // Parsed filter values
    private UUID playerUuid;
    private String attacker;
    private EntityDamageEvent.DamageCause cause;
    private String world;
    private Integer mode;
    private EntryType type;
    private Long timeThreshold;
    private Integer radius;

    private String parseError;

    private Player executingPlayer;

    public enum EntryType {
        DEATH, DAMAGE
    }

    public LookupFilter(Player executingPlayer) {
        this.executingPlayer = executingPlayer;
    }

    public boolean parse(String[] args) {
        for (String arg : args) {
            if (!parseArg(arg)) {
                return false;
            }
        }
        return true;
    }

    private boolean parseArg(String arg) {
        int colonIndex = arg.indexOf(':');
        if (colonIndex == -1) {
            parseError = "Invalid filter format: " + arg + " (expected key:value)";
            return false;
        }

        String key = arg.substring(0, colonIndex).toLowerCase();
        String value = arg.substring(colonIndex + 1);

        key = SHORTCUTS.getOrDefault(key, key);

        return switch (key) {
            case "user" -> parsePlayer(value);
            case "attacker" -> parseAttacker(value);
            case "cause" -> parseCause(value);
            case "world" -> parseWorld(value);
            case "mode" -> parseMode(value);
            case "type" -> parseType(value);
            case "time" -> parseTime(value);
            case "radius" -> parseRadius(value);
            default -> {
                parseError = "Unknown filter: " + key;
                yield false;
            }
        };
    }

    private boolean parsePlayer(String value) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(value);
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            parseError = "Player not found: " + value;
            return false;
        }
        this.playerUuid = player.getUniqueId();
        return true;
    }

    private boolean parseAttacker(String value) {
        if (value.startsWith("#")) {
            this.attacker = value.toLowerCase();
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(value);
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            parseError = "Attacker player not found: " + value;
            return false;
        }
        this.attacker = player.getUniqueId().toString();
        return true;
    }

    private boolean parseCause(String value) {
        try {
            this.cause = EntityDamageEvent.DamageCause.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            parseError = "Invalid damage cause: " + value;
            return false;
        }
    }

    private boolean parseWorld(String value) {
        this.world = value;
        return true;
    }

    private boolean parseMode(String value) {
        return switch (value.toLowerCase()) {
            case "pvp" -> {
                this.mode = 0;
                yield true;
            }
            case "peaceful" -> {
                this.mode = 1;
                yield true;
            }
            default -> {
                parseError = "Invalid mode: " + value + " (expected pvp or peaceful)";
                yield false;
            }
        };
    }

    private boolean parseType(String value) {
        return switch (value.toLowerCase()) {
            case "death" -> {
                this.type = EntryType.DEATH;
                yield true;
            }
            case "damage" -> {
                this.type = EntryType.DAMAGE;
                yield true;
            }
            default -> {
                parseError = "Invalid type: " + value + " (expected death or damage)";
                yield false;
            }
        };
    }

    private boolean parseTime(String value) {
        Matcher matcher = DURATION_PATTERN.matcher(value.toLowerCase());
        if (!matcher.matches()) {
            parseError = "Invalid duration format: " + value + " (expected format like 1d2h30m, 2h, 30m)";
            return false;
        }

        long millis = 0;
        String days = matcher.group(1);
        String hours = matcher.group(2);
        String minutes = matcher.group(3);

        if (days == null && hours == null && minutes == null) {
            parseError = "Invalid duration format: " + value + " (expected format like 1d2h30m, 2h, 30m)";
            return false;
        }

        if (days != null) {
            millis += Long.parseLong(days) * 24 * 60 * 60 * 1000;
        }
        if (hours != null) {
            millis += Long.parseLong(hours) * 60 * 60 * 1000;
        }
        if (minutes != null) {
            millis += Long.parseLong(minutes) * 60 * 1000;
        }

        this.timeThreshold = System.currentTimeMillis() - millis;
        return true;
    }

    private boolean parseRadius(String value) {
        try{
            this.radius = Integer.parseInt(value);
            if(radius > 0 && radius <= 100) {
                this.world = this.executingPlayer.getWorld().getName();
                return true;
            }

            this.radius = null;
            parseError = "Invalid radius: " + value;
        }catch (NumberFormatException e){
            parseError = "Invalid radius: " + value;
            this.radius = null;
            return false;
        }
        return false;
    }

    // Getters
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getAttacker() {
        return attacker;
    }

    public EntityDamageEvent.DamageCause getCause() {
        return cause;
    }

    public String getWorld() {
        return world;
    }

    public Integer getMode() {
        return mode;
    }

    public EntryType getType() {
        return type;
    }

    public Long getTimeThreshold() {
        return timeThreshold;
    }

    public Integer getRadius() {
        return radius;
    }

    public String getParseError() {
        return parseError;
    }

    public boolean hasFilters() {
        return playerUuid != null || attacker != null || cause != null ||
                world != null || mode != null || type != null || timeThreshold != null || radius != null;
    }

    public String getFilterSummary() {
        StringBuilder sb = new StringBuilder();
        if (playerUuid != null) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(playerUuid);
            sb.append("u:").append(p.getName()).append(" ");
        }
        if (attacker != null) {
            if (attacker.startsWith("#")) {
                sb.append("a:").append(attacker).append(" ");
            } else {
                try {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(attacker));
                    sb.append("a:").append(p.getName()).append(" ");
                } catch (Exception e) {
                    sb.append("a:").append(attacker).append(" ");
                }
            }
        }
        if (cause != null) {
            sb.append("c:").append(cause.name()).append(" ");
        }
        if (world != null) {
            sb.append("w:").append(world).append(" ");
        }
        if (mode != null) {
            sb.append("m:").append(mode == 0 ? "pvp" : "peaceful").append(" ");
        }
        if (type != null) {
            sb.append("ty:").append(type.name().toLowerCase()).append(" ");
        }
        if (timeThreshold != null) {
            long diff = System.currentTimeMillis() - timeThreshold;
            sb.append("t:").append(formatDuration(diff)).append(" ");
        }
        if (radius != null) {
            sb.append("r:").append(radius).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatDuration(long millis) {
        long minutes = millis / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        minutes = minutes % 60;
        hours = hours % 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        return sb.length() > 0 ? sb.toString() : "0m";
    }
}
