# Validation report — LinoAC 0.5.0

Date: 26 October 2026. The supplied reference artifact and static derivatives were never executed or loaded as plugins. Earlier version reports remain historical: [0.4.0](VALIDATION_0.4.0.md), [0.3.0](VALIDATION_0.3.0.md), [0.2.0](VALIDATION_0.2.0.md).

## Final artifact

- `LinoAC-0.5.0.jar`, 448,289 bytes, SHA-256 `d461e0ba70214786b9aaa08f11135f6ea93848cdf75e5a9b782acdd323325828`.
- 160 project classes, all Java class version 65; 96 comment-free Java source files. Author Lino; version 0.5.0. Dependencies remain provided.
- Static archive audit found no identical reference members, reference brand literals, foreign class namespaces, temporary probes or identical long configuration lines. None of 757 strings recovered in 14 local static ledgers matched a compiled UTF-8 constant longer than forty characters; common API/material identifiers are outside that authored-message comparison. This bounded audit does not prove legal permission or complete behavior equivalence.
- Maven 3.9.11/JDK 25 `mvnw.cmd -B -ntp clean verify` passed, targeting Java 21. Maven compilation itself has no permanent test suite; the temporary suites below ran separately.

## Source-model tests

All nine temporary suites were recompiled against the final compiled production classes with `javac --release 21` and executed on Java 25.0.1: **3,861 assertions passed, zero failed**.

| Suite | Passed |
| --- | ---: |
| Raw combat clocks, finding modes and activation |115|
| Combat tri-state exemptions |18|
| Material predicates |36|
| Velocity acknowledgement/history and geometry |77|
| Finite velocity candidates |28|
| Cooldown, buffers, alert selection and pending ownership |2,652|
| Protocol frame latches, gates and ground claims |59|
| Invalid-motion budgets and strict boundaries |74|
| World/event/ability producers, transport loss and constructor knowledge |802|

The geometry suite contains two aggregate assertions over 256 independently generated Spigot Vector cases; these are not inflated into 256 assertions. The target-boat tests distinguish proven initial false, actual contact, and unknown state after geometry loss. These finite traces are not original-versus-reconstruction replays, field accuracy tests or proof of all 175 rule paths.

## Actual Spigot runtime

The identical final artifact passed **103/103** checks on Spigot 1.21.11 (`4598-Spigot-56165ca-d74c5d8`), Java 21.0.7. The loopback server stopped gracefully, saved all three dimensions and reported no plugin exceptions.

The run covered real packet clients, relative-target attacks, Reach/Hitbox suspicious and clean controls, PING/velocity acknowledgement stages, cooldowns, API veto, delayed-action cancellation, successful/failed reload, hit suppression modes, mining and projectile authority. An independent consumer dispatched real Bukkit mining/projectile/damage callbacks; those events were not all generated naturally by client gameplay. Controlled check buffers, cooldowns and TPS configuration isolate the paths; this does not certify default tuning.

The same JAR also passed **106/106** checks on actual Spigot 26.3 (`4663-Spigot-bed90fc-c4db4c3`), Java 25.0.1. Two offline protocol-777 clients completed login, movement, attacks, relative-target history and PING/velocity acknowledgement tests, plus the shared API/enforcement/Bukkit-callback suite. Both profiles reported complete source state. No client decoder errors or plugin exceptions were observed; the server exited through its normal shutdown path. It emitted save lines for overworld, nether and end at shutdown; no independent chunk-integrity claim is made.

The 26.3 client used official [MCProtocolLib](https://github.com/GeyserMC/MCProtocolLib), version 26.3, artifact SHA-256 `c4ff8bd92ca84240b0ec772e09e4a1d9f6d756b03a9d923bbbad894b17c38a96`, matching the repository checksum. It is a protocol fixture, not a vanilla physics client. A missing ClientTickEnd packet in an early fixture caused vanilla movement rejection; adding the required tick marker resolved it without changing LinoAC. A separate initial whitelist rejection was also a fixture configuration failure. Failed attempts remain identified in local evidence. Across the two final runs, **209/209** runtime assertions passed.

The full PacketEvents 2.14.0 runtime hash is `060087c58ec268eae7dd0eb1f17ac3bea1eaadda1693414141ff64e506091bd7`.

## Failed attempts and corrections

Candidate01 exposed a genuine producer prerequisite bug: Hitbox A unnecessarily required LIQUID telemetry, and proven initial exemption values were unavailable before the first world cache. The final implementation separates Hitbox prerequisites and constructor knowledge from actual geometry. An additional source audit fixed the same initial BOAT state for targets without inventing clean values after geometry loss.

A fixed 350 ms Conduit wait in candidate02 ran before the real client acknowledgement and produced one failed mining exemption test. The final fixture waits for actual acknowledged Conduit state, records the active Bukkit effect, and then observes zero VL. No production change was needed for this fixture race. Earlier attempts remain in the local evidence ledger.

The first velocity fixture expected the requested Bukkit Y value 0.5; actual wire decoding was 0.5000305194408838. The corrected oracle uses captured wire velocity and confirms both acknowledgement stages. An ordinary concurrent Speed A reward interfered with an early fixed-decay probe; its isolated atomic recheck passed. The actual low-TPS provider condition was unobserved (TPS 20), so no physical low-TPS gate claim is made; pure boundary tests remain separate.

## Limits and cleanup

The complete objective remains open in [FIDELITY.md](FIDELITY.md). Remaining movement/world paths, real optional integrations, connected proxy execution, translated clients and multiplayer field accuracy require further work. Versions between the tested boundaries were not retested in this phase. Main-thread ownership and conservative unknown-state handling are deliberate safeguards, not equivalence claims.

Temporary harnesses, compiled fixtures, protocol clients, isolated servers and worlds were removed from the workspace through Windows Recycle Bin after recording these results. `.work` is empty. The ignored local evidence ledger retains reports, not executable test fixtures. CI compiles source and does not claim to run the deleted standalone suites. The [machine report](fidelity-validation-0.5.0.json) preserves exact final assertions and artifact identity.
