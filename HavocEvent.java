package id.havoc.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;

public final class HavocEvent extends JavaPlugin {
    private boolean eventRunning;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("hv") != null) getCommand("hv").setExecutor(this);
        getLogger().info("HavocEvent enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("havocevent.admin")) {
            sender.sendMessage(color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(color("&eUsage: /hv <start|reload>"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> startEvent(sender);
            case "reload" -> {
                reloadConfig();
                sender.sendMessage(color("&aHavocEvent config reloaded."));
            }
            default -> sender.sendMessage(color("&eUsage: /hv <start|reload>"));
        }
        return true;
    }

    private void startEvent(CommandSender sender) {
        if (eventRunning) {
            sender.sendMessage(color("&cAn event sequence is already running."));
            return;
        }

        World world = getConfiguredWorld();
        if (getConfig().getBoolean("worldborder.enabled", true) && world == null) {
            sender.sendMessage(color("&cWorldBorder world not found. Check worldborder.world in config.yml."));
            return;
        }

        eventRunning = true;
        sender.sendMessage(color("&aHavoc event sequence started."));

        if (!getConfig().getBoolean("countdown.enabled", true)) {
            announceAndExpand();
            return;
        }

        List<Integer> numbers = getConfig().getIntegerList("countdown.numbers");
        if (numbers.isEmpty()) numbers = List.of(3, 2, 1);
        final List<Integer> countdown = List.copyOf(numbers);
        long intervalTicks = Math.max(1L, Math.round(getConfig().getDouble("countdown.interval", 1.0) * 20.0));

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= countdown.size()) {
                    cancel();
                    announceAndExpand();
                    return;
                }
                int number = countdown.get(index++);
                String title = getConfig().getString("countdown.title.title", "&c&l{number}").replace("{number}", String.valueOf(number));
                String subtitle = getConfig().getString("countdown.title.subtitle", "");
                showTitle(title, subtitle,
                        getConfig().getInt("countdown.title.fade-in", 0),
                        getConfig().getInt("countdown.title.stay", 20),
                        getConfig().getInt("countdown.title.fade-out", 0));
                playCountdownSound(number);
            }
        }.runTaskTimer(this, 0L, intervalTicks);
    }

    private void announceAndExpand() {
        showTitle(getConfig().getString("announcement.title", "&6&lHAVOC EVENT"),
                getConfig().getString("announcement.subtitle", "&fTHE EVENT HAS STARTED!"),
                getConfig().getInt("announcement.fade-in", 10),
                getConfig().getInt("announcement.stay", 60),
                getConfig().getInt("announcement.fade-out", 20));

        if (getConfig().getBoolean("announcement.sound.enabled", true)) {
            playSound(getConfig().getString("announcement.sound.type", "UI_TOAST_CHALLENGE_COMPLETE"),
                    (float) getConfig().getDouble("announcement.sound.volume", 1.0),
                    (float) getConfig().getDouble("announcement.sound.pitch", 1.0));
        }
        startWorldBorderExpansion();
        if (!getConfig().getBoolean("worldborder.enabled", true)) eventRunning = false;
    }

    private void startWorldBorderExpansion() {
        if (!getConfig().getBoolean("worldborder.enabled", true)) return;
        World world = getConfiguredWorld();
        if (world == null) {
            getLogger().warning("WorldBorder world not found; expansion skipped.");
            eventRunning = false;
            return;
        }

        double start = Math.max(1.0, getConfig().getDouble("worldborder.start-size", 100.0));
        double end = Math.max(1.0, getConfig().getDouble("worldborder.end-size", 1000.0));
        double centerX = getConfig().getDouble("worldborder.center-x", 0.0);
        double centerZ = getConfig().getDouble("worldborder.center-z", 0.0);
        int duration = Math.max(1, getConfig().getInt("worldborder.duration", 300));
        int interval = Math.max(1, getConfig().getInt("worldborder.interval", 1));

        WorldBorder border = world.getWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(start);

        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                elapsed = Math.min(duration, elapsed + interval);
                double progress = (double) elapsed / duration;
                double currentSize = start + ((end - start) * progress);
                border.setSize(currentSize);
                if (elapsed >= duration) {
                    border.setSize(end);
                    cancel();
                    eventRunning = false;
                    Bukkit.broadcastMessage(color("&a[Havoc] &fWorldBorder expansion completed."));
                }
            }
        }.runTaskTimer(this, 0L, interval * 20L);
    }

    private World getConfiguredWorld() {
        String worldName = getConfig().getString("worldborder.world", "world");
        return Bukkit.getWorld(worldName == null ? "world" : worldName);
    }

    private void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String safeTitle = title == null ? "" : title;
        String safeSubtitle = subtitle == null ? "" : subtitle;
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(color(safeTitle), color(safeSubtitle), Math.max(0, fadeIn), Math.max(0, stay), Math.max(0, fadeOut));
        }
    }

    private void playCountdownSound(int number) {
        if (!getConfig().getBoolean("countdown.sound.enabled", true)) return;
        String path = switch (number) {
            case 3 -> "countdown.sound.three";
            case 2 -> "countdown.sound.two";
            case 1 -> "countdown.sound.one";
            default -> "countdown.sound.other";
        };
        playSound(getConfig().getString(path, "BLOCK_NOTE_BLOCK_HAT"),
                (float) getConfig().getDouble("countdown.sound.volume", 1.0),
                (float) getConfig().getDouble("countdown.sound.pitch", 1.0));
    }

    private void playSound(String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isBlank()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Unknown sound in config.yml: " + soundName);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
