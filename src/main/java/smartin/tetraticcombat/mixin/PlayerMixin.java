package smartin.tetraticcombat.mixin;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smartin.tetraticcombat.ForgeConfigHolder;
import smartin.tetraticcombat.ItemResolver.Resolver;

import java.util.HashMap;
import java.util.Map;

@Mixin({Player.class})
public class PlayerMixin {
    @Unique
    private static final Map<Player, ItemStack> playerItemStackMap = new HashMap<>();
    @Inject(
            at = @At(value = "HEAD"),
            method = "tick()V",
            remap = true,
            require = 1
    )
    private void tick(CallbackInfo ci){
        if(!ForgeConfigHolder.COMMON.playerMixin.get()) return;
        Player p = (Player)(Object) this;
        ItemStack handStack = playerItemStackMap.get(p);
        if(handStack==null || !ItemStack.matches(p.getMainHandItem(), handStack)){
            Resolver.generateBetterCombatNBT(p.getMainHandItem(),false);
            playerItemStackMap.put(p,p.getMainHandItem().copy());
        }
    }
}
