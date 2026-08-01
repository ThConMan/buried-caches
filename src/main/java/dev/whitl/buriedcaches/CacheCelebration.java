package dev.whitl.buriedcaches;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * The effect bundle a tier plays when its cache appears. STANDARD is heard only
 * by the finder; GRAND and MYTHIC play world sounds so nearby players share it.
 */
public enum CacheCelebration {
    STANDARD,
    GRAND,
    MYTHIC;

    public static CacheCelebration fromConfig(String raw) {
        if (raw == null) {
            return STANDARD;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return STANDARD;
        }
    }

    public void play(Player player, Location location) {
        Location center = location.clone().add(0.5, 0.7, 0.5);
        switch (this) {
            case STANDARD -> {
                player.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.25f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, center, 18, 0.35, 0.35, 0.35, 0.0);
            }
            case GRAND -> {
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                location.getWorld().playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.1f);
                location.getWorld().spawnParticle(Particle.ENCHANT, center, 60, 0.5, 0.6, 0.5, 0.5);
                location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 24, 0.4, 0.4, 0.4, 0.0);
            }
            case MYTHIC -> {
                location.getWorld().playSound(location, Sound.ITEM_TOTEM_USE, 0.7f, 1.2f);
                location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.9f);
                location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 40, 0.4, 0.5, 0.4, 0.25);
                location.getWorld().spawnParticle(Particle.END_ROD, center, 30, 0.4, 0.5, 0.4, 0.05);
            }
        }
    }
}
