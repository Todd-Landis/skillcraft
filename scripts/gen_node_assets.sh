#!/usr/bin/env bash
# Generates blockstates, block models and item models for every skilling node.
# Re-run after adding a node to NodeType.java (and add a line to the NODES table below).
# Usage: bash scripts/gen_node_assets.sh   (from the project root)
set -euo pipefail

ASSETS=src/main/resources/assets/skillcraft
mkdir -p "$ASSETS/blockstates" "$ASSETS/models/block" "$ASSETS/items"

# name | side texture | top texture | depleted side | depleted top | base texture   (all minecraft:block/...)
NODES="
oak|oak_log|oak_log_top|stripped_oak_log|stripped_oak_log_top|mossy_cobblestone
birch|birch_log|birch_log_top|stripped_birch_log|stripped_birch_log_top|mossy_cobblestone
spruce|spruce_log|spruce_log_top|stripped_spruce_log|stripped_spruce_log_top|mossy_cobblestone
jungle|jungle_log|jungle_log_top|stripped_jungle_log|stripped_jungle_log_top|mossy_cobblestone
acacia|acacia_log|acacia_log_top|stripped_acacia_log|stripped_acacia_log_top|mossy_cobblestone
mangrove|mangrove_log|mangrove_log_top|stripped_mangrove_log|stripped_mangrove_log_top|mossy_cobblestone
cherry|cherry_log|cherry_log_top|stripped_cherry_log|stripped_cherry_log_top|mossy_cobblestone
dark_oak|dark_oak_log|dark_oak_log_top|stripped_dark_oak_log|stripped_dark_oak_log_top|mossy_cobblestone
pale_oak|pale_oak_log|pale_oak_log_top|stripped_pale_oak_log|stripped_pale_oak_log_top|mossy_cobblestone
crimson|crimson_stem|crimson_stem_top|stripped_crimson_stem|stripped_crimson_stem_top|blackstone
warped|warped_stem|warped_stem_top|stripped_warped_stem|stripped_warped_stem_top|blackstone
coal|coal_ore|coal_ore|cobblestone|cobblestone|mossy_cobblestone
copper|copper_ore|copper_ore|cobblestone|cobblestone|mossy_cobblestone
iron|iron_ore|iron_ore|cobblestone|cobblestone|mossy_cobblestone
quartz|nether_quartz_ore|nether_quartz_ore|netherrack|netherrack|blackstone
redstone|redstone_ore|redstone_ore|cobblestone|cobblestone|mossy_cobblestone
gold|gold_ore|gold_ore|cobblestone|cobblestone|mossy_cobblestone
lapis|lapis_ore|lapis_ore|cobblestone|cobblestone|mossy_cobblestone
diamond|diamond_ore|diamond_ore|cobblestone|cobblestone|mossy_cobblestone
emerald|emerald_ore|emerald_ore|cobblestone|cobblestone|mossy_cobblestone
ancient_debris|ancient_debris_side|ancient_debris_top|netherrack|netherrack|blackstone
ley|budding_amethyst|amethyst_block|calcite|calcite|smooth_basalt
"

model() { # parent side end base
  cat <<JSON
{
  "parent": "skillcraft:block/$1",
  "textures": {
    "side": "minecraft:block/$2",
    "end": "minecraft:block/$3",
    "base": "minecraft:block/$4"
  }
}
JSON
}

echo "$NODES" | while IFS='|' read -r name side top dside dtop base; do
  [ -z "$name" ] && continue
  id="${name}_node"
  model node_template "$side" "$top" "$base" > "$ASSETS/models/block/$id.json"
  model node_template_depleted "$dside" "$dtop" "$base" > "$ASSETS/models/block/${id}_depleted.json"
  cat > "$ASSETS/blockstates/$id.json" <<JSON
{
  "variants": {
    "depleted=false": { "model": "skillcraft:block/$id" },
    "depleted=true": { "model": "skillcraft:block/${id}_depleted" }
  }
}
JSON
  cat > "$ASSETS/items/$id.json" <<JSON
{
  "model": {
    "type": "minecraft:model",
    "model": "skillcraft:block/$id"
  }
}
JSON
  echo "generated $id"
done
