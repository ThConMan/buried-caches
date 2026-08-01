# BuriedCaches

A Paper plugin that hides treasure in the act of mining itself. While players dig
through natural stone and ore, each block carries a small chance of uncovering a
**Buried Cache** — a barrel filled from a vanilla loot table, locked to its finder
for a short window, with rarity tiers that escalate from a quiet chime to a
server-wide announcement.

Inspired by a buried-treasure datapack, rebuilt as a plugin with proper
anti-farm protection and persistence.

**Documentation:** https://thconman.github.io/buried-caches/

## Features

- **Natural discovery** — caches roll only on natural, survival-mined stone and
  ore below a configurable height. No structures, no maps, no commands needed.
- **Four rarity tiers** — Common, Rare, Epic, and Legendary by default, each
  pulling from a different vanilla loot table (`abandoned_mineshaft`,
  `buried_treasure`, `ancient_city`, `end_city_treasure`), fully configurable.
- **Escalating celebrations** — common finds chime for the finder; Epic and
  Legendary finds play world-audible effects and broadcast to the whole server.
- **Pity timer** — a guaranteed find after a configurable number of eligible
  blocks, so an unlucky player is never unlucky forever.
- **Anti-farm by design** — player-placed blocks are recorded in chunk
  persistent data and never count, surviving restarts, pistons, and explosions.
  A cooldown stops hammers and fast miners from chaining finds.
- **Finder ownership** — the barrel is locked to its finder for a configurable
  window: no opening, breaking, hoppering, or exploding it out from under them.
- **Lifetime stats** — per-player find counts by tier, stored in player
  persistent data, viewable by anyone with `/buriedcaches stats`.

## Installation

1. Download `buried-caches-<version>.jar` (or build it — see below).
2. Drop it into your server's `plugins/` folder.
3. Restart the server. The default `config.yml` is created on first run.

Requires Paper (or a fork) matching the `api-version` in `plugin.yml`.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/buriedcaches stats [player]` | `buriedcaches.stats` (default: everyone) | Lifetime finds by tier. Viewing others requires admin. |
| `/buriedcaches status [player]` | `buriedcaches.admin` (default: op) | Blocks since last find, cooldown, forced flag. |
| `/buriedcaches force <player>` | `buriedcaches.admin` | Guarantee a find on the next eligible block. |
| `/buriedcaches reset <player>` | `buriedcaches.admin` | Reset roll progress. |
| `/buriedcaches reload` | `buriedcaches.admin` | Reload the configuration. |

Alias: `/bcaches`

## Configuration

See the [documentation site](https://thconman.github.io/buried-caches/) for the
full reference. Highlights from the default config:

```yaml
trigger:
  minimum-blocks: 40        # no rolls before this many eligible blocks
  chance-per-block: 0.004   # ~1 in 250 after the minimum
  pity-after-blocks: 450    # guaranteed find at this count
  cooldown-seconds: 240     # minimum time between finds
  maximum-y: 64             # keeps the mechanic underground

tiers:
  legendary:
    weight: 2
    loot-table: minecraft:chests/end_city_treasure
    celebration: MYTHIC     # STANDARD | GRAND | MYTHIC
    broadcast: true         # announce to the whole server
```

## Building

```
./gradlew build
```

The jar lands in `build/libs/`. Requires JDK 25 (Gradle resolves the toolchain
automatically).

## License

MIT
