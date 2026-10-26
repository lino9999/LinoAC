# Validation report — LinoAC 0.4.0

Date: 25 October 2026. The original and static derivatives were never executed or loaded as plugins. [0.3.0](VALIDATION_0.3.0.md) and [0.2.0](VALIDATION_0.2.0.md) results remain historical; they are not tests of this artifact.

## Final artifact

- `LinoAC-0.4.0.jar`, 338,320 bytes, SHA-256 `5376acecdcdb606b44c6c2ec5db1335a04dbba0de48682594d9a0ed30e605f6e`.
- 119 project classes, all Java class version 65; 78 comment-free Java source files. Metadata identifies Lino and version 0.4.0. Dependencies remain provided, not bundled.
- 203 catalogue entries, 175 operative paths, four original no-ops and 24 missing slots. Legacy-only paths retain their applicability gates.
- Maven 3.9.11/JDK 25 `mvnw.cmd -B -ntp clean verify` passed, targeting Java 21. The JAR contains no original classes/assets or temporary probes.

## Source-model tests

This phase passed **1,475** independent Java 21 assertions:

| Suite | Passed |
| --- | ---: |
| First-placement FastPlace batches/exclusions |86|
| GhostHand material geometry/direct findings |107|
| Independent cumulative counter routes/reset/saturation |1,017|
| Timer windows, balance, packet authority and buffer modes |96|
| Immutable world snapshots, vehicle acknowledgements and conditional decay |44|
| Position-only phases, previous-floor origin, signed WEB and source age boundaries |125|

The counter suite tested the unchanged source hash before final integration; the other 458 assertions were rerun against the final compiled detector source. Live counter integration was tested separately below. These finite tests are not an original-versus-reconstruction replay or a new proof for all 175 paths.

## Actual Spigot runtime

The identical final JAR passed:

| Minecraft | Actual Spigot | Java | Scope | Passed |
| --- | --- | --- | --- | ---: |
|1.21.11|4598-Spigot-56165ca-d74c5d8|21.0.7|Protocol client, real Bukkit callbacks, upgrade, API, provider and shutdown|88/88|
|26.3|4663-Spigot-bed90fc-c4db4c3|25.0.1|Startup, API, actual TPS provider and shutdown|9/9|

Both loopback-only servers stopped gracefully through Bukkit. The full PacketEvents 2.14.0 release hash was `060087c58ec268eae7dd0eb1f17ac3bea1eaadda1693414141ff64e506091bd7`.

The 1.21.11 run covered:

- An actual 0.2.0 configuration upgrade: inherited proxy channel/experimental marker, retained custom legacy placement limit 17, startup and reload.
- GhostHand's four bed orientations, eight-glass enclosure, opening and neighboring-BEDROCK controls, unknown/below/established wire height, direct findings with buffer 100, break cancellation, API veto, cancel=false, bypass and disabled policy.
- FastPlace's first-event phase and late trigger inclusion; main/off-hand HOE, SCULK, SCAFFOLDING and cancelled-event exemptions preserving that phase.
- Rotation-only retention and exact 24/25 position-packet vehicle-age boundary.
- Accepted counter classification, explicit reset, manual/wave retention, successful automatic VL reset preserving history/buffers, cancelled punishment and cooldown behavior.
- Actual 120-packet movement bursts activating both Timer A and D; millisecond timing and actual PacketEvents/Main TPS equality and eligibility. Raw provider values slightly above twenty were capped at twenty.

A real low-level Minecraft client provided position/timing traffic. An independent consumer dispatched actual Bukkit break/place events against real blocks and inspected real API events; those break/place cases were not naturally generated client interactions. Controlled check buffers and minimum TPS values stimulated behavior; this does not certify packaged default tuning. The 26.3 run did not use a gameplay client. Versions 26.1.x and 26.2 were not retested in this phase.

## Preserved failures and corrections

The first attempt exposed a production upgrade bug: Bukkit `getString(path, explicitFallback)` bypassed resource defaults, so old configurations without proxy fields could fail startup. The actual YAML behavior was reproduced and Settings now uses inherited lookup with a null fallback. The final old-config runtime passed.

A second source comparison found that flag gating used a separate TPS estimate. It was aligned with the same PacketEvents metric used by original flag/Timer code, then the final artifact was rebuilt and both runtime scopes rerun. Earlier successful artifacts/results remain separately identified rather than relabeled as final.

Fixture-only retries retained in [the machine report](fidelity-validation.json) include an out-of-range policy maximum and Windows request-file sharing contention. The 26.3 first probe also assumed a 1.21-specific shutdown log phrase and treated a Windows OSHI/Perflib diagnostic as a plugin failure. Original failed outcomes are retained; rechecks use actual plugin errors, disable/save/exit evidence and the real provider result.

## Limits and cleanup

The complete reconstruction objective remains active. Several movement/world branches, exemption/history effects and enforcement timing still need comparison; see [FIDELITY.md](FIDELITY.md). No multiplayer accuracy, translator coverage, comparative original replay or performance benchmark is claimed. Unknown geometry is conservatively skipped instead of force-loaded or represented as original SPONGE values. The optional integrations retain separate runtime limits.

GhostHand break interception is enabled by its separate `checks.ghosthand-a.cancel` option; automatic punishments and setbacks remain off by default. Temporary harnesses/classes, clients, probes, isolated servers and worlds were removed from the workspace through Windows Recycle Bin after preserving these results. The ignored .work directory is empty. CI builds source; deleted standalone harnesses are not presented as persistent CI suites.
