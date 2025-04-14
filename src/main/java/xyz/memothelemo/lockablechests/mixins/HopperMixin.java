package xyz.memothelemo.lockablechests.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.memothelemo.lockablechests.ModLogger;
import xyz.memothelemo.lockablechests.events.HopperEventCallback;

import java.lang.reflect.Method;

@Mixin(HopperBlockEntity.class)
public class HopperMixin {
    @Unique private static boolean HAS_ALERTED_EJECT_ERROR = false;
    @Unique private static boolean HAS_ALERTED_SUCK_ERROR = false;

    @Inject(
            method = "suckInItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onSuckingItems(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
        BlockState blockState = level.getBlockState(blockPos);
        Container container;
        try {
            Method method = HopperBlockEntity.class.getDeclaredMethod(
                    // getInputInventory in yarn mappings
                    "method_11248",
                    Level.class,
                    Hopper.class,
                    BlockPos.class,
                    BlockState.class
            );
            method.setAccessible(true);

            container = (Container) method.invoke(
                    HopperBlockEntity.class,
                    level, hopper, blockPos, blockState
            );
        } catch (Exception e) {
            if (!HAS_ALERTED_SUCK_ERROR) {
                HAS_ALERTED_SUCK_ERROR = true;
                ModLogger.INSTANCE.warn("Cannot process hopper suck items mixin", e);
            }
            cir.setReturnValue(false);
            return;
        }
        if (container == null) return;

        InteractionResult result = HopperEventCallback.SUCK.invoker().onHopperSuckItems(level, container);
        if (result != InteractionResult.PASS)
            cir.setReturnValue(false);
    }

    @Inject(
            method = "ejectItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onEjectingItems(
            Level level,
            BlockPos pos,
            HopperBlockEntity blockEntity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Container container;
        try {
            Method method = HopperBlockEntity.class.getDeclaredMethod(
                    // getOutputInventory in yarn mappings
                    "method_11255",
                    Level.class,
                    BlockPos.class,
                    HopperBlockEntity.class
            );
            method.setAccessible(true);
            container = (Container) method.invoke(HopperBlockEntity.class, level, pos, blockEntity);
        } catch (Exception e) {
            if (!HAS_ALERTED_EJECT_ERROR) {
                HAS_ALERTED_EJECT_ERROR = true;
                ModLogger.INSTANCE.warn("Cannot process hopper eject items mixin", e);
            }
            cir.setReturnValue(false);
            return;
        }
        if (container == null) return;

        InteractionResult result = HopperEventCallback.EJECT.invoker().onHopperEjectItems(level, container);
        if (result != InteractionResult.PASS)
            cir.setReturnValue(false);
    }
}
