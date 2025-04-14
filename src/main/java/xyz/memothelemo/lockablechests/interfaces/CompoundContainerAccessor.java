package xyz.memothelemo.lockablechests.interfaces;

import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface CompoundContainerAccessor {
    @NotNull Container lc$getContainer1();
    @NotNull Container lc$getContainer2();
}
