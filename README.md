# PvpSystem

A Paper plugin for tracking and managing PvP and PvE combat events. Logs all deaths and damage with detailed information and provides an interactive lookup interface to search, inspect, and restore past events.

## Features

- Logs all player deaths and damage events with attacker, cause, location, and inventory
- Advanced lookup system with combinable filters and tab completion
- Interactive paginated UI with clickable teleport, info, inventory view, and inventory restore actions
- Per-player death history command
- Peaceful mode toggle to flag non-PvP players
- Configurable cooldown preventing players from switching to peaceful mode after PvP combat
- Inventory viewing and restoration from death records
- Action triggers to automatically run commands on PvP events involving peaceful players
- Configurable custom menu buttons in the lookup info view
- PlaceholderAPI support (`%pvpsystem_isPeaceful%`)
- Fully customizable messages via `config.yml`
- SQLite database storage

## Requirements

- Paper 1.21.1+
- Java 21+
- (Optional) [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholder support

## Commands

| Command         | Description                           | Usage                                 |
|-----------------|---------------------------------------|---------------------------------------|
| `/pvplookup`    | Search deaths and damage with filters | `/pvplookup [filters...]`             |
| `/deathhistory` | View a player's death history         | `/deathhistory <player> [filters...]` |
| `/peaceful`     | Toggle peaceful mode                  | `/peaceful [player] <on/off>`         |

`/peaceful` can also be used as `/friedlich`.

## Permissions

| Permission                                | Description                                          |
|-------------------------------------------|------------------------------------------------------|
| `pvpsystem.commands.peaceful`             | Use `/peaceful` on yourself                          |
| `pvpsystem.commands.peaceful.other`       | Set peaceful mode for other players                  |
| `pvpsystem.commands.lookup`               | Use `/pvplookup`                                     |
| `pvpsystem.commands.lookup.teleport`      | Use the teleport button in the lookup UI             |
| `pvpsystem.commands.lookup.info`          | View detailed info in the lookup UI                  |
| `pvpsystem.commands.lookup.view`          | View death inventories in the lookup UI              |
| `pvpsystem.commands.lookup.reset`         | Restore inventories from the lookup UI               |
| `pvpsystem.commands.lookup.deathhistory`  | Use `/deathhistory`                                  |

## Filter Reference

Filters are passed as arguments in `key:value` format. Each filter has a short and long name. Multiple filters can be combined in any order.

| Short  | Long        | Description                      | Values                                            |
|--------|-------------|----------------------------------|---------------------------------------------------|
| `u:`   | `user:`     | Filter by victim player          | Player name                                       |
| `a:`   | `attacker:` | Filter by attacker               | Player name or `#entity` (see below)              |
| `c:`   | `cause:`    | Filter by damage cause           | Bukkit DamageCause (e.g. `entity_attack`, `fall`) |
| `w:`   | `world:`    | Filter by world                  | World name                                        |
| `r:`   | `radius:`   | Filter by radius around you      | `1`-`100` (blocks)                                |
| `m:`   | `mode:`     | Filter by defender mode          | `pvp` or `peaceful`                               |
| `ty:`  | `type:`     | Filter by entry type             | `death` or `damage`                               |
| `t:`   | `time:`     | Time window size                 | Duration (e.g. `30m`, `2h`, `1d2h30m`)            |
| `af:`  | `after:`    | Offset time window into the past | Duration (e.g. `1h`, `7d`)                        |

### Time Filtering

- `t:1h` - entries from the last hour
- `af:2h` - entries older than 2 hours
- `af:2h t:30m` - entries from 2h30m ago to 2h ago (a 30-minute window starting 2 hours in the past)

### Special Attacker Values

When filtering by attacker, you can use `#` prefixed values for non-player sources:

`#lava`, `#fire`, `#tnt`, `#end_crystal`, `#creeper`, `#explosion`, `#fall`, `#drowning`, `#suffocation`, `#starvation`, `#poison`, `#magic`, `#void`, `#falling_block`, `#projectile`, `#lightning`, `#wither`, `#dragon_breath`, `#kinetic`, `#magma_block`, `#cramming`, `#dryout`, `#freeze`, `#sonic_boom`

Mob types also use this format (e.g. `#zombie`, `#skeleton`).

## Examples

```
/pvplookup u:Steve t:1d
```
All deaths and damage for Steve in the last day.

```
/pvplookup a:#creeper ty:death t:7d
```
All deaths caused by creepers in the last week.

```
/pvplookup af:2h t:30m w:world
```
Events in the overworld from 2h30m ago to 2h ago.

```
/pvplookup m:peaceful r:50
```
Events involving peaceful-mode players within 50 blocks of you.

```
/deathhistory Steve t:1d
```
Steve's deaths in the last day.

## Action Triggers

Action triggers let the server automatically run commands when PvP combat involving a peaceful-mode player is detected. This is useful for integrating with third-party plugins such as replay recorders.

When a damage or death event occurs where at least one of the involved players has peaceful mode enabled, the plugin:

1. Generates a unique **trigger key** for that combat pair.
2. Runs all configured `activateCommands` as console (e.g. starting a replay recording).
3. Keeps the trigger alive as long as new combat events between the same pair keep occurring.
4. Once no new events have occurred for the configured `delaySinceLastEvent` duration, runs all `deactivateCommands` as console (e.g. stopping the recording).

The trigger key is stored with every logged damage and death entry, and is visible in the `[Info]` hover tooltip in the lookup UI. It can also be used as a placeholder in custom menu buttons (see below).

### Action Trigger Settings

| Key                                 | Description                                                                 | Default |
|-------------------------------------|-----------------------------------------------------------------------------|---------|
| `actionTrigger.enabled`             | Whether action triggers are active                                          | `true`  |
| `actionTrigger.activateCommands`    | List of commands run (as console) when a trigger starts                     | --      |
| `actionTrigger.deactivateCommands`  | List of commands run (as console) when a trigger expires                    | --      |
| `actionTrigger.delaySinceLastEvent` | Time in milliseconds after the last combat event before the trigger expires | `15000` |

Commands support the `%triggerKey%` placeholder along with all other standard event placeholders (see placeholder table below).

## Custom Menu Buttons

The buttons shown in the detailed info view of the lookup UI (accessed by clicking `[Info]` on a lookup entry) can be fully customized and extended through `config.yml`. Buttons are defined separately for death entries under `deathEntryButtons` and for damage entries under `damageEntryButtons`.

Each button is defined as a named key (e.g. `replay`, `openinv`) under the respective section, with the following properties:

| Property           | Description                                                                                              |
|--------------------|----------------------------------------------------------------------------------------------------------|
| `displayCondition` | When to show the button: `ALL` (always) or `HAS_TRIGGER` (only when the entry has an action trigger key) |
| `display`          | The button label shown in chat (supports `&` color codes and placeholders)                               |
| `hover`            | Tooltip text shown when hovering over the button (supports `&` color codes and placeholders)             |
| `command`          | The command the clicking player will execute (supports placeholders)                                     |

You can add as many buttons as you want. To remove a default button, simply delete its entry from the config.

### Button Permissions

The button name determines its required permission. A button named `replay` requires the permission `pvpsystem.commands.lookup.replay`. A button named `openinv` requires `pvpsystem.commands.lookup.openinv`. Players without the matching permission will not see the button.

The built-in `[View]` and `[Reset]` buttons for death entries are not affected by this system and continue to use their existing permissions (`pvpsystem.commands.lookup.view` and `pvpsystem.commands.lookup.reset`).

## Placeholders

The following placeholders can be used in action trigger commands, custom menu button templates, and info hover text:

| Placeholder        | Description                                                          |
|--------------------|----------------------------------------------------------------------|
| `%uuid%`           | UUID of the victim                                                   |
| `%defender%`       | Name of the victim (color-coded by mode)                             |
| `%attacker%`       | Name of the attacker (color-coded by mode)                           |
| `%cause%`          | Damage cause (e.g. `ENTITY_ATTACK`, `FALL`)                          |
| `%attackerHealth%` | Attacker's health at the time of the event                           |
| `%defenderHealth%` | Victim's health at the time of the event                             |
| `%timestamp%`      | Formatted timestamp of the event                                     |
| `%world%`          | World name where the event occurred                                  |
| `%x%`              | X coordinate of the event                                            |
| `%y%`              | Y coordinate of the event                                            |
| `%z%`              | Z coordinate of the event                                            |
| `%levels%`         | Experience levels lost (death entries only)                          |
| `%damage%`         | Final damage dealt (damage entries only)                             |
| `%originalDamage%` | Original damage before reduction (damage entries only)               |
| `%itemsInInv%`     | Number of item stacks in the victim's inventory (death entries only) |
| `%triggerKey%`     | Unique action trigger key for the combat pair (empty if no trigger)  |

## Peaceful Mode Cooldown

After a player engages in PvP combat, they are prevented from switching to peaceful mode for a configurable duration. This stops players from abusing peaceful mode to escape combat.

- The cooldown only applies when a player tries to enable peaceful mode on **themselves** -- admins setting another player's mode are not affected.
- The cooldown timer starts on PvP actions (dealing or receiving PvP damage).
- Configurable via `peacefulCommandCooldownAfterPvp` in `config.yml` (value in milliseconds, default `60000` = 60 seconds).
- When a player tries to enable peaceful mode while the cooldown is active, they receive a configurable denial message (`commands.peaceful.cooldown` in `config.yml`).

## PlaceholderAPI

If [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed, the following placeholder is available:

| Placeholder                | Description                                                         |
|----------------------------|---------------------------------------------------------------------|
| `%pvpsystem_isPeaceful%`   | Returns `true` if the player is in peaceful mode, `false` otherwise |

## Configuration

The `config.yml` contains the following settings:

| Key                               | Description                                                 | Default                     |
|-----------------------------------|-------------------------------------------------------------|-----------------------------|
| `prefix`                          | Chat message prefix                                         | `&7[&3Pvp&1System&7]: &f`   |
| `joinMessageDuration`             | Ticks to display join mode message                          | `100`                       |
| `damageThreshold`                 | Damage event queue size before flushing to DB               | `6`                         |
| `damageBelowThreshold`            | Minimum damage entries to keep in queue                     | `1`                         |
| `lastDamageDuration`              | Duration (ms) to track last damage source                   | `2000`                      |
| `peacefulCommandCooldownAfterPvp` | Cooldown (ms) before peaceful mode can be enabled after PvP | `60000`                     |

All chat messages, UI text, hover text, and button labels can be customized under the `lang` section using `&` color codes and `%placeholder%` variables.
