# LinoAC 0.5.0 — Combat, Protocol and Enforcement Reconstruction

LinoAC by Lino adds independent raw-input combat state, acknowledged velocity prediction, target-relative history, packet-order latches and event-specific mining authority. The catalogue remains 203 entries and 175 implemented rule paths. Full reconstruction remains in progress; this is a prerelease.

## Changes

- Sequence combat uses recovered clock, batch, exemption and finding-mode contracts. Reach/Hitbox use bounded relative-target history; velocity uses two-stage PING acknowledgements and finite motion candidates.
- Packet rules preserve version gates and frame latches. Invalid-motion budgets consume acknowledged effects and position-aged environmental facts. Unknown or lost state does not become a fabricated clean value.
- FastBreak observes SURVIVAL interaction/start timing and LOWEST break authority. FastBow observes eligible arrow launches, including cancelled callbacks, with crossbow and multishot exclusions.
- Automatic punishment has a player-wide monotonic cooldown, ownership-protected delayed tasks and cancellation on accepted reload/reset/logout/disable. Cancelled, empty or failed command batches do not consume cooldown.
- `LinoAlertEvent` can veto local staff chat independently of accepted VL and other actions. Fresh hit suppression sets zero damage; an explicit mode selects event cancellation and preserves existing enabled configurations on upgrade.
- Proxy defaults are independently named `linoac:proxy` and `#LINOAC#`. Configure a compatible LinoAC receiver. No reference branding, classes or assets are packaged.

## Installation

Use Spigot/Paper 1.21.11–26.3 with Java 21 for 1.21.11 or Java 25 for 26.x. Install the full [PacketEvents 2.14.0 release](https://github.com/retrooper/packetevents/releases/tag/v2.14.0), replace the plugin JAR and restart. Review `/lac status` and the configuration. Automatic punishment and setbacks remain disabled by default; GhostHand break cancellation has its separate setting.

## Verification and limits

The final artifact passed 3,861 finite source-model assertions. Actual Spigot 1.21.11 passed 103/103 runtime assertions and Spigot 26.3 passed 106/106 with two protocol clients. Exact hashes, failed attempts and cleanup are recorded in [VALIDATION.md](https://github.com/lino9999/LinoAC/blob/main/docs/VALIDATION.md). [FIDELITY.md](https://github.com/lino9999/LinoAC/blob/main/docs/FIDELITY.md) records unfinished movement/world and integration scopes. No field accuracy, whole-plugin equivalence or performance certification is claimed.

JAR SHA-256: `d461e0ba70214786b9aaa08f11135f6ea93848cdf75e5a9b782acdd323325828`.
