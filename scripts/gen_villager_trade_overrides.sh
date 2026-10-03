#!/usr/bin/env bash
# Removes villager / wandering trader trades that SELL station gear or ingots, by overriding the
# vanilla villager_trade tag lists ("replace": true) without those entries. Villagers still roll
# their normal number of trades, just from the remaining pool.
#
# Gear = every item in data_maps/item/bench_recipes.json, plus smelted ingots/scrap.
# Needs the patched Minecraft jar, which exists after running ./gradlew build once.
# Usage: bash scripts/gen_villager_trade_overrides.sh   (from the project root)
set -euo pipefail

JAR=$(ls build/moddev/artifacts/minecraft-patched-*-merged.jar | head -1)
WORK=build/tradework
rm -rf "$WORK" && mkdir -p "$WORK"
trap 'rm -rf "$WORK"' EXIT
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}jar"
(cd "$WORK" && "$JAVA_BIN" xf "$OLDPWD/$JAR" data/minecraft/villager_trade data/minecraft/tags/villager_trade)

# Items that must not be sold
BANNED=$(grep -oE '"minecraft:[a-z_]+":' src/main/resources/data/skillcraft/data_maps/item/bench_recipes.json | tr -d '":' | sed 's/^minecraft/minecraft:/')
BANNED="$BANNED minecraft:copper_ingot minecraft:iron_ingot minecraft:gold_ingot minecraft:netherite_scrap"

# Trade ids whose "gives" item is banned
removed=()
while IFS= read -r file; do
  gives=$(awk '/"gives"/{f=1} f && /"id"/{gsub(/[",]/,""); print $2; exit}' "$file")
  for item in $BANNED; do
    if [ "$gives" = "$item" ]; then
      rel=${file#"$WORK/data/minecraft/villager_trade/"}
      removed+=("minecraft:${rel%.json}")
      break
    fi
  done
done < <(find "$WORK/data/minecraft/villager_trade" -name '*.json')

OUT=src/main/resources/data/minecraft/tags/villager_trade
rm -rf "$OUT"
count=0
while IFS= read -r tag; do
  rel=${tag#"$WORK/data/minecraft/tags/villager_trade/"}
  changed=0
  values=()
  while IFS= read -r value; do
    skip=0
    for r in "${removed[@]}"; do [ "$value" = "$r" ] && skip=1 && changed=1 && break; done
    [ $skip -eq 0 ] && values+=("$value")
  done < <(grep -oE '"#?minecraft:[a-z0-9_/]+"' "$tag" | tr -d '"')
  [ $changed -eq 0 ] && continue
  mkdir -p "$OUT/$(dirname "$rel")"
  {
    echo "{"
    echo "  \"replace\": true,"
    echo "  \"values\": ["
    for i in "${!values[@]}"; do
      sep=","; [ "$i" -eq $(( ${#values[@]} - 1 )) ] && sep=""
      echo "    \"${values[$i]}\"$sep"
    done
    echo "  ]"
    echo "}"
  } > "$OUT/$rel"
  count=$((count + 1))
done < <(find "$WORK/data/minecraft/tags/villager_trade" -name '*.json')

echo "Removed ${#removed[@]} gear/ingot trades across $count trade lists:"
printf '  %s\n' "${removed[@]}"
