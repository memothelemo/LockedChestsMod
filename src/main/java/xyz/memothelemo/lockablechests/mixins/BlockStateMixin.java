package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.memothelemo.lockablechests.events.BlockExplodeCallback;

import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateMixin {
    @Shadow protected abstract BlockState asState();

    @Inject(method = "onExplosionHit", at = @At("HEAD"), cancellable = true)
    public void shouldReallyExplodeBlock(
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> dropConsumer,
            CallbackInfo ci
    ) {
        InteractionResult result = BlockExplodeCallback.EVENT.invoker()
                .shouldExplodeBlock(explosion.level(), this.asState(), pos);

        if (result != InteractionResult.PASS) ci.cancel();
    }
}
