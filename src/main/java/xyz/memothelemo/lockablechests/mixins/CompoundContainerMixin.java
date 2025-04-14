package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.memothelemo.lockablechests.interfaces.CompoundContainerAccessor;

@Mixin(CompoundContainer.class)
public class CompoundContainerMixin implements CompoundContainerAccessor {
    @Shadow @Final private Container container1;
    @Shadow @Final private Container container2;

    @Override
    public @NotNull Container lc$getContainer1() {
        return this.container1;
    }

    @Override
    public @NotNull Container lc$getContainer2() {
        return this.container2;
    }
}
