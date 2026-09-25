package smartin.tetraticcombat.mixin;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smartin.tetraticcombat.ItemResolver.Resolver;

@Mixin(value = WeaponRegistry.class, remap = false)
public class WeaponRegistryMixin {
    @Inject(
            method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void customizeTetraAttributes(ItemStack stack, CallbackInfoReturnable<WeaponAttributes> callback) {
        callback.setReturnValue(Resolver.customizeWeaponAttributes(stack, callback.getReturnValue()));
    }
}
