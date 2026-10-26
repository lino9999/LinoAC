# Validation report — LinoAC 0.3.0

Date: 25 October 2026. These results concern the exact 0.3.0 artifact below. [0.2.0 results](VALIDATION_0.2.0.md) are historical and are not reused as fresh tests. The supplied original and static derivatives were never executed.

## Artifact and build

- JAR: `LinoAC-0.3.0.jar`, 317,530 bytes.
- SHA-256: `76c01ce3353b9d36a1115434f50b9aa2a1811b954d59ac18f253093842f5f8f5`.
- 109 project classes, all Java class version 65; 74 Java source files without comments. Metadata identifies Lino, version 0.3.0 and only the LinoAC class namespace.
- 203 catalogue entries and 175 startup-verified operative paths. Four original no-ops and 24 missing slots remain unavailable. NoSlow A remains legacy-server/client gated.
- `mvnw.cmd -B -ntp clean verify` passed using Maven 3.9.11 and JDK 25, targeting Java 21. PacketEvents 2.14.0 and optional PlaceholderAPI are provided dependencies. No original code/assets or temporary probes are packaged.

## Fresh isolated behavior tests

Standalone harnesses ran against the final compiled source on Java 21:

| Suite | Passed assertions |
| --- | ---: |
| Twenty click rules, windows and scores |2,038|
| Click exclusions and shared timing |85|
| Cinematic/sensitivity/entity state and Aim predicates |395|
| Global eligibility, scoped consumers and Aim finding modes |425|
| Placement chain, bridge geometry and callback boundaries |75|
| Proxy codec, carrier, limits and lifecycle |45|
| Telemetry, acknowledgements and buffer modes |48|
| Combined findings and cooldown boundaries |35|
| Independently recovered calibration bit comparisons |201|

Total: **3,347**. These finite tests cover clean, suspicious, threshold and missing-input cases. They are not a replay of the original. Historical coverage of all 175 labels does not prove that every original branch now matches; [FIDELITY.md](FIDELITY.md) lists the remaining work.

## Actual Spigot and network results

The identical JAR passed **40 plugin probes** across four isolated servers:

| Minecraft | Actual Spigot implementation | Java | Startup, registry, commands and reload |
| --- | --- | --- | --- |
|1.21.11|4598-Spigot-56165ca-d74c5d8|21.0.7|Pass|
|26.1.1|dev-Spigot-4a90bec-d74c5d8|25.0.1|Pass|
|26.1.2|4632-Spigot-566f972-690a402|25.0.1|Pass|
|26.3|4663-Spigot-bed90fc-c4db4c3|25.0.1|Pass|

Each server reported 175 operative paths and all 203 catalogue entries, exercised version/status, aliases, check listing/description and reload, then stopped with exit code 0. The 26.3 host logged OSHI/JNA Windows Perflib diagnostics before plugin startup; four raw duplicated diagnostic lines remain in the machine report. No LinoAC/PacketEvents exception was observed. Minecraft 26.2 was not available in the installed cache and was not retested for 0.3.0; its 0.2.0 result remains historical.

An actual minecraft-protocol 1.68.0 client on loopback Spigot 1.21.11 passed **60/60** network/consumer assertions, including:

- Nine actual movement observations, three rotations and three position-containing packets, with source markers and received timestamps.
- Strict/direct buffer modes, multiplication after overshoot, cancellation and reset through the live violation pipeline.
- 180 actual swings activating Autoclicker B, a live dispersion finding and matching shared CPS/observation/placeholder-provider values.
- Held-firework exclusion, FIFO spawn/destroy visibility, rejection of out-of-order PONG without consuming state, actual outgoing velocity and acknowledged teleport ages 1 then 2.
- Actual subscribed-client delivery of two-UTF proxy packets with twelve alert fields, automatic command metadata and plain manual payload. Disabling the general switch suppressed alert/automatic delivery while the explicit manual route remained available.

The controlled fixture used minimum TPS 1 and explicit low check buffers to stimulate behavior; this is not default tuning certification. The temporary consumer wrapped the real collector callback while retaining the engine callback. Proxy bytes were verified at the subscribed Minecraft client; no Bungee/Velocity receiver was supplied.

## Corrections and remaining limits

Temporary fixture errors were retained: relative control-path resolution, invalid zero TPS/cooldown settings and a missing channel subscription. The first extended run passed 53/57 because the unsubscribed client received no proxy packets. After correcting the client, the identical final JAR passed 60/60. These outcomes are preserved separately in [the machine-readable report](fidelity-validation-0.3.0.json).

The 26.x tests establish startup/commands, not gameplay certification. There was no human multiplayer accuracy study, translator matrix, comparative original replay or performance benchmark. Real Floodgate/mcMMO environments, complete per-branch history ordering, original cumulative counters and several movement/world cores remain unverified. The optional ghost repair retains its local death/grace guard; that final guard was inspected and included in the rebuilt tested artifact. Automatic punishments and setbacks remain off by default.

Temporary harness sources/classes, clients, probes, isolated servers and worlds were removed from the workspace through Windows Recycle Bin after recording these results. The ignored .work directory is empty. Standard caches/runtimes, the user's original artifact, ignored analysis evidence and release assets are retained. CI builds the source; deleted standalone harnesses are not presented as persistent CI tests.

Later source audits in 0.4.0 supersede the all-movement/union-source age assumptions in this historical report. See the current fidelity ledger for the explicit corrections.
