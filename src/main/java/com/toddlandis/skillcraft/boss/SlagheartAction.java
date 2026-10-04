package com.toddlandis.skillcraft.boss;

// Everything Slagheart can be doing. Synced to clients (as the ordinal) so they can play the
// matching animation; the animation file names match getSerializedName().
public enum SlagheartAction {
    NONE("idle"),
    AWAKEN("awaken"),
    HAMMERFALL("hammerfall"),
    TONG_SWEEP("tong_sweep"),
    SLAG_SPIT("slag_spit"),
    EMBER_RAIN("ember_rain"),
    FORGE_QUAKE("forge_quake"),
    VENT("vent"),
    ROAR("roar"),
    DEATH("death");

    private final String animation;

    SlagheartAction(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static SlagheartAction byId(int id) {
        SlagheartAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
