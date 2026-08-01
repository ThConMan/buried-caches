package dev.whitl.buriedcaches;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BuriedCachesCommand implements CommandExecutor, TabCompleter {

    private final BuriedCachesPlugin plugin;
    private final PlayerProgressStore progress;

    public BuriedCachesCommand(BuriedCachesPlugin plugin, PlayerProgressStore progress) {
        this.plugin = plugin;
        this.progress = progress;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("stats")) {
            if (!sender.hasPermission("buriedcaches.stats")) {
                sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                return true;
            }
            Player target = args.length >= 2 && sender.hasPermission("buriedcaches.admin")
                    ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player player ? player : null);
            if (target == null) {
                sender.sendMessage(Component.text("Choose an online player.", NamedTextColor.RED));
                return true;
            }
            showStats(sender, target);
            return true;
        }

        if (!sender.hasPermission("buriedcaches.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(Component.text("BuriedCaches configuration reloaded.", NamedTextColor.GREEN));
            return true;
        }

        if (args.length >= 1 && List.of("status", "force", "reset").contains(args[0].toLowerCase(Locale.ROOT))) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
            } else {
                target = sender instanceof Player player ? player : null;
            }
            if (target == null) {
                sender.sendMessage(Component.text("Choose an online player.", NamedTextColor.RED));
                return true;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "force" -> {
                    progress.forceNext(target);
                    sender.sendMessage(Component.text(target.getName()
                            + " will find treasure on their next eligible natural block.", NamedTextColor.GREEN));
                }
                case "reset" -> {
                    progress.reset(target);
                    sender.sendMessage(Component.text("Reset BuriedCaches progress for "
                            + target.getName() + ".", NamedTextColor.GREEN));
                }
                default -> showStatus(sender, target);
            }
            return true;
        }

        sender.sendMessage(Component.text("/buriedcaches stats [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/buriedcaches status [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/buriedcaches force <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/buriedcaches reset <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/buriedcaches reload", NamedTextColor.YELLOW));
        return true;
    }

    private void showStats(CommandSender sender, Player target) {
        sender.sendMessage(Component.text("Buried cache finds for " + target.getName() + ":",
                NamedTextColor.GOLD));
        for (TreasureTier tier : plugin.lootService().tiers()) {
            sender.sendMessage(Component.text("  " + tier.displayName() + ": ", tier.color())
                    .append(Component.text(progress.tierFinds(target, tier.id()),
                            NamedTextColor.WHITE)));
        }
        sender.sendMessage(Component.text("  Total: ", NamedTextColor.GOLD)
                .append(Component.text(progress.totalFinds(target), NamedTextColor.WHITE)));
    }

    private void showStatus(CommandSender sender, Player target) {
        TreasureProgress state = progress.get(target);
        long elapsed = Math.max(0L, System.currentTimeMillis() - state.lastTreasureMillis());
        long cooldown = plugin.triggerPolicy().cooldownMillis();
        long remaining = state.lastTreasureMillis() <= 0 ? 0 : Math.max(0L, cooldown - elapsed);
        sender.sendMessage(Component.text("BuriedCaches status for " + target.getName() + ":",
                NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Eligible blocks since treasure: " + state.blocksSinceTreasure(),
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Cooldown remaining: "
                + Duration.ofMillis(remaining).toSeconds() + " seconds", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Forced next: " + state.forced(), NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> values = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("buriedcaches.admin")) {
                values.addAll(List.of("stats", "status", "force", "reset", "reload"));
            } else if (sender.hasPermission("buriedcaches.stats")) {
                values.add("stats");
            }
        } else if (args.length == 2 && sender.hasPermission("buriedcaches.admin")
                && List.of("stats", "status", "force", "reset")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            Bukkit.getOnlinePlayers().forEach(player -> values.add(player.getName()));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
