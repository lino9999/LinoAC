# LinoAC integration API

Add LinoAC as a provided dependency and declare `depend: [LinoAC]` or `softdepend: [LinoAC]` in your plugin descriptor. Obtain the service after LinoAC has enabled:

```java
LinoAPI api = Bukkit.getServicesManager().load(LinoAPI.class);
```

The namespace is `io.github.lino9999.linoac.api`. `LinoAPI.get()` is a convenience lookup that throws if the service is unavailable. API consumers must compile against the LinoAC version they run with; internal services are not a stable extension contract.

## Facade

All state queries and mutations require the server main thread and an enabled LinoAC instance. Off-thread calls throw `IllegalStateException`. `version()` and `catalogue()` expose immutable metadata. Schedule callbacks with the Bukkit scheduler before accessing player state.

| Method | Result |
| --- | --- |
| `catalogue()` | Immutable definitions containing ID, family, category, description and executable availability. Unavailable historical slots remain explicit. |
| `profile(UUID)`, `onlineProfiles()` | Immutable snapshots of actual ping, reported brand, protocol version, arm swings per second, buffers, violations, category totals and staff settings. An offline UUID has no profile. |
| `measurements(UUID)` | Optional immutable collector measurement map. Keys are present only when that measurement is available; missing data is not a measured zero. Offline UUIDs return an empty optional. |
| `policy(CheckType)` | Current effective check policy, including thresholds, ping/TPS gates, intervals and configured actions. |
| `check(UUID, CheckType)` | Optional online-player snapshot combining definition, policy, VL and buffer. |
| `setFrozen(UUID, boolean, actor)`, `setAlerts(UUID, boolean)` | Event-aware mutations; return false when rejected or cancelled. Alert settings are persisted. |
| `setVerbose(UUID, boolean)` | Change the online player's verbose subscription. |
| `reset(UUID, actor)`, `resetAll(actor)` | Event-aware reset of buffers, violations and detector memory. Accepted resets cancel pending automatic punishment tasks. |
| `setback(UUID, Location, CheckType)` | Event-aware teleport to a finite location in the same world and an already loaded chunk. |
| `queueWave(UUID)`, `removeWaveEntry(UUID)`, `waveQueue()`, `startWave()` | Persist and manage punishment wave entries. Adding requires an online player; removing and inspecting persisted UUIDs does not. The queue snapshot is immutable. |
| `flag(UUID, checkId, detail)` | Submit a finding through the normal eligibility, policy, buffer, event and action pipeline. It does not force a violation or bypass permissions, grace periods, ping/TPS gates or cooldowns. Unknown or unavailable checks are rejected. |
| `statistics()` | Command-dispatch batch counts: current session, persisted total and the last seven UTC calendar days. Persisted values are optional when their history cannot be read reliably. A successful dispatch does not prove a ban or kick succeeded. |

Profile maps, policy command lists, catalogue lists, wave maps and measurement maps cannot be modified through their returned snapshots. Mutations requiring an online UUID throw `IllegalArgumentException` if it is offline. Nonfinite setback coordinates also throw `IllegalArgumentException`; rejected worlds, unloaded chunks and cancelled setbacks return false. Client brand text is supplied by the client and does not establish client authenticity. Sensitivity and statistical estimates require sufficient observations and may be absent.

## Events

Events run synchronously on the main thread. Text and numeric payloads describe the event's recorded state; command lists and entry maps are immutable, and locations are defensively copied. `LinoViolationEvent.getPlayer()` exposes the live Bukkit player and therefore also requires main-thread use. Cancellation prevents the named pending action; it does not undo earlier independent actions.

| Event | Cancellable | Emission and effect |
| --- | --- | --- |
| `LinoPlayerRegisterEvent` | No | After player state and packet tracking have been registered. |
| `LinoClientBrandEvent` | Yes | Before accepting a changed reported brand. |
| `LinoViolationEvent` | Yes | After a check reaches its buffer threshold, before storing the next VL, logging it or executing its actions. Cancellation retains the pre-existing VL. |
| `LinoAlertEvent` | Yes | Before local staff chat delivery, after proxy relay. Cancellation affects staff chat only; accepted VL, history, console, commands, webhooks and punishment remain independent. |
| `LinoPostViolationEvent` | No | After an accepted violation has completed its synchronous action pipeline. A delayed punishment may still be pending. |
| `LinoAlertToggleEvent` | Yes | Before changing and persisting an alert subscription. |
| `LinoFreezeEvent` | Yes | Before freezing or unfreezing an online player; inspect `isFrozen()` for the requested state. |
| `LinoCheckToggleEvent` | Yes | Before a staff command or menu persists a check's enabled state. Unavailable checks cannot be enabled. |
| `LinoResetEvent` | Yes | Before reset and delayed-task cancellation. `isGlobal()` distinguishes global resets; the player UUID is optional. |
| `LinoSetbackEvent` | Yes | Before an accepted target location is used for a setback teleport. |
| `LinoGhostBlockEvent` | Yes | Before repair of the recorded block position and the associated correction. |
| `LinoPunishmentEvent` | Yes | Before dispatching a manual, wave or automatic punishment command batch. |
| `LinoPunishmentLogEvent` | Yes | Before writing one punishment command's audit entry. Cancelling the log does not cancel an already dispatched command. |
| `LinoWebhookPunishmentEvent` | Yes | Before submitting a punishment notification to the bounded HTTP worker. Cancellation does not undo the punishment. |
| `LinoWaveStartEvent` | Yes | Before starting an immutable snapshot of the persisted queue. Cancelling leaves entries queued. |
| `LinoWaveEntryEvent` | Yes | Before executing an entry's command batch. Cancelling skips this run and retains the persisted entry for a future wave. |
| `LinoWaveEndEvent` | No | At wave completion, including removal of the last active entry. Reports attempted entries, command batches successfully dispatched and elapsed duration. |

Use Bukkit `@EventHandler` listeners to subscribe. Listeners should complete quickly and schedule disk or network work separately. Reentrant mutations can change subsequent state; consumers should query a fresh snapshot after an action rather than treating an older snapshot as live data.

## 0.4.0 state and enforcement details

Profiles retain current per-check VL and its sum. Measurement snapshots additionally expose `cumulative-violations-total`, `-combat`, `-movement`, `-player`, `-autoclicker`, `-scaffold` and `-timer`. These independent counters exclude experimental/combined findings. Timer counts remain separate from total/player; autoclicker counts remain separate from combat. Successful automatic command dispatch clears current VL, preserving buffers and cumulative history. Manual/wave dispatch does not perform that reset. Explicit reset clears all counters and buffers and cancels pending actions.

GhostHand break interception follows an accepted violation/post-event pair; cancelling `LinoViolationEvent` prevents that interception. `checks.ghosthand-a.cancel: false` retains findings without cancelling the break. Server-world snapshots contain cached material facts from actual Bukkit XYZ movement; client position packets advance source ages, while rotation-only packets do not. The legacy `entity-cram-movement-age` measurement is a position-packet count; `entity-cram-position-age` is its explicit alias. Flag TPS, timer TPS and reported TPS use the same PacketEvents-derived metric, capped at twenty.

## 0.5.0 state and enforcement details

Ready findings apply their buffer multiplier before policy, ping/TPS, bypass, automatic-punishment cooldown and violation-event gates. `LinoViolationEvent` cancellation therefore preserves existing VL while retaining the submitted buffer transition. Reach A and Hitbox A decay empty eligible target history by exactly 0.025 rather than the general configured decay.

Successful automatic command dispatch starts a player-wide monotonic cooldown (`settings.automatic-punishment-cooldown-ms`, default 10000). It suppresses accepted findings and another automatic dispatch across checks. Empty, rejected, cancelled or failed command batches do not consume that cooldown. Delayed automatic actions use per-player ownership tickets; accepted reload, reset, logout and disable cancel them, including stale callbacks whose scheduler cancellation is delayed. Rejected reload keeps the active configuration and pending actions. Explicit reset also clears the cooldown.

Fresh hit suppression sets damage to zero. Enable `settings.cancel-hits-cancel-event` for event cancellation. A pre-0.5 configuration with hit suppression already enabled and no explicit mode preserves cancellation on upgrade. Freezing still cancels attacks.

`source-profile-complete` is zero after a packet-state queue overflow. Stateful evaluators then stop using unprovable retained history; a small set of stateless malformed-input checks remains active. Reconnect creates a fresh protocol profile. Resetting violations does not restore lost transport state. If connection flood kicking is disabled, incomplete state remains conservative instead of silently claiming detection coverage. Unknown physical geometry remains absent. Proven constructor exemption values are separate from actual world samples.
