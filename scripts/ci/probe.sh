#!/usr/bin/env bash
# TEMPORARY (removed before merge): prints Minecraft API sources the dev container can't download.
set -u
SRC=$(find build/moddev/artifacts -name 'minecraft-patched-*-sources.jar' | head -1)
echo "SOURCES JAR: $SRC"; ls build/moddev/artifacts
mkdir -p /tmp/mc && (cd /tmp/mc && jar xf "$GITHUB_WORKSPACE/$SRC")
for j in build/moddev/artifacts/*.jar; do echo "== $j"; done
show() { for n in "$@"; do f=$(find /tmp/mc -name "$n.java" | head -1); echo "=================== FILE $n ($f)"; [ -n "$f" ] && grep -v '^import ' "$f"; done; }
sig() { for n in "$@"; do f=$(find /tmp/mc -name "$n.java" | head -1); echo "=================== SIG $n ($f)"; [ -n "$f" ] && grep -nE '^\s*(public|protected|static|private)?\s*(abstract |static |final |default )*[A-Za-z<>\[\], ?.]+ [a-zA-Z]+\(.*\)?\s*\{?$|^\s*(public |protected )?(abstract |static |final )*(class|record|interface|enum) ' "$f" | grep -v ' private ' | head -400; done; }
show AnimationState KeyframeAnimation AnimationDefinition TeleportTransition SavedDataType FunctionGameTestInstance TestData EyesLayer ThrownItemRenderer ArrowRenderer TippableArrowRenderer SpectralArrowRenderer SpectralArrow SpectralArrowItem ArrowItem ServerBossEvent ToolMaterial ArmorMaterial ArmorMaterials EquipmentAssets ArmorType AttributeModifier BreezeModel BreezeRenderer CreakingModel CreakingRenderer IronGolemModel LivingEntityRenderState LivingEntityRenderer MobRenderer ThrowableItemProjectile Snowball SmallFireball EntitySpawnReason
sig Mob LivingEntity Entity Item ItemStack Items EntityType GameTestHelper ServerPlayer ServerLevel Level Player AbstractArrow Projectile PathfinderMob Monster BossEvent AttributeInstance Attributes SavedData LootParams LootTable LootContextParamSets ReloadableServerRegistries MinecraftServer DimensionType ServerBossEvent Explosion EntityRenderer RenderTypes RenderType EntityModel ModelPart PartDefinition CubeListBuilder PartPose LayerDefinition MeshDefinition CubeDeformation WitherBoss Warden SynchedEntityData EntityDataSerializers DamageSource DamageSources BlockBehaviour RenderLayer SubmitNodeCollector
for f in $(cd /tmp/mc && find . -path '*Item.java' -path '*world/item/*' | head -0); do :; done
echo "=================== Item.Properties"; f=$(find /tmp/mc -path '*world/item/Item.java'); awk '/class Properties/,0' "$f" | grep -nE 'public .*\(' | head -120
echo "=================== Items spear/mace"; grep -nE 'SPEAR|MACE|_SWORD =|_AXE =|CHAINMAIL|COPPER_HELMET|ARROW' $(find /tmp/mc -path '*world/item/Items.java') | head -60
echo "=================== WardenModel/Renderer"; show WardenModel WardenRenderer WardenEmissiveLayer WardenRenderState
echo "=================== DATA FILES"
mkdir -p /tmp/res
for j in build/moddev/artifacts/*.jar; do case "$j" in *sources*) ;; *) (cd /tmp/res && jar xf "$GITHUB_WORKSPACE/$j" data assets 2>/dev/null) ;; esac; done
ls /tmp/res/data/minecraft | head -80
for p in data/minecraft/dimension_type/the_end.json data/minecraft/dimension_type/overworld.json data/minecraft/worldgen/world_preset/flat.json data/minecraft/worldgen/flat_level_generator_preset/the_void.json assets/minecraft/equipment/iron.json assets/minecraft/equipment/chainmail.json assets/minecraft/items/iron_sword.json assets/minecraft/models/item/iron_sword.json assets/minecraft/items/bow.json assets/minecraft/items/iron_spear.json assets/minecraft/models/item/iron_spear.json assets/minecraft/items/iron_chestplate.json data/minecraft/loot_table/entities/wither_skeleton.json; do echo "--- $p"; cat /tmp/res/$p; echo; done
ls /tmp/res/data/minecraft/timeline 2>/dev/null; for t in /tmp/res/data/minecraft/timeline/*.json; do echo "--- $t"; head -c 1500 $t; echo; done
ls /tmp/res/assets/minecraft/textures/entity/equipment/ 2>/dev/null; ls /tmp/res/assets/minecraft/textures/entity/equipment/humanoid | head -40
