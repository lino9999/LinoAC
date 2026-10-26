# Validation report

Date: 25 October 2026. Release: LinoAC 0.2.0. The supplied original and static transformations were never executed.

## Tested artifact

- JAR: `LinoAC-0.2.0.jar`, 254,186 bytes.
- SHA-256: `643e67cedf9dbd66c1aecd8af843be7712e43b04b289227ba962a048272117f6`.
- 79 project classes, all Java class version 65 (Java 21), all in the LinoAC namespace. Metadata identifies author Lino and version 0.2.0. Java source contains no comments.
- 203 configured catalogue entries; the startup-verified registry owns 175 executable paths. Missing and original no-op slots remain unavailable.
- PacketEvents 2.14.0 and PlaceholderAPI are provided dependencies, not bundled classes. No original code/assets are packaged.

## Build and synthetic behavior

`mvnw.cmd -B -ntp clean verify` passed with Maven 3.9.11, JDK 25 and compiler release 21. Temporary standalone Java harnesses passed:

| Suite | Assertions | Positive path coverage |
| --- | --- | --- |
| Combat |9|61 IDs|
| Movement |73|51 IDs|
| Player/protocol |12|37 IDs|
| World/combinations |21|26 IDs|
| Velocity candidate model |15,008|500 trajectories and acknowledgement boundaries|
| Packet buffer |13|64-bit IDs, ordering, duplicate/unknown tokens, bounded concurrent queue and immutable snapshots|

Together the four rule suites exercise all 175 implemented paths. They include clean traffic, thresholds, exemptions, timer jitter, valid physical movement, placement authority, cooldowns and regression cases. Direct observation traces establish evaluator behavior, not field accuracy or exact original equivalence. Legacy-only positive traces do not mean those paths can run on supported modern servers.

## Actual Spigot matrix

The identical release JAR was copied to isolated loopback-only servers with PacketEvents 2.14.0. Every server reached ready state, enabled LinoAC, exercised commands and shut down cleanly. Optional PlaceholderAPI/Floodgate/mcMMO were absent in this matrix.

| Minecraft | Actual Spigot implementation | Java | Startup/commands/reload |
| --- | --- | --- | --- |
|1.21.11|4598-Spigot-56165ca-d74c5d8|21|Pass|
|26.1.1|dev-Spigot-4a90bec-d74c5d8, pinned archived source refs|25|Pass|
|26.1.2|4632-Spigot-566f972-690a402|25|Pass|
|26.2|4637-Spigot-8db49a2-f5fed68|25|Pass|
|26.3|4663-Spigot-bed90fc-c4db4c3|25|Pass|

Runs covered plugin listing/status, check listing/description, aliases/shortcuts, console/player restrictions, missing-player errors, wave/log commands, enable/disable, reload, reset and invalid input. No LinoAC startup, task or event errors were recorded. Version 26.1.1 was built from pinned historical official-source commits; it was not silently replaced with 26.1.2. Server and dependency hashes are in [the machine-readable report](validation-results.json).

## Protocol client, API and HTTPS

A temporary offline-mode minecraft-protocol 1.68.0 client connected only to isolated Spigot 1.21.11. The final artifact passed 83 assertion executions (79 distinct names across repeated wave phases), including 50 API assertions. The primary run passed 81/83; two final flag assertions were correctly blocked by the actual TPS eligibility gate during a burst of fixture configuration writes. A targeted 7/7 recheck waited for actual eligibility and verified cancelled/accepted flags and post-event ordering on the identical JAR. Original primary outcomes are preserved in the report rather than presented as one all-green run.

Covered scenarios:

- Login, unauthorized commands, idle/ordinary ground movement and fence support without violations; staff profile, GUI, freeze correction and monitoring without automatic punishment.
- Synthetic speed, invalid hotbar 30, pitch 100, NaN position and sustained timer packets during repeated damage/item use. Invalid values were cancelled without corrupting history or disconnecting the client.
- Violation audit entries, queue persistence/removal, invalid reload rejection and continued operation.
- Independent API consumer: service registration, immutable profiles/policies/measurements, main-thread enforcement, alert/freeze/check/reset/setback cancellation, violation/post-event ordering, punishment/log/webhook cancellation, delayed-task cancellation, wave lifecycle/manual removal, hit suppression, rotation/shuffle and statistics.
- Real v1 configuration migration retained customized buffer/maxVL values and an unrelated extension key, and produced an untouched backup. This test found and fixed default inheritance validation before the final run.
- Real loopback HTTPS delivery tested distinct endpoints, JSON escaping, mention suppression, rich context, cancellation and refusal to follow redirects. A process-local trust store was used; global trust was unchanged.

The isolated functional fixture sets minimum TPS 18 for timer-a, badpackets-q and the API speed-a test, allowing controlled stimulation during host load. Packaged defaults were tested separately in the five-server matrix. These are synthetic stimuli, not a cheat-client certification or default-tuning study.

## PlaceholderAPI and statistics restart

Actual PlaceholderAPI 2.11.7 loaded alongside the final JAR on Spigot 1.21.11. A separate run passed 33/33 assertions, including actual expansion registration, 25 canonical/alias resolutions, alias equivalence and a fresh-server statistics reload. Persisted punishment total 2 was recovered with new session total 0, and the global punishment placeholder returned 2. Provider hash is recorded in the machine-readable report.

## Practical limits and cleanup

There was no public multiplayer field trial, translator matrix, large-player load test, comparative original replay or performance benchmark. The 26.x matrix establishes startup/commands/reload, not full gameplay certification. The installed protocol-client data did not support 26.3, so no 26.3 gameplay result is claimed. Previous 0.1.0 Paper results are not presented as tests of this artifact.

Real Floodgate/mcMMO installation behavior, client-brand and ghost-block event runtime cancellation, complete custom-mechanics compatibility and crash-boundary wave recovery remain untested. Their implemented wiring was inspected; mcMMO's domain-specific exemption also has a pure regression. Folia remains unsupported. Opaque original exemptions and cinematic camera state remain reconstruction limits. Automatic punishments and setbacks are off by default.

Temporary harness sources/classes, test clients, worlds, loopback certificates/trust stores and isolated BuildTools fixtures are removed after preserving these factual results. Standard Maven/npm caches, installed runtimes and the user's existing server are retained. CI builds source; it does not rerun deleted local harnesses. The original artifact and local analysis evidence remain ignored and are excluded from releases.
