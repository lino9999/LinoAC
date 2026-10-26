package io.github.lino9999.linoac.check;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CheckType {
    AIM_A("aim-a", "aim A: Slope", "combat", true),
    AIM_B("aim-b", "aim B: Modulo", "combat", true),
    AIM_C("aim-c", "aim C: Repeated", "combat", true),
    AIM_D("aim-d", "aim D: Straight", "combat", true),
    AIM_E("aim-e", "aim E: Ratio", "combat", true),
    AIM_F("aim-f", "aim F: Straight", "combat", true),
    AIM_G("aim-g", "aim G: Ratio", "combat", true),
    AIM_H("aim-h", "aim H: Negative", "combat", true),
    AIM_I("aim-i", "aim I: Constant", "combat", true),
    AIM_K("aim-k", "aim K: Linear", "combat", true),
    AIM_L("aim-l", "aim L: Direction", "combat", true),
    AIM_M("aim-m", "aim M: Unavailable legacy configuration", "combat", false),
    AIM_N("aim-n", "aim N: Small Yaw", "combat", true),
    AIM_O("aim-o", "aim O: Small Yaw", "combat", true),
    AIM_P("aim-p", "aim P: Yaw Acceleration", "combat", true),
    AIM_Q("aim-q", "aim Q: GCD Modulo", "combat", true),
    AIM_R("aim-r", "aim R: Unavailable legacy configuration", "combat", false),
    AIM_S("aim-s", "aim S: Divisor Y", "combat", true),
    AIM_U("aim-u", "aim U: GCD Flaw", "combat", true),
    AIM_W("aim-w", "aim W: Analysis", "combat", true),
    AIM_X("aim-x", "aim X: Analysis", "combat", true),
    AIM_Y("aim-y", "aim Y: Rotation", "combat", true),
    AUTOBLOCK_A("autoblock-a", "autoblock A: Sequence", "combat", true),
    AUTOBLOCK_B("autoblock-b", "autoblock B: Packet", "combat", true),
    AUTOBLOCK_C("autoblock-c", "autoblock C: Order", "combat", true),
    AUTOBLOCK_D("autoblock-d", "autoblock D: Order", "combat", true),
    AUTOCLICKER_A("autoclicker-a", "autoclicker A: Limit", "combat", true),
    AUTOCLICKER_B("autoclicker-b", "autoclicker B: Deviation", "combat", true),
    AUTOCLICKER_C("autoclicker-c", "autoclicker C: Rounded", "combat", true),
    AUTOCLICKER_D("autoclicker-d", "autoclicker D: Skewness", "combat", true),
    AUTOCLICKER_E("autoclicker-e", "autoclicker E: Variance", "combat", true),
    AUTOCLICKER_F("autoclicker-f", "autoclicker F: Distinct", "combat", true),
    AUTOCLICKER_G("autoclicker-g", "autoclicker G: Outliers", "combat", true),
    AUTOCLICKER_H("autoclicker-h", "autoclicker H: Average Deviation", "combat", true),
    AUTOCLICKER_I("autoclicker-i", "autoclicker I: Kurtosis", "combat", true),
    AUTOCLICKER_J("autoclicker-j", "autoclicker J: Range", "combat", true),
    AUTOCLICKER_K("autoclicker-k", "autoclicker K: Average Difference", "combat", true),
    AUTOCLICKER_L("autoclicker-l", "autoclicker L: Kurtosis Difference", "combat", true),
    AUTOCLICKER_M("autoclicker-m", "autoclicker M: Variance Difference", "combat", true),
    AUTOCLICKER_N("autoclicker-n", "autoclicker N: Deviation Difference", "combat", true),
    AUTOCLICKER_O("autoclicker-o", "autoclicker O: Spikes", "combat", true),
    AUTOCLICKER_P("autoclicker-p", "autoclicker P: Identical", "combat", true),
    AUTOCLICKER_Q("autoclicker-q", "autoclicker Q: Average Deviation", "combat", true),
    AUTOCLICKER_R("autoclicker-r", "autoclicker R: Consistency", "combat", true),
    AUTOCLICKER_S("autoclicker-s", "autoclicker S: Distinct", "combat", true),
    AUTOCLICKER_T("autoclicker-t", "autoclicker T: Kurtosis", "combat", true),
    REACH_A("reach-a", "reach A: History", "combat", true),
    REACH_B("reach-b", "reach B: Simple", "combat", true),
    FASTBOW_A("fastbow-a", "fastbow A: Limit", "combat", true),
    CRITICALS_A("criticals-a", "criticals A: Ground", "combat", true),
    CRITICALS_B("criticals-b", "criticals B: Modulo", "combat", true),
    KILLAURA_A("killaura-a", "killaura A: Post", "combat", true),
    KILLAURA_B("killaura-b", "killaura B: Acceleration", "combat", true),
    KILLAURA_C("killaura-c", "killaura C: Head Snap", "combat", true),
    KILLAURA_D("killaura-d", "killaura D: Multi Aura", "combat", true),
    KILLAURA_F("killaura-f", "killaura F: Unavailable legacy configuration", "combat", false),
    KILLAURA_H("killaura-h", "killaura H: Unavailable legacy configuration", "combat", false),
    KILLAURA_J("killaura-j", "killaura J: Frequency", "combat", true),
    KILLAURA_K("killaura-k", "killaura K: Pattern", "combat", true),
    KILLAURA_L("killaura-l", "killaura L: Strafe", "combat", true),
    HITBOX_A("hitbox-a", "hitbox A: History", "combat", true),
    HITBOX_B("hitbox-b", "hitbox B: Simple", "combat", true),
    VELOCITY_A("velocity-a", "velocity A: Vertical", "combat", true),
    VELOCITY_B("velocity-b", "velocity B: Horizontal", "combat", true),
    VELOCITY_C("velocity-c", "velocity C: Ignored Vertical (inert original slot)", "combat", false),
    VELOCITY_D("velocity-d", "velocity D: Horizontal", "combat", true),
    NOSLOW_A("noslow-a", "noslow A: Packet", "movement", true),
    NOSLOW_B("noslow-b", "noslow B: Soul Sand", "movement", true),
    NOSLOW_C("noslow-c", "noslow C: Web", "movement", true),
    NOSLOW_D("noslow-d", "noslow D: Unavailable legacy configuration", "movement", false),
    NOSLOW_E("noslow-e", "noslow E: Unavailable legacy configuration", "movement", false),
    MOTION_A("motion-a", "motion A: Constant", "movement", true),
    MOTION_B("motion-b", "motion B: Inverse", "movement", true),
    MOTION_C("motion-c", "motion C: Jump", "movement", true),
    MOTION_D("motion-d", "motion D: Unavailable legacy configuration", "movement", false),
    MOTION_E("motion-e", "motion E: Modulo", "movement", true),
    MOTION_G("motion-g", "motion G: Unavailable legacy configuration", "movement", false),
    MOTION_H("motion-h", "motion H: Unavailable legacy configuration", "movement", false),
    JUMP_A("jump-a", "jump A: Motion", "movement", true),
    JUMP_B("jump-b", "jump B: Height", "movement", true),
    JUMP_C("jump-c", "jump C: Motion", "movement", true),
    STEP_A("step-a", "step A: Vanilla", "movement", true),
    STEP_B("step-b", "step B: Unavailable legacy configuration", "movement", false),
    STEP_C("step-c", "step C: Motion", "movement", true),
    BOATFLY_A("boatfly-a", "boatfly A: Vertical", "movement", true),
    BOATFLY_B("boatfly-b", "boatfly B: Vertical", "movement", true),
    BOATFLY_C("boatfly-c", "boatfly C: Hover", "movement", true),
    ENTITYFLIGHT_A("entityflight-a", "entityflight A: Ascension", "movement", true),
    ENTITYFLIGHT_B("entityflight-b", "entityflight B: Hover", "movement", true),
    ANTILEVITATION_A("antilevitation-a", "antilevitation A: Ignore", "movement", true),
    NOSADDLE_A("nosaddle-a", "nosaddle A: Spoof", "movement", true),
    ENTITYSPEED_A("entityspeed-a", "entityspeed A: Limit", "movement", true),
    ELYTRA_A("elytra-a", "elytra A: Limit", "movement", true),
    ELYTRA_B("elytra-b", "elytra B: Acceleration", "movement", true),
    ELYTRA_C("elytra-c", "elytra C: Acceleration", "movement", true),
    ELYTRA_F("elytra-f", "elytra F: Packet", "movement", true),
    ELYTRA_G("elytra-g", "elytra G: Acceleration", "movement", true),
    ELYTRA_I("elytra-i", "elytra I: Motion", "movement", true),
    ELYTRA_K("elytra-k", "elytra K: Acceleration", "movement", true),
    ELYTRA_L("elytra-l", "elytra L: Ascending", "movement", true),
    ELYTRA_M("elytra-m", "elytra M: Ascending", "movement", true),
    ELYTRA_N("elytra-n", "elytra N: Ground", "movement", true),
    SPEED_A("speed-a", "speed A: Friction", "movement", true),
    SPEED_B("speed-b", "speed B: Ground", "movement", true),
    SPEED_C("speed-c", "speed C: Air", "movement", true),
    SPEED_D("speed-d", "speed D: Ground", "movement", true),
    SPEED_E("speed-e", "speed E: Prediction", "movement", true),
    FASTCLIMB_A("fastclimb-a", "fastclimb A: Limit", "movement", true),
    WALLCLIMB_A("wallclimb-a", "wallclimb A: Spider", "movement", true),
    JESUS_A("jesus-a", "jesus A: Ground", "movement", true),
    JESUS_B("jesus-b", "jesus B: Motion", "movement", true),
    JESUS_C("jesus-c", "jesus C: Y Motion", "movement", true),
    JESUS_D("jesus-d", "jesus D: Jump", "movement", true),
    JESUS_E("jesus-e", "jesus E: Speed", "movement", true),
    FLIGHT_A("flight-a", "flight A: Prediction [S]", "movement", true),
    FLIGHT_B("flight-b", "flight B: Prediction [C]", "movement", true),
    FLIGHT_C("flight-c", "flight C: Ascension", "movement", true),
    FLIGHT_D("flight-d", "flight D: Glide", "movement", true),
    FLIGHT_E("flight-e", "flight E: Hover", "movement", true),
    FLIGHT_F("flight-f", "flight F: Prediction", "movement", true),
    VCLIP_A("vclip-a", "vclip A: Clip", "movement", true),
    SPRINT_A("sprint-a", "sprint A: Angle", "movement", true),
    STRAFE_A("strafe-a", "strafe A: Air", "movement", true),
    STRAFE_B("strafe-b", "strafe B: Unavailable legacy configuration", "movement", false),
    FASTUSE_A("fastuse-a", "fastuse A: Unavailable legacy configuration", "player", false),
    FASTPLACE_A("fastplace-a", "fastplace A: Delay", "player", true),
    FASTPLACE_B("fastplace-b", "fastplace B: Unavailable legacy configuration", "player", false),
    FASTBREAK_A("fastbreak-a", "fastbreak A: Delay", "player", true),
    TOWER_A("tower-a", "tower A: Limit", "player", true),
    SCAFFOLD_A("scaffold-a", "scaffold A: Interact", "player", true),
    SCAFFOLD_B("scaffold-b", "scaffold B: Interact", "player", true),
    SCAFFOLD_C("scaffold-c", "scaffold C: Sprint", "player", true),
    SCAFFOLD_D("scaffold-d", "scaffold D: Rotations", "player", true),
    SCAFFOLD_E("scaffold-e", "scaffold E: Rotations [2]", "player", true),
    SCAFFOLD_F("scaffold-f", "scaffold F: Packet", "player", true),
    SCAFFOLD_G("scaffold-g", "scaffold G: Speed", "player", true),
    SCAFFOLD_H("scaffold-h", "scaffold H: Rotations [3]", "player", true),
    SCAFFOLD_I("scaffold-i", "scaffold I: Acceleration", "player", true),
    SCAFFOLD_J("scaffold-j", "scaffold J: Acceleration [2]", "player", true),
    SCAFFOLD_K("scaffold-k", "scaffold K: Limit", "player", true),
    SCAFFOLD_M("scaffold-m", "scaffold M: Expand", "player", true),
    SCAFFOLD_N("scaffold-n", "scaffold N: Expand", "player", true),
    SCAFFOLD_O("scaffold-o", "scaffold O: Unavailable legacy configuration", "player", false),
    TIMER_A("timer-a", "timer A: Average", "player", true),
    TIMER_D("timer-d", "timer D: Balance", "player", true),
    BARITONE_A("baritone-a", "baritone A: Rotations", "player", true),
    BARITONE_B("baritone-b", "baritone B: Large Rotation", "player", true),
    GHOSTHAND_A("ghosthand-a", "ghosthand A: Bed", "player", true),
    AIRPLACE_A("airplace-a", "airplace A: Invalid", "player", true),
    BADPACKETS_A("badpackets-a", "badpackets A: Abilities", "player", true),
    BADPACKETS_B("badpackets-b", "badpackets B: Sequence", "player", true),
    BADPACKETS_C("badpackets-c", "badpackets C: Pitch", "player", true),
    BADPACKETS_D("badpackets-d", "badpackets D: Timer (inert original slot)", "player", false),
    BADPACKETS_E("badpackets-e", "badpackets E: Interact", "player", true),
    BADPACKETS_F("badpackets-f", "badpackets F: Steer Vehicle", "player", true),
    BADPACKETS_G("badpackets-g", "badpackets G: Hotbar", "player", true),
    BADPACKETS_H("badpackets-h", "badpackets H: Packet Order", "player", true),
    BADPACKETS_I("badpackets-i", "badpackets I: Entity Action", "player", true),
    BADPACKETS_J("badpackets-j", "badpackets J: Held Item Slot", "player", true),
    BADPACKETS_K("badpackets-k", "badpackets K: Sneak", "player", true),
    BADPACKETS_M("badpackets-m", "badpackets M: Post BlockPlace", "player", true),
    BADPACKETS_N("badpackets-n", "badpackets N: Post BlockDig", "player", true),
    BADPACKETS_O("badpackets-o", "badpackets O: Negative Hotbar Slot", "player", true),
    BADPACKETS_P("badpackets-p", "badpackets P: Post Entity Action", "player", true),
    BADPACKETS_Q("badpackets-q", "badpackets Q: Hotbar", "player", true),
    BADPACKETS_R("badpackets-r", "badpackets R: Post HeldItemSlot", "player", true),
    BADPACKETS_S("badpackets-s", "badpackets S: Unavailable legacy configuration", "player", false),
    BADPACKETS_T("badpackets-t", "badpackets T: Keep Alive", "player", true),
    BADPACKETS_U("badpackets-u", "badpackets U: Unavailable legacy configuration", "player", false),
    BADPACKETS_V("badpackets-v", "badpackets V: Null Vehicle (inert original slot)", "player", false),
    BADPACKETS_W("badpackets-w", "badpackets W: Post WindowClick (inert original slot)", "player", false),
    BADPACKETS_X("badpackets-x", "badpackets X: Post ArmAnimation", "player", true),
    BADPACKETS_Y("badpackets-y", "badpackets Y: Invalid Position", "player", true),
    BADPACKETS_Z("badpackets-z", "badpackets Z: Spectate", "player", true),
    BADPACKETS_1("badpackets-1", "badpackets 1: Unavailable legacy configuration", "player", false),
    BADPACKETS_2("badpackets-2", "badpackets 2: Unavailable legacy configuration", "player", false),
    BADPACKETS_4("badpackets-4", "badpackets 4: Unavailable legacy configuration", "player", false),
    BADPACKETS_5("badpackets-5", "badpackets 5: Scaffold", "player", true),
    BADPACKETS_6("badpackets-6", "badpackets 6: Nuker", "player", true),
    BADPACKETS_7("badpackets-7", "badpackets 7: No Swing", "player", true),
    BADPACKETS_8("badpackets-8", "badpackets 8: Nuker", "player", true),
    BADPACKETS_9("badpackets-9", "badpackets 9: No Swing", "player", true),
    INVENTORY_A("inventory-a", "inventory A: Unavailable legacy configuration", "player", false),
    INVENTORY_B("inventory-b", "inventory B: Unavailable legacy configuration", "player", false),
    INVALID_A("invalid-a", "invalid A: Invalid", "player", true),
    INVALID_B("invalid-b", "invalid B: Invalid", "player", true),
    INVALID_C("invalid-c", "invalid C: Y", "player", true),
    INVALID_D("invalid-d", "invalid D: Unavailable legacy configuration", "player", false),
    INVALID_E("invalid-e", "invalid E: X/Z", "player", true),
    INVALID_F("invalid-f", "invalid F: Spoofed Y", "player", true),
    INVALID_G("invalid-g", "invalid G: Unavailable legacy configuration", "player", false),
    INVALID_H("invalid-h", "invalid H: Unavailable legacy configuration", "player", false),
    INVALID_I("invalid-i", "invalid I: Invalid Y movement", "player", true),
    INVALID_J("invalid-j", "invalid J: Motion", "player", true),
    GROUNDSPOOF_A("groundspoof-a", "groundspoof A: Spoof", "player", true),
    GROUNDSPOOF_B("groundspoof-b", "groundspoof B: Spoof", "player", true),
    GROUNDSPOOF_C("groundspoof-c", "groundspoof C: Spoof", "player", true),
    IMPROBABLE_A("improbable-a", "improbable A: Combat", "player", true),
    IMPROBABLE_B("improbable-b", "improbable B: Movement", "player", true),
    IMPROBABLE_C("improbable-c", "improbable C: Player", "player", true),
    IMPROBABLE_D("improbable-d", "improbable D: Auto Clicker", "player", true),
    IMPROBABLE_E("improbable-e", "improbable E: Total", "player", true),
    IMPROBABLE_F("improbable-f", "improbable F: Scaffold", "player", true);

    public static final CheckType SPEED = SPEED_A;
    public static final CheckType FLIGHT = FLIGHT_A;
    public static final CheckType JUMP = JUMP_A;
    public static final CheckType STEP = STEP_A;
    public static final CheckType FAST_CLIMB = FASTCLIMB_A;
    public static final CheckType JESUS = JESUS_A;
    public static final CheckType NO_SLOW = NOSLOW_A;
    public static final CheckType SPRINT = SPRINT_A;
    public static final CheckType REACH = REACH_A;
    public static final CheckType HITBOX = HITBOX_A;
    public static final CheckType AUTO_CLICKER = AUTOCLICKER_A;
    public static final CheckType MULTI_AURA = KILLAURA_D;
    public static final CheckType FAST_BOW = FASTBOW_A;
    public static final CheckType FAST_PLACE = FASTPLACE_A;
    public static final CheckType FAST_BREAK = FASTBREAK_A;
    public static final CheckType GHOST_HAND = GHOSTHAND_A;
    public static final CheckType AIR_PLACE = AIRPLACE_A;
    public static final CheckType SCAFFOLD = SCAFFOLD_A;
    public static final CheckType INVENTORY = INVENTORY_A;
    public static final CheckType TIMER = TIMER_A;
    public static final CheckType BAD_PACKETS = BADPACKETS_Y;
    public static final CheckType PACKET_FLOOD = BADPACKETS_D;

    private static final java.util.Set<CheckType> EXPERIMENTAL = java.util.Set.of(KILLAURA_L, VELOCITY_D, ELYTRA_N, SCAFFOLD_N, BADPACKETS_D, INVALID_J);
    private static final java.util.Map<String, String> DISPLAY_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("aim", "Aim"),
            java.util.Map.entry("autoblock", "Auto Block"),
            java.util.Map.entry("autoclicker", "Auto Clicker"),
            java.util.Map.entry("reach", "Reach"),
            java.util.Map.entry("fastbow", "Fast Bow"),
            java.util.Map.entry("criticals", "Criticals"),
            java.util.Map.entry("killaura", "Kill Aura"),
            java.util.Map.entry("hitbox", "Hitbox"),
            java.util.Map.entry("velocity", "Velocity"),
            java.util.Map.entry("noslow", "No Slow"),
            java.util.Map.entry("motion", "Motion"),
            java.util.Map.entry("jump", "Jump"),
            java.util.Map.entry("step", "Step"),
            java.util.Map.entry("boatfly", "Boat Fly"),
            java.util.Map.entry("entityflight", "Entity Flight"),
            java.util.Map.entry("antilevitation", "Anti Levitation"),
            java.util.Map.entry("nosaddle", "No Saddle"),
            java.util.Map.entry("entityspeed", "Entity Speed"),
            java.util.Map.entry("elytra", "Elytra"),
            java.util.Map.entry("speed", "Speed"),
            java.util.Map.entry("fastclimb", "Fast Climb"),
            java.util.Map.entry("wallclimb", "Wall Climb"),
            java.util.Map.entry("jesus", "Jesus"),
            java.util.Map.entry("flight", "Flight"),
            java.util.Map.entry("vclip", "VClip"),
            java.util.Map.entry("sprint", "Sprint"),
            java.util.Map.entry("strafe", "Strafe"),
            java.util.Map.entry("fastplace", "Fast Place"),
            java.util.Map.entry("fastbreak", "Fast Break"),
            java.util.Map.entry("tower", "Tower"),
            java.util.Map.entry("scaffold", "Scaffold"),
            java.util.Map.entry("timer", "Timer"),
            java.util.Map.entry("baritone", "Baritone"),
            java.util.Map.entry("ghosthand", "Ghost Hand"),
            java.util.Map.entry("airplace", "Air Place"),
            java.util.Map.entry("badpackets", "Bad Packets"),
            java.util.Map.entry("invalid", "Invalid"),
            java.util.Map.entry("groundspoof", "Ground Spoof"),
            java.util.Map.entry("improbable", "Improbable"),
            java.util.Map.entry("inventory", "Inventory"),
            java.util.Map.entry("fastuse", "Fast Use"));

    public boolean experimental() { return EXPERIMENTAL.contains(this); }
    public String displayName() { return DISPLAY_NAMES.getOrDefault(family(), family()); }
    public String displayType() { return id.substring(id.lastIndexOf('-') + 1).toUpperCase(java.util.Locale.ROOT); }

    private final String id;
    private final String description;
    private final String category;
    private final boolean available;

    CheckType(String id, String description, String category, boolean available) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.available = available;
    }

    public String id() { return id; }
    public String description() { return description; }
    public String category() { return category; }
    public boolean available() { return available; }
    public String family() { return id.substring(0, id.lastIndexOf('-')); }

    public static Optional<CheckType> find(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace("_", "-");
        String compact = normalized.replace("-", "");
        Optional<CheckType> exact = Arrays.stream(values()).filter(type -> type.id.equals(normalized) || type.id.replace("-", "").equals(compact)).findFirst();
        if (exact.isPresent()) return exact;
        return switch (compact) {
            case "speed" -> Optional.of(SPEED_A);
            case "flight" -> Optional.of(FLIGHT_A);
            case "jump" -> Optional.of(JUMP_A);
            case "step" -> Optional.of(STEP_A);
            case "fastclimb" -> Optional.of(FASTCLIMB_A);
            case "jesus" -> Optional.of(JESUS_A);
            case "noslow" -> Optional.of(NOSLOW_A);
            case "sprint" -> Optional.of(SPRINT_A);
            case "reach" -> Optional.of(REACH_A);
            case "hitbox" -> Optional.of(HITBOX_A);
            case "autoclicker" -> Optional.of(AUTOCLICKER_A);
            case "multiaura" -> Optional.of(KILLAURA_D);
            case "fastbow" -> Optional.of(FASTBOW_A);
            case "fastplace" -> Optional.of(FASTPLACE_A);
            case "fastbreak" -> Optional.of(FASTBREAK_A);
            case "ghosthand" -> Optional.of(GHOSTHAND_A);
            case "airplace" -> Optional.of(AIRPLACE_A);
            case "scaffold" -> Optional.of(SCAFFOLD_A);
            case "inventory" -> Optional.of(INVENTORY_A);
            case "timer" -> Optional.of(TIMER_A);
            case "badpackets" -> Optional.of(BADPACKETS_Y);
            case "packetflood" -> Optional.of(BADPACKETS_D);
            default -> Optional.empty();
        };
    }
}
