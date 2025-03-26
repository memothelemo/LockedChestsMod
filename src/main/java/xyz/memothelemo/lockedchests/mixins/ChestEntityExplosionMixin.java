package xyz.memothelemo.lockedchests.mixins;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import xyz.memothelemo.lockedchests.events.ModifableImmuneToExplosion;
import xyz.memothelemo.lockedchests.events.OnChestAffectedByExplosion;

@Mixin(ChestBlockEntity.class)
public class ChestEntityExplosionMixin implements ModifableImmuneToExplosion {
    @Override
    public boolean isImmuneToExplosion() {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        if (!chest.hasWorld()) return false;

        World world = chest.getWorld();
        ActionResult result = OnChestAffectedByExplosion.EVENT.invoker().interact(world, chest);

        return result != ActionResult.FAIL;
    }
}
