<p align="center"><img src="assets/linoac-icon-128.png" width="128" height="128" alt="LinoAC Minecraft anti-cheat pixel shield"></p>

# LinoAC — Minecraft Anti-Cheat & Staff Tools

[![Build](https://github.com/lino9999/LinoAC/actions/workflows/build.yml/badge.svg)](https://github.com/lino9999/LinoAC/actions/workflows/build.yml)
[![Download](https://img.shields.io/github/v/release/lino9999/LinoAC?include_prereleases&label=download)](https://github.com/lino9999/LinoAC/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

**LinoAC by Lino** is an open-source Java Minecraft anti-cheat for **Spigot and Paper 1.21.11–26.3**. It combines packet-based combat and movement analysis, world-action checks, configurable staff alerts, persistent punishment waves and an integration API.

Version **0.6.0** completes the independent movement and world evaluators, restores Hitbox B and adds mounted movement, elytra phase budgets and acknowledged modern sulfur handling, with **175 independently implemented rule paths across 203 catalog entries**. Static inspection of the supplied artifact found 179 registered classes: 175 contain detection behavior, four are no-ops, and 24 additional configuration entries have no registered implementation. The previous count of 195 omitted eight numeric variants. [Every entry and its applicability](docs/CHECKS.md) is documented. Legacy-only rules remain protocol-gated; 175 is a code-coverage count, not 175 simultaneously active rules on modern clients.

All nonempty registered rule paths have independent evaluators. Recovered packet authority, state cadence, buffers and exemptions guide the implementation; documented repairs address defective reference behavior. See the [fidelity ledger](docs/FIDELITY.md). Field accuracy and exact original-versus-reconstruction equivalence remain unverified. Automatic punishments and setbacks are disabled by default. GhostHand can cancel enclosed-bed breaks; set `checks.ghosthand-a.cancel: false` for monitoring without that interception. See [validation](docs/VALIDATION.md) and [reconstruction evidence](docs/REVERSE_ENGINEERING.md).

## Features

- Aim and arm-swing statistics; attack sequences, reach, hitbox, criticals, bow speed and acknowledged velocity response.
- Speed, flight, jump, step, climbing, ground claims, liquids, strafe, elytra and mounted-entity checks with acknowledged attributes, phase budgets and environmental exemptions.
- Client-acknowledged sulfur contact on modern servers, separate physical/metadata gliding state and explicit vehicle packet/event authority. Optional per-check dismount actions default off.
- Accepted block-placement and mining checks, scaffold patterns, tower, bed-enclosure heuristics, automation patterns and combined-violation checks.
- Wire-level packet order, hotbar, abilities, timing, malformed movement and connection acknowledgement checks. Invalid numeric movement is discarded before Bukkit receives it.
- A startup-verified rule registry, bounded packet queues, independently configurable buffers, decay, VL thresholds, ping/TPS gates, alerts, setbacks and actions.
- Rich clickable alerts with configurable hover, severity and commands; verbose samples, category rankings, detailed profiles and acknowledgement diagnostics.
- Paginated check GUI, freeze and frozen-disconnect actions, teleport, knockback, random rotation and hotbar shuffle.
- Optional zero-damage or event-cancel hit suppression, delayed punishment with reset/logout/reload cancellation, broadcasts and persistent punishment waves with configurable spacing.
- Bounded asynchronous logs, retention, independent channel switches, configurable formats and accurate in-game page totals; persisted punishment statistics.
- LinoAC two-field proxy alert/punishment messaging, with separate automatic and manual command formats.
- Client brand notices, blocklist/allowlist, optional ghost-block resynchronization and separate HTTPS alert/punishment webhooks.
- PlaceholderAPI, Floodgate exemptions, a domain-specific mcMMO BERSERK hook, immutable API snapshots and synchronous lifecycle events. No remote licensing, downloaded code or telemetry.

## Requirements and installation

1. Use Spigot or Paper **1.21.11 through 26.3**. Folia is unsupported.
2. Use Java **21** for 1.21.11 and Java **25** for 26.x, according to your server distribution.
3. Install [PacketEvents 2.14.0](https://github.com/retrooper/packetevents/releases/tag/v2.14.0) in `plugins/`.
4. Download **LinoAC-0.6.0.jar** from [GitHub Releases](https://github.com/lino9999/LinoAC/releases), place it in `plugins/`, then restart.
5. Review `plugins/LinoAC/config.yml` and run `/lac status`. Start with monitoring and evaluate your own mechanics before enabling enforcement.

The same Java 21 plugin JAR serves the full target range. [VALIDATION.md](docs/VALIDATION.md) distinguishes startup/command, protocol-client and synthetic evaluator tests. Other forks, client translators and custom mechanics need their own validation.

## Commands and aliases

Main command: `/linoac`. Aliases: `/lac`, `/linoanticheat`. Permission names below use the `linoac.` prefix.

| Command after `/linoac` | Purpose | Permission |
| --- | --- | --- |
| `help` | Show permitted commands and syntax | `status` |
| `status` | Version, server TPS, check count and enforcement mode | `status` |
| `alerts` | Toggle persistent clickable alerts | `alerts` |
| `verbose` | Toggle unbuffered diagnostic samples for this session | `verbose` |
| `profile <player>` | Client, ping, eligible swing CPS, frozen state and violations | `profile` |
| `violations <player>` | List current per-check violation levels | `profile` |
| `cps <player>` | Show CPS from the latest twenty eligible swing intervals | `profile` |
| `connection <player>` | Show protocol, ping, pending acknowledgements and movement interval | `profile` |
| `top [total|combat|movement|player]` | Show the ten online players ranked by the selected category | `profile` |
| `checks` | List all check IDs and enabled states | `status` |
| `describe <check>` | Explain a check | `status` |
| `menu [page]` | Open the check inventory; toggling requires `manage` | `status` |
| `enable <check>` / `disable <check>` | Save a check's enabled state and reload | `manage` |
| `reload` | Validate and replace the active configuration | `manage` |
| `reset <player\|all>` | Clear online violations and buffers | `manage` |
| `freeze <player>` | Toggle movement, block-action and inventory-click freeze | `freeze` |
| `teleport <player>` | Teleport yourself to an online player | `teleport` |
| `ban <player>` | Run the configured manual punishment commands | `punish` |
| `wave add <player>` | Add an online player to the persistent queue | `wave` |
| `wave remove <player>` | Remove a queued player, including offline players | `wave` |
| `wave list` | List queued names | `wave` |
| `wave execute` | Start processing the queue | `wave` |
| `logs <player> [page]` | Read retained violation records | `logs` |
| `punishlogs <player> [page]` | Read retained punishment records | `logs` |
| `knockback <player>` | Apply a small staff test velocity | `test` |
| `rotate <player>` | Randomize an online player's yaw and pitch | `test` |
| `shuffle <player>` | Randomize the nine hotbar slots | `test` |
| `testalert [player]` | Test local alerts and both configured webhooks without enforcement | `test` |

| Shortcut | Aliases | Equivalent |
| --- | --- | --- |
| `/lacalerts` | `/lalerts` | `/linoac alerts` |
| `/lacwave` | `/lwave` | `/linoac wave` |
| `/laclogs` | `/llogs` | `/linoac logs` |
| `/lacpunishments` | `/lpunishlogs` | `/linoac punishlogs` |

Player parameters require an online player except for log lookup and wave removal. Commands requiring a sender location, alert preferences or a GUI require a player sender. Console can use `testalert <player>`. There are no old command or permission aliases.

## Permissions

`linoac.admin` grants all administrative permissions listed above and defaults to operators. Individual staff permissions also default to operators. `linoac.bypass` suppresses findings and **defaults to false**, including for operators. Never grant administrative permissions to ordinary players. An explicitly assigned `linoac.bypass.<check-id>` suppresses findings for that check. Packet history and buffers continue through flag-only gates; invalid protocol values are still discarded.

## Configuration

`checks.boatfly-a.dismount`, `checks.boatfly-b.dismount`, `checks.entityflight-a.dismount`, `checks.entityspeed-a.dismount` and `checks.nosaddle-a.dismount` are independent options under `checks`; all default to false. An accepted finding schedules dismount only for the same player session and mount. Reset, logout and accepted reload cancel pending actions.

`checks.airplace-a.cancel` controls placement cancellation. `checks.hitbox-b.maximum-angle` defaults to 45 degrees. Per-check speed margins, flight ground-confirmation exclusions and shared movement exemptions are independently configurable.

Missing textual keys inherit shipped defaults, including during upgrades from earlier configurations. `checks.fastplace-a.max-blocks` inherits an explicitly customized legacy `limits.maximum-placements` value when absent.

The [default configuration](src/main/resources/config.yml) is the authoritative reference. `checks.defaults` contains shared policy values; `checks.<id>` overrides individual fields. This avoids repeating the same policy 203 times. Unavailable catalog slots cannot be enabled by configuration, command or API. Version 1 configurations migrate to canonical variant IDs with a `config-v1-backup.yml` backup.

| Section | Controls |
| --- | --- |
| `settings` | Global flag limits, scoped join exclusions, reset enable/interval, console alerts, delayed automatic enforcement, hit suppression and statistics |
| `limits` | Shared reach, packet/placement/bridge rates, mining timing, elytra speed and combined-VL thresholds |
| `checks.defaults`, `checks.<id>` | Enable, buffer, decay, buffer multiplier, VL/action thresholds, per-check ping/TPS, alert/webhook intervals and command list |
| `alerts` | Format, hover, click action, custom console commands, severity and optional maximum-VL suppression |
| `integrations` | Optional mcMMO BERSERK mining exemption and position-packet-aged crowding exemption for scoped checks |
| `connection` | Actual outgoing ping ledger, pending/timeout/ping limits and flood handling |
| `ghost-blocks` | Optional repeated-ground-mismatch block resynchronization and optional safe-location setback |
| `logs` | Separate channel switches, message formats and daily file size limit |
| `brands` | Resolution, staff/console notices, ignored substrings, blocklist and allowlist |
| `freeze` | Console commands on a frozen player's disconnect |
| `punishments`, `wave` | Manual commands, queue routing, interval, entry spacing and lifecycle broadcasts |
| `proxy` | Outgoing channel, alert delimiter and general alert/automatic-punishment switch |
| `webhook` | HTTPS endpoints, notification switches and presentation |
| `messages` | Prefix and configurable lifecycle messages; `&` colors supported |

Join exclusion uses `settings.join-check-wait-time` milliseconds or fewer than `settings.min-ticks-existed` position-containing packets, only for the recovered consumers. `settings.cinematic` enables the scoped cinematic estimate exemption; the estimator still collects its inputs while disabled. The optional entity-crowding integration defaults off; its age and player vehicle-grace/web ages advance only on position-containing packets. Mounted pose and contact clocks advance on VEHICLE_MOVE. On Spigot/Paper, crowding uses the cached Bukkit nearby-entity count. `grace-ticks` in this integration counts position packets.

Automatic commands require both `settings.automatic-punishments: true` and the check's `punishable: true`. Setbacks and optional ghost repair are independently off by default. Staff manual commands and waves remain explicit actions. Reset, reload or logout revokes pending delayed punishments. Automatic dispatch runs no earlier than the next server tick. A successful automatic dispatch clears current per-check VL while preserving cumulative history and buffers; rejected or cancelled dispatches retain VL and do not consume its cooldown. Explicit reset clears both VL and cumulative history.

Alert/action templates include `%player%`, `%uuid%`, `%check%`, `%check-family%`, `%description%`, `%vl%`, `%max-vl%`, `%ping%`, `%tps%`, `%version%`, `%client-brand%`, `%world%`, `%x%`, `%y%`, `%z%`, `%info%` and `%severity%`. Punishment commands support `%player%`, `%uuid%`, `%check%`. Commands execute as console. Clicked commands execute as the viewing staff member with normal permission checks. Brand strings are client-reported and untrusted.

Reload validates YAML and numeric ranges before replacing active settings. Failed reloads preserve the current settings. Successful reloads reset detector history while retaining the collector's independent timing state. Logs use ISO timestamps, UUIDs and names, a 4,096-job queue, configurable daily size cap and retention; in-game pages show eight records within the configured recent limit. `state.yml` persists alerts/wave entries; `statistics.yml` persists dispatched-punishment counts. Violation, freeze and verbose states are session-only. Statistics count accepted command dispatches, not independently confirmed bans.

Wave entries are removed and saved before command dispatch. A crash between save and dispatch can skip an entry; failed or cancelled dispatches require review and requeueing. A cancelled wave-entry event preserves its persistent queue entry. Wave storage is a moderation queue, not a transactional ban database.

## Integrations and API

PlaceholderAPI exposes `%linoac_violations%`, `%linoac_violations_combat%`, `%linoac_violations_movement%`, `%linoac_violations_player%`, `%linoac_cps%`, `%linoac_ping%`, `%linoac_brand%`, `%linoac_client%`, `%linoac_uuid%`, `%linoac_frozen%`, `%linoac_alerts%`, `%linoac_transaction_ping%`, `%linoac_pending_transactions%`, `%linoac_transaction_age_ms%`, `%linoac_movement_speed%`, `%linoac_mouse_step_yaw%`, `%linoac_mouse_step_pitch%`, `%linoac_tps%`, `%linoac_version%` and `%linoac_total_punishments%`. Aliases also expose `%linoac_total_violations%`, `%linoac_combat_violations%`, `%linoac_movement_violations%`, `%linoac_player_violations%` and `%linoac_client_brand%`. Player fields are immutable online snapshots; unobserved measurements say `unknown`. CPS measures arm swings. Mouse steps are estimates.

Webhooks accept separate alert/punishment HTTPS URLs with a common fallback. Delivery has a bounded 32-job queue, three-second timeouts, mention suppression and independent rate limits. Manual `testalert` can test both channels. Delivery is best effort. Floodgate recognition can exempt Bedrock players from detection.

Proxy messaging uses two modified-UTF fields on `linoac:proxy`, with delimiter `#LINOAC#`. A receiver must explicitly implement this LinoAC framing. Existing custom channel settings remain configurable; fresh installations use only LinoAC identifiers. `Bungee:` punishment commands route through the online subject player; automatic commands append check metadata, while manual commands send the plain command and remain explicit even when `proxy.enabled` is false. Wave commands run locally as console. A compatible proxy receiver/channel subscription is required; accepted dispatch is not proof of receiver execution.

`LinoAPI` is registered with Bukkit's ServicesManager. It exposes immutable profiles, policies and check state, alert/freeze/reset controls, safe setbacks, explicit flags, wave operations and punishment statistics. See [API contracts and events](docs/API.md). Bukkit-facing calls require the server main thread. The new namespace intentionally replaces the original API.

## Build and maintenance

Use `./mvnw -B -ntp clean verify` or `mvnw.cmd -B -ntp clean verify` with JDK 21+. Output: `target/LinoAC-0.6.0.jar`. PacketEvents and PlaceholderAPI are provided dependencies and are not bundled.

Read [docs/AGENTS.md](docs/AGENTS.md) for a compact source map, [CHECKS.md](docs/CHECKS.md) for rule coverage and [SpigotMC BBCode](docs/spigot/) for ready-to-use resource text. Temporary test fixtures are removed after validation as requested; CI builds source and does not rerun the deleted local harnesses. Source and new assets use the MIT license; dependency and original-artifact rights remain separate.
