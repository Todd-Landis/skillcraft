#!/usr/bin/env bash
# Generates Forging Anvil and Crafting Bench recipes, and switches off the matching vanilla
# crafting-table / smithing-table recipes so gear can only be made at the new stations.
# Usage: bash scripts/gen_bench_recipes.sh   (from the project root)
# Edit the tables below and re-run; the JSON output is committed so the mod doesn't need this to build.
set -euo pipefail

OUT=src/main/resources/data/skillcraft/data_maps/item/bench_recipes.json
DISABLED=src/main/resources/data/minecraft/recipe
mkdir -p "$(dirname "$OUT")" "$DISABLED"

entries=()
disable=()

# recipe <output item> <station> <level> <xp> <time ticks> <ingredient json list> [keep_components_from] [count]
# Output items without a namespace are minecraft: items.
recipe() {
  local keep="" count="" id="$1"
  [ -n "${7:-}" ] && [ "${7:-}" != "-" ] && keep=", \"keep_components_from\": $7"
  [ -n "${8:-}" ] && count=", \"count\": $8"
  [[ "$id" == *:* ]] || id="minecraft:$id"
  entries+=("    \"$id\": { \"station\": \"$2\", \"level\": $3, \"xp\": $4, \"time\": $5$count, \"ingredients\": [$6]$keep }")
}
part() { # part <item or #tag> <count>
  printf '{ "item": "%s", "count": %s }' "$1" "$2"
}

# ---------------- Forging Anvil (Smithing) ----------------
# Pieces in unlock order: name, main material count, sticks, level offset within the metal's tier
PIECES="shovel:1:2:0 sword:2:1:1 hoe:2:2:2 axe:3:2:3 pickaxe:3:2:4 boots:4:0:5 helmet:5:0:6 leggings:7:0:7 chestplate:8:0:8"
# Metals: item prefix, material item, base level, XP per material
METALS="copper:minecraft:copper_ingot:1:12.5 iron:minecraft:iron_ingot:10:25 golden:minecraft:gold_ingot:20:37.5 diamond:minecraft:diamond:30:50"

for metal in $METALS; do
  IFS=: read -r prefix ns mat base xpper <<< "$metal"
  for piece in $PIECES; do
    IFS=: read -r name mats sticks offset <<< "$piece"
    level=$(( base + offset ))
    xp=$(awk "BEGIN { print $mats * $xpper }")
    time=$(( 40 + 20 * mats ))
    parts=$(part "$ns:$mat" "$mats")
    [ "$sticks" -gt 0 ] && parts="$parts, $(part minecraft:stick "$sticks")"
    recipe "${prefix}_${name}" anvil "$level" "$xp" "$time" "$parts"
    disable+=("${prefix}_${name}")
  done
done

# Spears (stab) sit beside swords in each metal; warhammers (crush, SkillCraft) beside axes/pickaxes
SPEAR_METALS="copper:minecraft:copper_ingot:1:12.5 iron:minecraft:iron_ingot:10:25 golden:minecraft:gold_ingot:20:37.5 diamond:minecraft:diamond:30:50"
for metal in $SPEAR_METALS; do
  IFS=: read -r prefix ns mat base xpper <<< "$metal"
  recipe "${prefix}_spear" anvil $(( base + 1 )) "$xpper" 60 "$(part "$ns:$mat" 1), $(part minecraft:stick 2)"
  disable+=("${prefix}_spear")
  recipe "skillcraft:${prefix}_warhammer" anvil $(( base + 3 )) "$(awk "BEGIN { print 4 * $xpper }")" 120 "$(part "$ns:$mat" 4), $(part minecraft:stick 2)"
done

# Chainmail (medium armour): forged from iron nuggets, between the iron plate pieces
for armor in boots:12:11 helmet:15:13 leggings:21:15 chestplate:24:17; do
  IFS=: read -r name nuggets level <<< "$armor"
  recipe "chainmail_$name" anvil "$level" "$(awk "BEGIN { print $nuggets * 25 / 9 }")" $(( 40 + 5 * nuggets )) "$(part minecraft:iron_nugget "$nuggets")"
done

# Ember Sigil: the key to Slagheart's arena (Smithing 20)
recipe skillcraft:ember_sigil anvil 20 45 120 "$(part minecraft:iron_ingot 3), $(part minecraft:gold_ingot 1), $(part minecraft:coal 6), $(part minecraft:redstone 4)"

# Netherite: ingot at 40, then upgrades of diamond gear at 41-49 (still needs the vanilla template)
recipe netherite_ingot anvil 40 100 200 "$(part minecraft:netherite_scrap 4), $(part minecraft:gold_ingot 4)"
disable+=(netherite_ingot)
for piece in $PIECES; do
  IFS=: read -r name mats sticks offset <<< "$piece"
  recipe "netherite_${name}" anvil $(( 41 + offset )) 150 200 \
    "$(part minecraft:diamond_$name 1), $(part minecraft:netherite_ingot 1), $(part minecraft:netherite_upgrade_smithing_template 1)" 0
  disable+=("netherite_${name}_smithing")
done
recipe netherite_spear anvil 41 150 200 \
  "$(part minecraft:diamond_spear 1), $(part minecraft:netherite_ingot 1), $(part minecraft:netherite_upgrade_smithing_template 1)" 0
disable+=(netherite_spear_smithing)
recipe skillcraft:netherite_warhammer anvil 45 150 200 \
  "$(part skillcraft:diamond_warhammer 1), $(part minecraft:netherite_ingot 1), $(part minecraft:netherite_upgrade_smithing_template 1)" 0

# ---------------- Crafting Bench (Crafting) ----------------
# Wooden and stone tools are level 1 so nobody is ever stuck without a pickaxe
TOOLS="shovel:1:2:5 sword:2:1:8 hoe:2:2:8 axe:3:2:12 pickaxe:3:2:12"
for tool in $TOOLS; do
  IFS=: read -r name mats sticks xp <<< "$tool"
  recipe "wooden_$name" crafting_bench 1 "$xp" 40 "$(part '#minecraft:wooden_tool_materials' "$mats"), $(part minecraft:stick "$sticks")"
  recipe "stone_$name" crafting_bench 1 "$(( xp * 2 ))" 50 "$(part '#minecraft:stone_tool_materials' "$mats"), $(part minecraft:stick "$sticks")"
  disable+=("wooden_$name" "stone_$name")
done
recipe wooden_spear crafting_bench 1 8 40 "$(part '#minecraft:wooden_tool_materials' 1), $(part minecraft:stick 2)"
recipe stone_spear crafting_bench 1 16 50 "$(part '#minecraft:stone_tool_materials' 1), $(part minecraft:stick 2)"
disable+=(wooden_spear stone_spear)

# Leather armor: name, leather, level, xp
for armor in boots:4:5:16 helmet:5:7:18 leggings:7:9:27 chestplate:8:11:30; do
  IFS=: read -r name leather level xp <<< "$armor"
  recipe "leather_$name" crafting_bench "$level" "$xp" $(( 40 + 10 * leather )) "$(part minecraft:leather "$leather")"
  disable+=("leather_$name")
done

# Padded armour (light; strong against crush and ranged): wool, leather and string
for armor in boots:2:2:2:20:30 helmet:3:2:2:21:35 leggings:5:2:3:22:50 chestplate:6:3:4:23:60; do
  IFS=: read -r name wool leather string level xp <<< "$armor"
  recipe "skillcraft:padded_$name" crafting_bench "$level" "$xp" $(( 60 + 10 * wool )) \
    "$(part '#minecraft:wool' "$wool"), $(part minecraft:leather "$leather"), $(part minecraft:string "$string")"
done

# Ammo: light arrows (6 per craft) and heavy arrows (4 per craft, need iron nuggets)
recipe skillcraft:light_arrow crafting_bench 1 4 30 "$(part minecraft:stick 1), $(part minecraft:feather 2)" - 6
recipe skillcraft:heavy_arrow crafting_bench 15 12 50 "$(part minecraft:stick 1), $(part minecraft:feather 1), $(part minecraft:iron_nugget 3)" - 4

recipe fishing_rod crafting_bench 1 15 60 "$(part minecraft:stick 3), $(part minecraft:string 2)"
recipe bow crafting_bench 10 25 60 "$(part minecraft:stick 3), $(part minecraft:string 3)"
recipe shield crafting_bench 15 40 80 "$(part '#minecraft:planks' 6), $(part minecraft:iron_ingot 1)"
recipe crossbow crafting_bench 25 60 120 "$(part minecraft:stick 3), $(part minecraft:string 2), $(part minecraft:iron_ingot 1), $(part minecraft:tripwire_hook 1)"
disable+=(fishing_rod bow shield crossbow)

# ---------------- Write files ----------------
{
  echo "{"
  echo "  \"values\": {"
  for i in "${!entries[@]}"; do
    if [ "$i" -lt $(( ${#entries[@]} - 1 )) ]; then echo "${entries[$i]},"; else echo "${entries[$i]}"; fi
  done
  echo "  }"
  echo "}"
} > "$OUT"

for name in "${disable[@]}"; do
  printf '{\n  "neoforge:conditions": [{ "type": "neoforge:never" }]\n}\n' > "$DISABLED/$name.json"
done

echo "Wrote ${#entries[@]} bench recipes to $OUT and disabled ${#disable[@]} vanilla recipes."
