package com.toddlandis.skillcraft.skill;

// The starter set of skills. Each one gets its own package of mechanics later;
// this enum is the single list everything else (saving, syncing, UI) iterates over.
public enum Skill {
    // Combat
    MELEE(Category.COMBAT),
    RANGED(Category.COMBAT),
    CONSTITUTION(Category.COMBAT),

    // Gathering
    MINING(Category.GATHERING),
    WOODCUTTING(Category.GATHERING),
    FISHING(Category.GATHERING),
    FARMING(Category.GATHERING),

    // Artisan
    SMITHING(Category.ARTISAN),
    CRAFTING(Category.ARTISAN),
    FIREMAKING(Category.ARTISAN),
    CONSTRUCTION(Category.ARTISAN),

    // Support
    AGILITY(Category.SUPPORT),
    SAILING(Category.SUPPORT);

    public enum Category { COMBAT, GATHERING, ARTISAN, SUPPORT }

    private final Category category;

    Skill(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }

    // Translation key, e.g. "skill.skillcraft.woodcutting" in en_us.json
    public String translationKey() {
        return "skill.skillcraft." + name().toLowerCase();
    }
}
