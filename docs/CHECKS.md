# Check catalogue and applicability

LinoAC 0.5.0 contains 203 catalogue entries, 179 registered upstream classes and 175 independently reconstructed executable rule paths. Four registered original callbacks are no-ops; 24 configured slots have no registered detector. They remain visible and disabled rather than receiving invented behavior. The original 195 count omitted eight numeric BadPackets entries.

A registered rule is evaluated only when its actual observations, protocol and exemption conditions apply. The historical 0.2.0 direct-feed suite covered all 175 paths; fresh suites and changed contracts are reported separately in VALIDATION.md. These tests do not prove cheat accuracy or equivalence to the obfuscated original. NoSlow A requires both a pre-1.9 server and client, so it intentionally cannot run on the supported modern server range. Nine additional paths require legacy clients; translators were not certified. BadPackets 7 uses pre-1.21.2 swing ordering; BadPackets 9 uses the modern client-tick-end marker.

| ID | Signal | Category | Status |
| --- | --- | --- | --- |
| `aim-a` | aim A: Slope | combat | Reconstructed |
| `aim-b` | aim B: Modulo | combat | Reconstructed |
| `aim-c` | aim C: Repeated | combat | Reconstructed |
| `aim-d` | aim D: Straight | combat | Reconstructed |
| `aim-e` | aim E: Ratio | combat | Reconstructed |
| `aim-f` | aim F: Straight | combat | Reconstructed |
| `aim-g` | aim G: Ratio | combat | Reconstructed |
| `aim-h` | aim H: Negative | combat | Reconstructed |
| `aim-i` | aim I: Constant | combat | Reconstructed |
| `aim-k` | aim K: Linear | combat | Reconstructed |
| `aim-l` | aim L: Direction | combat | Reconstructed |
| `aim-m` | aim M: Unavailable legacy configuration | combat | No registered upstream detector; disabled |
| `aim-n` | aim N: Small Yaw | combat | Reconstructed |
| `aim-o` | aim O: Small Yaw | combat | Reconstructed |
| `aim-p` | aim P: Yaw Acceleration | combat | Reconstructed |
| `aim-q` | aim Q: GCD Modulo | combat | Reconstructed |
| `aim-r` | aim R: Unavailable legacy configuration | combat | No registered upstream detector; disabled |
| `aim-s` | aim S: Divisor Y | combat | Reconstructed |
| `aim-u` | aim U: GCD Flaw | combat | Reconstructed |
| `aim-w` | aim W: Analysis | combat | Reconstructed |
| `aim-x` | aim X: Analysis | combat | Reconstructed |
| `aim-y` | aim Y: Rotation | combat | Reconstructed |
| `autoblock-a` | autoblock A: Sequence | combat | Legacy client only |
| `autoblock-b` | autoblock B: Packet | combat | Legacy client only |
| `autoblock-c` | autoblock C: Order | combat | Legacy client only |
| `autoblock-d` | autoblock D: Order | combat | Legacy client only |
| `autoclicker-a` | autoclicker A: Limit | combat | Reconstructed |
| `autoclicker-b` | autoclicker B: Deviation | combat | Reconstructed |
| `autoclicker-c` | autoclicker C: Rounded | combat | Reconstructed |
| `autoclicker-d` | autoclicker D: Skewness | combat | Reconstructed |
| `autoclicker-e` | autoclicker E: Variance | combat | Reconstructed |
| `autoclicker-f` | autoclicker F: Distinct | combat | Reconstructed |
| `autoclicker-g` | autoclicker G: Outliers | combat | Reconstructed |
| `autoclicker-h` | autoclicker H: Average Deviation | combat | Reconstructed |
| `autoclicker-i` | autoclicker I: Kurtosis | combat | Reconstructed |
| `autoclicker-j` | autoclicker J: Range | combat | Reconstructed |
| `autoclicker-k` | autoclicker K: Average Difference | combat | Reconstructed |
| `autoclicker-l` | autoclicker L: Kurtosis Difference | combat | Reconstructed |
| `autoclicker-m` | autoclicker M: Variance Difference | combat | Reconstructed |
| `autoclicker-n` | autoclicker N: Deviation Difference | combat | Reconstructed |
| `autoclicker-o` | autoclicker O: Spikes | combat | Reconstructed |
| `autoclicker-p` | autoclicker P: Identical | combat | Reconstructed |
| `autoclicker-q` | autoclicker Q: Average Deviation | combat | Reconstructed |
| `autoclicker-r` | autoclicker R: Consistency | combat | Reconstructed |
| `autoclicker-s` | autoclicker S: Distinct | combat | Reconstructed |
| `autoclicker-t` | autoclicker T: Kurtosis | combat | Reconstructed |
| `reach-a` | reach A: History | combat | Reconstructed |
| `reach-b` | reach B: Simple | combat | Reconstructed |
| `fastbow-a` | fastbow A: Limit | combat | Reconstructed |
| `criticals-a` | criticals A: Ground | combat | Reconstructed |
| `criticals-b` | criticals B: Modulo | combat | Reconstructed |
| `killaura-a` | killaura A: Post | combat | Reconstructed |
| `killaura-b` | killaura B: Acceleration | combat | Legacy client only |
| `killaura-c` | killaura C: Head Snap | combat | Reconstructed |
| `killaura-d` | killaura D: Multi Aura | combat | Reconstructed |
| `killaura-f` | killaura F: Unavailable legacy configuration | combat | No registered upstream detector; disabled |
| `killaura-h` | killaura H: Unavailable legacy configuration | combat | No registered upstream detector; disabled |
| `killaura-j` | killaura J: Frequency | combat | Reconstructed |
| `killaura-k` | killaura K: Pattern | combat | Reconstructed |
| `killaura-l` | killaura L: Strafe | combat | Legacy client only |
| `hitbox-a` | hitbox A: History | combat | Reconstructed |
| `hitbox-b` | hitbox B: Simple | combat | Reconstructed |
| `velocity-a` | velocity A: Vertical | combat | Reconstructed |
| `velocity-b` | velocity B: Horizontal | combat | Reconstructed |
| `velocity-c` | velocity C: Ignored Vertical (inert original slot) | combat | Original no-op; disabled |
| `velocity-d` | velocity D: Horizontal | combat | Reconstructed |
| `noslow-a` | noslow A: Packet | movement | Legacy server/client only; inactive on supported servers |
| `noslow-b` | noslow B: Soul Sand | movement | Reconstructed |
| `noslow-c` | noslow C: Web | movement | Reconstructed |
| `noslow-d` | noslow D: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `noslow-e` | noslow E: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `motion-a` | motion A: Constant | movement | Reconstructed |
| `motion-b` | motion B: Inverse | movement | Reconstructed |
| `motion-c` | motion C: Jump | movement | Reconstructed |
| `motion-d` | motion D: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `motion-e` | motion E: Modulo | movement | Reconstructed |
| `motion-g` | motion G: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `motion-h` | motion H: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `jump-a` | jump A: Motion | movement | Reconstructed |
| `jump-b` | jump B: Height | movement | Reconstructed |
| `jump-c` | jump C: Motion | movement | Reconstructed |
| `step-a` | step A: Vanilla | movement | Reconstructed |
| `step-b` | step B: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `step-c` | step C: Motion | movement | Reconstructed |
| `boatfly-a` | boatfly A: Vertical | movement | Reconstructed |
| `boatfly-b` | boatfly B: Vertical | movement | Reconstructed |
| `boatfly-c` | boatfly C: Hover | movement | Reconstructed |
| `entityflight-a` | entityflight A: Ascension | movement | Reconstructed |
| `entityflight-b` | entityflight B: Hover | movement | Reconstructed |
| `antilevitation-a` | antilevitation A: Ignore | movement | Reconstructed |
| `nosaddle-a` | nosaddle A: Spoof | movement | Reconstructed |
| `entityspeed-a` | entityspeed A: Limit | movement | Reconstructed |
| `elytra-a` | elytra A: Limit | movement | Reconstructed |
| `elytra-b` | elytra B: Acceleration | movement | Reconstructed |
| `elytra-c` | elytra C: Acceleration | movement | Reconstructed |
| `elytra-f` | elytra F: Packet | movement | Reconstructed |
| `elytra-g` | elytra G: Acceleration | movement | Reconstructed |
| `elytra-i` | elytra I: Motion | movement | Reconstructed |
| `elytra-k` | elytra K: Acceleration | movement | Reconstructed |
| `elytra-l` | elytra L: Ascending | movement | Reconstructed |
| `elytra-m` | elytra M: Ascending | movement | Reconstructed |
| `elytra-n` | elytra N: Ground | movement | Reconstructed |
| `speed-a` | speed A: Friction | movement | Reconstructed |
| `speed-b` | speed B: Ground | movement | Reconstructed |
| `speed-c` | speed C: Air | movement | Reconstructed |
| `speed-d` | speed D: Ground | movement | Reconstructed |
| `speed-e` | speed E: Prediction | movement | Reconstructed |
| `fastclimb-a` | fastclimb A: Limit | movement | Reconstructed |
| `wallclimb-a` | wallclimb A: Spider | movement | Reconstructed |
| `jesus-a` | jesus A: Ground | movement | Reconstructed |
| `jesus-b` | jesus B: Motion | movement | Reconstructed |
| `jesus-c` | jesus C: Y Motion | movement | Reconstructed |
| `jesus-d` | jesus D: Jump | movement | Reconstructed |
| `jesus-e` | jesus E: Speed | movement | Reconstructed |
| `flight-a` | flight A: Prediction [S] | movement | Reconstructed |
| `flight-b` | flight B: Prediction [C] | movement | Reconstructed |
| `flight-c` | flight C: Ascension | movement | Reconstructed |
| `flight-d` | flight D: Glide | movement | Reconstructed |
| `flight-e` | flight E: Hover | movement | Reconstructed |
| `flight-f` | flight F: Prediction | movement | Reconstructed |
| `vclip-a` | vclip A: Clip | movement | Reconstructed |
| `sprint-a` | sprint A: Angle | movement | Reconstructed |
| `strafe-a` | strafe A: Air | movement | Reconstructed |
| `strafe-b` | strafe B: Unavailable legacy configuration | movement | No registered upstream detector; disabled |
| `fastuse-a` | fastuse A: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `fastplace-a` | fastplace A: Delay | player | Reconstructed |
| `fastplace-b` | fastplace B: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `fastbreak-a` | fastbreak A: Delay | player | Reconstructed |
| `tower-a` | tower A: Limit | player | Reconstructed |
| `scaffold-a` | scaffold A: Interact | player | Reconstructed |
| `scaffold-b` | scaffold B: Interact | player | Reconstructed |
| `scaffold-c` | scaffold C: Sprint | player | Reconstructed |
| `scaffold-d` | scaffold D: Rotations | player | Reconstructed |
| `scaffold-e` | scaffold E: Rotations [2] | player | Reconstructed |
| `scaffold-f` | scaffold F: Packet | player | Reconstructed |
| `scaffold-g` | scaffold G: Speed | player | Reconstructed |
| `scaffold-h` | scaffold H: Rotations [3] | player | Reconstructed |
| `scaffold-i` | scaffold I: Acceleration | player | Reconstructed |
| `scaffold-j` | scaffold J: Acceleration [2] | player | Reconstructed |
| `scaffold-k` | scaffold K: Limit | player | Reconstructed |
| `scaffold-m` | scaffold M: Expand | player | Reconstructed |
| `scaffold-n` | scaffold N: Expand | player | Reconstructed |
| `scaffold-o` | scaffold O: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `timer-a` | timer A: Average | player | Reconstructed |
| `timer-d` | timer D: Balance | player | Reconstructed |
| `baritone-a` | baritone A: Rotations | player | Reconstructed |
| `baritone-b` | baritone B: Large Rotation | player | Reconstructed |
| `ghosthand-a` | ghosthand A: Bed | player | Reconstructed |
| `airplace-a` | airplace A: Invalid | player | Reconstructed |
| `badpackets-a` | badpackets A: Abilities | player | Reconstructed |
| `badpackets-b` | badpackets B: Sequence | player | Reconstructed |
| `badpackets-c` | badpackets C: Pitch | player | Reconstructed |
| `badpackets-d` | badpackets D: Timer (inert original slot) | player | Original no-op; disabled |
| `badpackets-e` | badpackets E: Interact | player | Reconstructed |
| `badpackets-f` | badpackets F: Steer Vehicle | player | Reconstructed |
| `badpackets-g` | badpackets G: Hotbar | player | Reconstructed |
| `badpackets-h` | badpackets H: Packet Order | player | Legacy client only |
| `badpackets-i` | badpackets I: Entity Action | player | Legacy client only |
| `badpackets-j` | badpackets J: Held Item Slot | player | Legacy client only |
| `badpackets-k` | badpackets K: Sneak | player | Reconstructed |
| `badpackets-m` | badpackets M: Post BlockPlace | player | Reconstructed |
| `badpackets-n` | badpackets N: Post BlockDig | player | Reconstructed |
| `badpackets-o` | badpackets O: Negative Hotbar Slot | player | Reconstructed |
| `badpackets-p` | badpackets P: Post Entity Action | player | Reconstructed |
| `badpackets-q` | badpackets Q: Hotbar | player | Reconstructed |
| `badpackets-r` | badpackets R: Post HeldItemSlot | player | Reconstructed |
| `badpackets-s` | badpackets S: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `badpackets-t` | badpackets T: Keep Alive | player | Reconstructed |
| `badpackets-u` | badpackets U: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `badpackets-v` | badpackets V: Null Vehicle (inert original slot) | player | Original no-op; disabled |
| `badpackets-w` | badpackets W: Post WindowClick (inert original slot) | player | Original no-op; disabled |
| `badpackets-x` | badpackets X: Post ArmAnimation | player | Reconstructed |
| `badpackets-y` | badpackets Y: Invalid Position | player | Reconstructed |
| `badpackets-z` | badpackets Z: Spectate | player | Reconstructed |
| `badpackets-1` | badpackets 1: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `badpackets-2` | badpackets 2: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `badpackets-4` | badpackets 4: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `badpackets-5` | badpackets 5: Scaffold | player | Reconstructed |
| `badpackets-6` | badpackets 6: Nuker | player | Reconstructed |
| `badpackets-7` | badpackets 7: No Swing | player | Client before 1.21.2 |
| `badpackets-8` | badpackets 8: Nuker | player | Reconstructed |
| `badpackets-9` | badpackets 9: No Swing | player | Client 1.21.2 or newer |
| `inventory-a` | inventory A: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `inventory-b` | inventory B: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `invalid-a` | invalid A: Invalid | player | Reconstructed |
| `invalid-b` | invalid B: Invalid | player | Reconstructed |
| `invalid-c` | invalid C: Y | player | Reconstructed |
| `invalid-d` | invalid D: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `invalid-e` | invalid E: X/Z | player | Reconstructed |
| `invalid-f` | invalid F: Spoofed Y | player | Reconstructed |
| `invalid-g` | invalid G: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `invalid-h` | invalid H: Unavailable legacy configuration | player | No registered upstream detector; disabled |
| `invalid-i` | invalid I: Invalid Y movement | player | Reconstructed |
| `invalid-j` | invalid J: Motion | player | Reconstructed |
| `groundspoof-a` | groundspoof A: Spoof | player | Reconstructed |
| `groundspoof-b` | groundspoof B: Spoof | player | Reconstructed |
| `groundspoof-c` | groundspoof C: Spoof | player | Reconstructed |
| `improbable-a` | improbable A: Combat | player | Reconstructed |
| `improbable-b` | improbable B: Movement | player | Reconstructed |
| `improbable-c` | improbable C: Player | player | Reconstructed |
| `improbable-d` | improbable D: Auto Clicker | player | Reconstructed |
| `improbable-e` | improbable E: Total | player | Reconstructed |
| `improbable-f` | improbable F: Scaffold | player | Reconstructed |

## Implementation boundaries

Movement uses public attributes, loaded server collision geometry, environmental state and conservative budgets. Velocity uses actual outgoing impulses correlated with later known acknowledgements and candidate response intervals. Unknown physics is exempt. Reach A and Hitbox A use bounded target-relative packet history with captured ticks and acknowledged latency; Reach B uses retained raw positions and its own allowance budget. This is not a replicated per-client world. Aim/click statistics use bounded windows; cinematic camera state is reconstructed from packet history. Click timing and CPS are maintained independently of detector resets. World rules preserve per-check event authority: Scaffold N requires accepted placement chains, while Scaffold J also observes cancelled placement callbacks. The combined-VL rules exclude their own family to avoid feedback.

Recovered defects are corrected deliberately: unincremented digging counters, unreset swing latches, impossible ground predicates, reference-versus-coordinate block comparisons and Scaffold A's mismatched interaction dispatcher. Click checks retain the recovered dispersion, median-bias and tail-weight heuristics rather than substituting conventional statistical moments. Clock rollback and bounded placement history add robustness guards. These deliberate differences and remaining independently reconstructed heuristics mean complete original equivalence is not claimed. See [reverse-engineering notes](REVERSE_ENGINEERING.md) and [validation](VALIDATION.md).

## Further source recovery in 0.4.0

FastPlace uses a batch beginning with the first eligible placement, strictly more than 1,000 milliseconds, including the triggering event. Exempt placements retain that phase. GhostHand uses the ten neighboring bed-enclosure materials, not an eye ray, and flags directly; accepted findings can cancel the scoped LOWEST break event. Its API veto and neighboring-BEDROCK repair are deliberate improvements. Timer A retains fifty intervals; Timer D retains its unclamped millisecond balance and uses strict buffers with under-threshold decay. Position-only ages, previous-floor origin state and cached Bukkit XYZ-move material samples supersede the broader producer assumptions recorded for 0.3.0. Combined checks use separate cumulative counters, not sums of current VL. These additional recovered scopes do not establish complete equivalence for the remaining movement/world branches.

## Further behavioral recovery in 0.5.0

Combat sequence rules use raw packet clocks and independent eligible batches. Criticals preserve signed vertical remainders; FastBow observes eligible arrow launches at MONITOR, including cancelled launches, with crossbow and multishot exclusions. Velocity A/B/D use two-stage PING acknowledgements, retained impulse state and independently generated candidate motion rather than elapsed-time surrogates. Hitbox B preserves its proven dormant reference path.

Packet rules retain actual frame latches and modern/legacy applicability gates. Invalid motion uses acknowledged effects, impulse bounds, position-only ages and per-rule material allowances. Ground claims use the recovered cached air domains and strict predicates. FastBreak uses SURVIVAL interaction/start timing and LOWEST uncancelled break authority; public Bukkit break speed replaces the obsolete material hardness table. These additions do not close all 51 movement paths or remaining scaffold/world branches.
