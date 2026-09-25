package smartin.tetraticcombat.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smartin.tetraticcombat.ForgeConfigHolder;

@Mixin({ItemInHandRenderer.class})
public class ItemInHandRendererMixin {

    @Inject(
            at = @At(value = "HEAD"),
            method = "renderItem",
            require = 1
    )
    private void renderArmWithItem(LivingEntity arg, ItemStack itemstack, ItemDisplayContext arg3, boolean bl, PoseStack poseStack, MultiBufferSource arg5, int i, CallbackInfo ci){
        if(!ForgeConfigHolder.COMMON.enableRescale.get()) return;
        CustomData customData = itemstack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return;
        CompoundTag tag = customData.getUnsafe();
        float xScale = tag.contains("tetraticScaleX") ? tag.getFloat("tetraticScaleX"):1.0f;
        float yScale = tag.contains("tetraticScaleY") ? tag.getFloat("tetraticScaleY"):1.0f;
        float zScale = tag.contains("tetraticScaleZ") ? tag.getFloat("tetraticScaleZ"):1.0f;
        double xTranslate = tag.contains("tetraticTranslateX") ? tag.getFloat("tetraticTranslateX"):0.0d;
        double yTranslate = tag.contains("tetraticTranslateY") ? tag.getFloat("tetraticTranslateY"):0.0d;
        double zTranslate = tag.contains("tetraticTranslateZ") ? tag.getFloat("tetraticTranslateZ"):0.0d;
        poseStack.scale(xScale,yScale,zScale);
        poseStack.translate(xTranslate,yTranslate,zTranslate);
    }
}
