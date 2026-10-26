# Validation report - LinoAC 0.6.0

Date: 26 October 2026. The original and static derivatives were never executed or loaded as plugins. Historical results remain separate: [0.5.0](VALIDATION_0.5.0.md), [0.4.0](VALIDATION_0.4.0.md), [0.3.0](VALIDATION_0.3.0.md), [0.2.0](VALIDATION_0.2.0.md).

## Final artifact

- LinoAC-0.6.0.jar: **577,002 bytes**, SHA-256 **`55d46c95d32208d4b52775a1e7360df85be55cf8b87727fae3210df3d1309f32`**.
- 205 project classes, all Java class version 65; 125 comment-free Java source files. Author Lino, version 0.6.0. Dependencies remain provided.
- Maven 3.9.11/JDK 25 clean verify passed, targeting Java 21. Maven has no permanent test suite; the independent temporary suites below ran separately against the exact immutable final JAR.
- Catalogue/config coverage is 203/203, with 175 available nonempty paths. Four no-ops and 24 unregistered slots stay disabled. NoSlow A is inactive on supported servers.
- Fresh archive/source audit found no identical reference members, old branding, foreign class namespaces, temporary probes or original annotation descriptions longer than ten characters. The reference SHA remained unchanged. This is a bounded content/provenance audit, not whole-artifact legal or malware certification.

## Source-model tests

All thirteen suites were freshly compiled with javac --release 21 against the exact final JAR and executed on Java 25.0.1: **2,987 assertions passed, zero failed suites**.

| Suite | Passed |
| --- | ---: |
| Shared position, event, ability and ACK producers | 1469 |
| Sixteen position/vertical movement contracts | 225 |
| Placement windows and event authority | 139 |
| Hitbox B finite angular geometry | 41 |
| UTF-8 logs, pages, quotas and accumulation | 43 |
| Mounted predicates, subtype budgets and authority | 72 |
| Liquid, climbing and NoSlow predicates/repairs | 86 |
| Damage and soul-placement clocks | 11 |
| Glide, heading, phase and impulse budgets | 240 |
| Mounted/surface/gliding state and uncertainty | 398 |
| Vehicle-coordinate sulfur/ACK stages | 10 |
| Firework, campfire, flower pot and projectile enums | 94 |
| Swimming, submersion and nearby-solid enums | 159 |

Counts are assertions, not unique detectors or cheat categories. Producer tests include repeated continuously-unknown and consecutive-known-negative convergence sequences. Model observations are synthetic; the server evidence below is reported separately. The final dependency audit covers 106 new numeric glide/heading/budget keys and the conservative set of 129 consumed enum candidates drawn from the 139-entry reference enum inventory, with no unbound provider candidates.

## Actual runtime matrix

Every row ran the identical final SHA above. **756/756 assertions passed**: Spigot 511, Paper 245.

| Platform | Java | Passed | Scope |
| --- | --- | ---: | --- |
| Spigot 1.21.11 | 21.0.7 | 240/240 | Full native protocol and controlled Bukkit/API suite |
| Spigot 26.3 | 25.0.1 | 243/243 | Full native protocol and controlled Bukkit/API suite |
| Paper 26.3 build 223 | 25.0.1 | 245/245 | Full native protocol and controlled Bukkit/API suite |
| Spigot 26.1.1 | 25.0.1 | 10/10 | Native 775 startup/API/position profile |
| Spigot 26.1.2 | 25.0.1 | 10/10 | Native 775 startup/API/position profile |
| Spigot 26.2 | 25.0.1 | 8/8 | Startup/API/reload/status; no native 776 client |

Each full suite independently proved suspicious and clean controls for all **50 operative movement IDs** on the supported native client, plus CamelHusk subtype behavior. The remaining movement ID, NoSlow A, requires a pre-1.9 server/client and stays dormant. Full suites also cover fifteen world patterns, Reach/Hitbox A/B, velocity acknowledgement and clean A/B/D response, raw-versus-position clocks, gliding, mounted raw/Bukkit authority, water/lava/bubble contact, dismount veto/reset/mount replacement, accepted versus cancelled projectile damage, log pages/read failure and first-file quotas.

Clients send real protocol packets over loopback. The 26.3 fixtures use official MCProtocolLib protocol 777; 1.21.11 and the two 26.1 smokes use native protocol 775. Selected world actions, damage and PlayerMoveEvent cases dispatch actual Bukkit events against real players/blocks through a temporary consumer. Optional dismount guards also use the public finding API. These controlled callbacks and protocol clients do not simulate complete vanilla gameplay or establish field false-positive rates.

Temporary test policy used buffer 1, core-sixteen buffer multiplier 0, new-family/combat multiplier 1, decay 0, minimum TPS 1 and maximum ping 100000, with automatic punishments off and checks not punishable. Packaged shipping configuration was unchanged. Results therefore establish the exercised logic and authority, rather than default tuning.

Full-suite snapshot TPS minima were 19.9967 on Spigot 1.21.11, 19.9975 on Spigot 26.3 and 19.2852 on Paper; final readings were approximately 20. Paper is official 26.3 build 223, implementation name Paper. All six servers disabled the plugin and exited normally with code 0, no lifecycle/event exceptions and three dimension-saving lines. The extra all-dimensions marker is absent on modern 26.3/Paper and is not invented. No independent disk-integrity or load benchmark claim is made.

The full PacketEvents 2.14.0 runtime SHA is `060087c58ec268eae7dd0eb1f17ac3bea1eaadda1693414141ff64e506091bd7`. The official MCProtocolLib artifact SHA is `c4ff8bd92ca84240b0ec772e09e4a1d9f6d756b03a9d923bbbad894b17c38a96`.

## Failed attempts and corrections

Earlier candidates remain bound to their original hashes and are not counted as final evidence. Native tests exposed eight missing named exemption producers that the numeric-only audit missed. They were implemented and tested before the final candidate. SDK/source control review also fixed CamelHusk family selection and preserved the active inherited Llama budget for TraderLlama.

Fixture corrections remained separate: Motion A/B/C initially ran before liquid contact uncertainty had recovered; velocity incorrectly expected an acknowledged queue before the first held PONG; a mounted clean window began before two real descending packets replaced the previous hover; and Paper rejected a wall primer at an exact collision boundary, leaving prior ladder geometry cached. Final fixtures qualify actual state. Paper uses physically clear X 64.69 inside the recovered fraction band, with fresh non-climbable geometry, equal air counts and no near-ground exemption. No detection threshold was weakened to pass it. A missing fixture module and invalid early identity assertions are recorded separately in local evidence.

## Scope and cleanup

[FIDELITY.md](FIDELITY.md) records recovered behavior and deliberate repairs. All known remaining movement/world branches have independent implementations. Field accuracy, translated legacy clients, custom mechanics, live optional integrations and a connected proxy receiver retain their documented validation boundaries. Folia is unsupported.

Temporary harnesses, protocol clients, isolated servers/worlds and compiled testing files were removed through Windows Recycle Bin after preserving verification. All eleven owned temporary directories were verified inside this workspace, with no reparse points. `.work` is empty; owned ports 25860/25863 are free and no owned runtime process remains. The ignored local evidence manifest retains 36 raw report/log files, including failed attempts, and a clean final process/port audit. The [machine report](fidelity-validation-0.6.0.json) preserves exact counts, IDs, artifact identity and scope. CI compiles source and does not claim to run the removed suites.
