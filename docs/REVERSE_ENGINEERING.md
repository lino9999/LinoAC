# Reconstruction evidence and scope

Analysis date:25 October 2026. Supplied reference artifact, SHA-256 `0560adb0045b123c081caf606accbece66c4c8898a47af1d970ad9a2530177ed`. The supplied artifact and static transformations were inspected as data and never executed, loaded as plugins or redistributed.

## Inventory and corrected count

REA recorded 924 archive occurrences: 836 files and 88 directories. Direct constant-pool parsing recovered 822 classes without parser failures. Configuration was independently parsed structurally and with BukkitYamlConfiguration: **203 check entries across 41 families**, including eight numeric BadPackets variants. The earlier 195 count matched only lettered keys and was incorrect.

The exact static manager registry contains 179 annotated check classes matching 203 configured entries. Twenty-four configuration entries have no registered detector. Four registered callbacks—velocity-c, badpackets-d, badpackets-v and badpackets-w—are empty/no-op. LinoAC therefore implements 175 independent executable paths and retains all 203 catalogue entries for explicit coverage. Unavailable entries cannot be enabled. [CHECKS.md](CHECKS.md) lists every entry and protocol applicability.

REA inventory evidence: `ev_de951f2dbb8f9998d71dcd76c5021f9587171d5980bb636c4f4a170abb34aff9`. Inspection:`ev_6336ae951f0abea8deeb6ebabe93fa51b24d923f8c46f0e387baa5d4205606b8`; summary:`ev_1b78f9ddf6ff3be5d1e9b41b1c44f29cff13ba83dd4b387bdc8c57aef4fedbb3`. The exported evidence bundle remains local in ignored `.analysis/rea-evidence.json`. Counts derived by offline parsers are supplementary evidence rather than REA runtime observations.

## Static recovery method

Original javap instruction listings were compared with CFR 0.152 and Vineflower 1.12 reconstruction. A separate static-only transformation removed 15,824 identity/rethrow exception-handler edges across 375 classes to expose otherwise opaque control flow. It did not decrypt or execute the target. Transformation provenance and hashes are retained locally; derived JAR SHA-256:`345605e83b04eb52e5746b9932ff47a482d16b8ce7861b805d8b8f92f6802c93`.

Observations include actual registration, annotation metadata, numeric predicates, packet/event consumers, per-check settings, original no-op bodies, domain-specific exemptions and public API signatures. Independent implementation decisions include modern input collection, collision/attribute budgets, conservative velocity intervals, bounded state and deliberate API cancellation contracts. These decisions must not be described as branch-perfect recovery of opaque code.

## Implemented modules

| Module | Executable paths | Implemented observation boundary |
| --- | --- | --- |
| Combat |61| Actual rotation/attack/swing/use/release sequences, bounded statistics, server boxes, outgoing velocity and later known acknowledgements |
| Movement |51| Wire movement plus public attributes, loaded server collision geometry, environment and vehicle telemetry |
| Player/packets |37| Raw abilities/slots/coordinates/order, exact acknowledgement ledger, timer budget and authoritative ground state |
| World/combinations |26| Source-specific accepted/cancelled Bukkit callbacks, wire rotations/dig cancellation and actual category violation totals |

Every executable ID has a synthetic positive trace, with clean, threshold and exemption regressions. That establishes finite evaluator behavior, not collector correctness for every possible packet history, field accuracy or equivalent original tuning. Runtime tests are reported separately in [VALIDATION.md](VALIDATION.md).

NoSlowA's recovered predicate requires both pre-1.9 server and client; it intentionally cannot run on the supported modern range. Other legacy attack-order rules require a legacy client. Protocol translation was not certified. Available 175 is therefore implementation coverage, not a simultaneous modern-client detection count.

Recovered defects were corrected rather than reproduced: an unincremented dig counter, an unreset swing latch, an impossible ground modulo predicate, block-reference comparison where coordinates were intended and Scaffold A's mismatched interaction dispatcher. Conservative geometry/velocity bounds can skip evaluations when physics is unknown. Server geometry and packet/tick timing are not a replicated per-client world. Cinematic camera state is estimated through the recovered packet-history transitions and calibration rather than read directly from the client. Original exemption/history branches remain partly unresolved; see FIDELITY.md.

## Staff and integration reconstruction

LinoAC supplies renamed commands/permissions/aliases, rich alerts, policy inheritance, profiles/rankings, check GUI, freeze/logout actions, optional hit suppression, delayed enforcement cancellation, persistent waves, configurable logs, statistics, separate HTTPS notifications, ghost-block resynchronization and a renamed immutable API with documented events. The concrete optional mcMMO BERSERK event hook exempts FastBreak only. Declarations with empty or unrecovered hook bodies were not converted into guessed global bypasses.

API binary compatibility with the old namespace is intentionally absent. The target platforms are Spigot/Paper; Folia's region ownership model remains unsupported. Changing a descriptor flag would not establish safe Folia compatibility.

## Security and residual unknowns

The original startup contains a premium-license HTTP request; bundled metrics contains a bStats URL. Command dispatch helpers also exist. These facts alone do not establish malware. No confirmed backdoor or prompt injection was identified in inspected plaintext and recovered bodies; encrypted strings and unresolved control flow prevent a whole-artifact safety certification. The reconstruction contains no original class files, encrypted constants, assets, licensing, telemetry or downloaded executable code.

There was no original-versus-reconstruction runtime replay, multiplayer field accuracy study or comparative performance benchmark. Complete original behavioral equivalence is unresolved. The MIT license applies only to newly written source and project assets; the original artifact and analysis output are excluded from Git and release assets.

## Further fidelity recovery in 0.3.0

A separate static-only handler normalization removed 15,822 identity/rethrow handlers for focused recovery. Trusted class-file readers and JCE operations decoded 982 strings as data; no original class or static initializer was loaded. This recovered click exclusions, sensitivity calibration, flag ordering and proxy protocol framing. Numeric heuristics retain their recovered definitions even where names resemble conventional statistics. LinoAC implements its own proxy channel `linoac:proxy` and delimiter `#LINOAC#`. Commands, permissions, configuration layout, API and implementation namespace are independently owned by LinoAC.

New source-derived helper tests, actual network probes and exact artifact identity are reported in VALIDATION.md. Remaining branches and intentional differences are tracked in FIDELITY.md; this additional recovery does not turn implementation counts into a complete-equivalence claim.

## Further source recovery in 0.4.0

FastPlace batch initialization/cadence, GhostHand material-enclosure callbacks and local cancellation, Timer A/D windows/balance, seven cumulative-counter lifetimes and exact position-processor phases were recovered from instruction/data evidence. Previous all-movement age and mirrored/Bukkit union assumptions are explicitly superseded: supported non-Folia servers select the Bukkit snapshot; source ages advance only on position-containing packets, and WEB uses signed vertical motion. PositionState keeps those phases separate from read-only snapshots.

The source default counter branch admits only Invalid A/B; LinoAC deliberately repairs that behavior using the recovered category/family routes. GhostHand repairs adjacent BEDROCK pairing and respects the cancellable API. Automatic dispatch retains counters/buffers and clears VL only after accepted dispatch, avoiding original empty/failed/cancelled reset behavior. A new implementation regression in inherited text defaults and a separate TPS-estimate mismatch were reproduced and fixed before the final 0.4.0 artifact. Actual upgrade, callback, network and provider outcomes remain separate from static equivalence claims.

## Independent behavioral reconstruction in 0.5.0

Static findings describe observable inputs, state transitions, exemptions, finding modes and event authority. They are implemented in independent observation collectors, immutable records, bounded acknowledgement ledgers, shared tri-state exemptions and modular rule evaluators. Decompiled classes are reference evidence; they are not translated class by class or included in the build. The configuration keeps LinoAC's shared `checks.defaults` inheritance and canonical IDs instead of reproducing the reference layout.

This phase recovers sequence-based combat, target-relative position history, two-stage impulse acknowledgement, finite velocity candidate prediction, packet-order latches, invalid-motion budgets, position-aged material contacts and Bukkit event authority for mining/projectiles. Matching a finite predicate or formula does not establish complete detector equivalence. The fidelity ledger records the remaining movement/world and integration work.

The reference's Hitbox B target field is never assigned by its recovered callback path; this rule remains dormant. NoSlow A requires both a pre-1.9 server and client, so it is inactive throughout the supported server range. These facts are distinct from unavailable configuration slots.
