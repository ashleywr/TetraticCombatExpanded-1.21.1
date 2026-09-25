package smartin.tetraticcombat.ItemResolver;

import com.google.gson.Gson;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.component.BetterCombatDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.ModularItem;
import se.mickelus.tetra.util.ItemStackTagHelper;
import smartin.tetraticcombat.ForgeConfigHolder;
import smartin.tetraticcombat.TetraticCombat;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class Resolver {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SCALE_X = "tetraticScaleX";
    private static final String SCALE_Y = "tetraticScaleY";
    private static final String SCALE_Z = "tetraticScaleZ";
    private static final String TRANSLATE_X = "tetraticTranslateX";
    private static final String TRANSLATE_Y = "tetraticTranslateY";
    private static final String TRANSLATE_Z = "tetraticTranslateZ";
    private static final Map<ItemStack, CachedAttributes> ATTRIBUTE_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static JSONFormat weaponConfig;

    public static void reload(JSONFormat config) {
        weaponConfig = config;
        ATTRIBUTE_CACHE.clear();
    }

    public static void readConfig(String config) {
        LOGGER.info(config);
        weaponConfig = new Gson().fromJson(config, JSONFormat.class);
    }

    public static ExpandedContainer findWeaponByNBT(ItemStack stack) {
        if (weaponConfig == null || weaponConfig.attributemap == null) {
            return null;
        }

        CompoundTag tag = ItemStackTagHelper.getTag(stack);
        if (tag == null) {
            return null;
        }

        for (String key : tag.getAllKeys()) {
            Map<String, Condition> values = weaponConfig.attributemap.get(key);
            if (values != null) {
                Condition condition = values.get(tag.getString(key));
                if (condition != null) {
                    return condition.resolve(stack);
                }
            }
        }
        return null;
    }

    public static ItemStack generateBetterCombatNBT(ItemStack itemStack) {
        return generateBetterCombatNBT(itemStack, false);
    }

    public static void resetBetterCombatNBT(ItemStack itemStack) {
        itemStack.remove(BetterCombatDataComponents.WEAPON_PRESET_ID);
        if (ItemStackTagHelper.hasTag(itemStack)) {
            ItemStackTagHelper.mutate(itemStack, tag -> {
                tag.remove(SCALE_X);
                tag.remove(SCALE_Y);
                tag.remove(SCALE_Z);
                tag.remove(TRANSLATE_X);
                tag.remove(TRANSLATE_Y);
                tag.remove(TRANSLATE_Z);
            });
        }
    }

    public static ItemStack generateBetterCombatNBT(ItemStack itemStack, boolean force) {
        ExpandedContainer container = findWeaponByNBT(itemStack);
        if (container == null || container.attributes == null || container.attributes.parent() == null) {
            return itemStack;
        }

        try {
            if (force) {
                resetBetterCombatNBT(itemStack);
            }

            ResourceLocation preset = ResourceLocation.parse(container.attributes.parent());
            itemStack.set(BetterCombatDataComponents.WEAPON_PRESET_ID, preset);
            applyTransforms(itemStack, container);
        } catch (Exception exception) {
            TetraticCombat.LOGGER.warn("Could not apply Better Combat attributes to {}", itemStack, exception);
        }
        return itemStack;
    }

    /**
     * Better Combat 2.x stores a preset id on the stack instead of serializing full weapon attributes.
     * The preset supplies the animation, while this hook reapplies Tetra's per-stack range and quickness.
     */
    public static WeaponAttributes customizeWeaponAttributes(ItemStack stack, WeaponAttributes attributes) {
        if (attributes == null || !(stack.getItem() instanceof ModularItem item)) {
            return attributes;
        }

        boolean useTetraRange = ForgeConfigHolder.COMMON.enableTetraRange.get();
        boolean useRangeFallback = ForgeConfigHolder.COMMON.reachFallBack.get();
        boolean rescaleQuick = ForgeConfigHolder.COMMON.quickReducesUpswing.get();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        CachedAttributes cached = ATTRIBUTE_CACHE.get(stack);
        if (cached != null && cached.matches(attributes, customData, useTetraRange, useRangeFallback, rescaleQuick)) {
            return cached.result();
        }

        WeaponAttributes.Attack[] attacks = attributes.attacks();
        if (rescaleQuick) {
            attacks = rescaleUpswing(attacks, getQuickStat(stack, item));
        }

        double range = useTetraRange
                ? getAttackRange(stack, item, useRangeFallback)
                : attributes.attackRange();

        WeaponAttributes result = new WeaponAttributes(
                range,
                attributes.rangeBonus(),
                attributes.pose(),
                attributes.offHandPose(),
                attributes.two_handed(),
                attributes.category(),
                attacks,
                attributes.trailAppearance()
        );
        ATTRIBUTE_CACHE.put(stack, new CachedAttributes(
                attributes, customData, useTetraRange, useRangeFallback, rescaleQuick, result));
        return result;
    }

    private static void applyTransforms(ItemStack stack, ExpandedContainer container) {
        ItemStackTagHelper.mutate(stack, tag -> {
            putOrRemove(tag, SCALE_X, container.scaleX, 1.0f);
            putOrRemove(tag, SCALE_Y, container.scaleY, 1.0f);
            putOrRemove(tag, SCALE_Z, container.scaleZ, 1.0f);
            putOrRemove(tag, TRANSLATE_X, container.translationX, 0.0f);
            putOrRemove(tag, TRANSLATE_Y, container.translationY, 0.0f);
            putOrRemove(tag, TRANSLATE_Z, container.translationZ, 0.0f);
        });
    }

    private static void putOrRemove(CompoundTag tag, String key, float value, float defaultValue) {
        if (Float.compare(value, defaultValue) == 0) {
            tag.remove(key);
        } else {
            tag.putFloat(key, value);
        }
    }

    private static double getQuickStat(ItemStack stack, ModularItem item) {
        var map = item.getEffectDataCached(stack).levelMap;
        if (map.containsKey(ItemEffect.quickStrike)) {
            return map.get(ItemEffect.quickStrike) * 0.05d + 0.2d;
        }
        return 0.0d;
    }

    private static WeaponAttributes.Attack[] rescaleUpswing(WeaponAttributes.Attack[] source, double scale) {
        WeaponAttributes.Attack[] result = new WeaponAttributes.Attack[source.length];
        for (int index = 0; index < source.length; index++) {
            WeaponAttributes.Attack attack = source[index];
            double upswing = Math.max(0, attack.upswing() - attack.upswing() * scale);
            result[index] = new WeaponAttributes.Attack(
                    attack.conditions(),
                    attack.hitbox(),
                    attack.damageMultiplier(),
                    attack.movementSpeedMultiplier(),
                    attack.rangeMultiplier(),
                    attack.angle(),
                    upswing,
                    attack.animation(),
                    attack.swingSound(),
                    attack.impactSound(),
                    attack.trailParticles()
            );
        }
        return result;
    }

    private static double getAttackRange(ItemStack stack, ModularItem item, boolean useRangeFallback) {
        double entityRange = item.getAttributeValue(stack, Attributes.ENTITY_INTERACTION_RANGE.value());
        if (entityRange != 0) {
            return 3.0d + entityRange;
        }
        if (useRangeFallback) {
            return 3.0d + item.getAttributeValue(stack, Attributes.BLOCK_INTERACTION_RANGE.value());
        }
        return 3.0d;
    }

    private record CachedAttributes(
            WeaponAttributes base,
            CustomData customData,
            boolean useTetraRange,
            boolean useRangeFallback,
            boolean rescaleQuick,
            WeaponAttributes result
    ) {
        private boolean matches(
                WeaponAttributes currentBase,
                CustomData currentData,
                boolean currentUseTetraRange,
                boolean currentUseRangeFallback,
                boolean currentRescaleQuick
        ) {
            return base == currentBase
                    && customData == currentData
                    && useTetraRange == currentUseTetraRange
                    && useRangeFallback == currentUseRangeFallback
                    && rescaleQuick == currentRescaleQuick;
        }
    }
}
