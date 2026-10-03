#!/usr/bin/env bash
# TEMPORARY (removed before merge): prints Minecraft API sources the dev container can't download.
set -u
BIN=$(ls build/moddev/artifacts/minecraft-patched-*.jar | grep -v sources | head -1)
curl -sSLo /tmp/vf.jar https://repo1.maven.org/maven2/org/vineflower/vineflower/1.12.0/vineflower-1.12.0.jar
jar tf "$BIN" | grep '\.class$' > /tmp/classes.txt
NAMES=$(grep -v '^#' scripts/ci/probe_classes.txt | tr '\n' ' ')
mkdir -p /tmp/cls /tmp/mcout /tmp/mc
SEL=""
for n in $NAMES; do SEL="$SEL $(grep -E "/$n(\\$[^/]*)?\.class$" /tmp/classes.txt | tr '\n' ' ')"; done
(cd /tmp/cls && jar xf "$GITHUB_WORKSPACE/$BIN" $SEL)
(cd /tmp/cls && jar cf /tmp/sel.jar .)
java -jar /tmp/vf.jar -dgs=1 -log=WARN -e="$BIN" /tmp/sel.jar /tmp/mcout > /dev/null 2>&1
(cd /tmp/mc && jar xf /tmp/mcout/sel.jar); echo "decompiled: $(find /tmp/mc -name '*.java' | wc -l) files"
for q in $(grep -v '^#' scripts/ci/probe_find.txt 2>/dev/null); do echo "=================== FIND $q"; grep -iE "$q" /tmp/classes.txt | head -40; done
for j in build/moddev/artifacts/*.jar; do echo "== $j"; done
show() { for n in "$@"; do f=$(find /tmp/mc -name "$n.java" | head -1); echo "=================== FILE $n ($f)"; [ -n "$f" ] && grep -v '^import ' "$f"; done; }
sig() { for n in "$@"; do f=$(find /tmp/mc -name "$n.java" | head -1); echo "=================== SIG $n ($f)"; [ -n "$f" ] && grep -nE '^\s*(public|protected|static|private)?\s*(abstract |static |final |default )*[A-Za-z<>\[\], ?.]+ [a-zA-Z]+\(.*\)?\s*\{?$|^\s*(public |protected )?(abstract |static |final )*(class|record|interface|enum) ' "$f" | grep -v ' private ' | head -400; done; }
show $NAMES
echo "=================== DATA FILES"
mkdir -p /tmp/res
for j in build/moddev/artifacts/*.jar; do case "$j" in *sources*) ;; *) (cd /tmp/res && jar xf "$GITHUB_WORKSPACE/$j" data assets 2>/dev/null) ;; esac; done
ls /tmp/res/data/minecraft/dimension_type /tmp/res/data/minecraft/test_environment /tmp/res/data/minecraft/test_instance | head -50
ls /tmp/res/data/minecraft/tags/item /tmp/res/data/minecraft/tags/item/enchantable /tmp/res/data/minecraft/tags/entity_type | head -300
ls /tmp/res/data/minecraft/recipe | grep -E "spear|arrow|chain|mace" 
for p in data/minecraft/recipe/iron_spear.json data/minecraft/recipe/wooden_spear.json data/minecraft/recipe/arrow.json data/minecraft/recipe/iron_chain.json data/minecraft/tags/item/swords.json data/minecraft/tags/item/spears.json data/minecraft/tags/item/enchantable/weapon.json data/minecraft/tags/item/enchantable/melee_weapon.json data/minecraft/tags/item/enchantable/sharp_weapon.json data/minecraft/tags/item/enchantable/mace.json data/minecraft/tags/item/arrows.json data/minecraft/tags/item/enchantable/durability.json assets/minecraft/items/arrow.json assets/minecraft/models/item/arrow.json assets/minecraft/models/item/iron_spear_in_hand.json assets/minecraft/models/item/handheld.json assets/minecraft/equipment/leather.json assets/minecraft/items/mace.json assets/minecraft/models/item/mace.json data/minecraft/test_instance/always_pass.json data/minecraft/test_environment/default.json; do echo "--- $p"; cat /tmp/res/$p; echo; done
ls /tmp/res/assets/minecraft/textures/entity/projectiles /tmp/res/assets/minecraft/textures/mob_effect | head -80
