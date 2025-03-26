package xyz.memothelemo.lockedchests.mixins;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.memothelemo.lockedchests.events.OnChestAffectedByExplosion;

import java.util.List;

@Mixin(ExplosionImpl.class)
public class ExplosionMixin {
    @Shadow @Final private ServerWorld world;

    @Inject(method = "getBlocksToDestroy", at = @At("RETURN"))
    public void removeSomeAffectedBlocks(CallbackInfoReturnable<List<BlockPos>> cir) {
        // It is safe to assume that world is not null since explode()
        // assumes that it exists in the first place.
        List<BlockPos> affectedBlocks = cir.getReturnValue();

        // Maybe we have thousands of blocks to destroy, it will be wasteful
        // to clone everything.
        for (BlockPos affectedBlock : affectedBlocks) {
            BlockEntity entity = this.world.getBlockEntity(affectedBlock);
            if (!(entity instanceof ChestBlockEntity chest)) continue;

            ActionResult result = OnChestAffectedByExplosion.EVENT.invoker().interact(world, chest);
            if (result == ActionResult.FAIL) {
                affectedBlocks.remove(affectedBlock);
            }
        }
    }
}
