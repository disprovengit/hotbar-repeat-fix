package dev.hotbarrepeatfix.mixin;

import dev.hotbarrepeatfix.HotbarRepeat;
import dev.hotbarrepeatfix.RepeatConfig;
import java.util.ArrayList;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, remap = false)
abstract class KeyboardMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private final ArrayList<KeyMapping> hotbarRepeatFix$heldKeys = new ArrayList<>();
    @Unique private boolean hotbarRepeatFix$spaceInterrupted;

    @Inject(method = "keyPress", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
        shift = At.Shift.AFTER))
    private void hotbarRepeatFix$handlePress(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (!RepeatConfig.isEnabled()) {
            hotbarRepeatFix$heldKeys.clear();
            hotbarRepeatFix$spaceInterrupted = false;
            return;
        }
        if (action == InputConstants.PRESS && input.key() == InputConstants.KEY_SPACE) {
            hotbarRepeatFix$heldKeys.removeIf(key -> !key.isDown());
            if (!hotbarRepeatFix$heldKeys.isEmpty()) {
                hotbarRepeatFix$spaceInterrupted = true;
                ((HotbarRepeat) hotbarRepeatFix$heldKeys.getLast()).hotbarRepeatFix$continueThroughSpace();
            }
        }
        for (KeyMapping binding : minecraft.options.keyHotbarSlots) {
            if (!binding.matches(input)) {
                continue;
            }
            if (action == InputConstants.REPEAT) {
                ((HotbarRepeat) binding).hotbarRepeatFix$coalesceRepeat();
            } else if (action == InputConstants.PRESS) {
                hotbarRepeatFix$heldKeys.removeIf(key -> !key.isDown());
                if (hotbarRepeatFix$heldKeys.isEmpty()) {
                    hotbarRepeatFix$spaceInterrupted = false;
                }
                for (KeyMapping held : hotbarRepeatFix$heldKeys) {
                    ((HotbarRepeat) held).hotbarRepeatFix$clearRepeat();
                }
                hotbarRepeatFix$heldKeys.remove(binding);
                hotbarRepeatFix$heldKeys.add(binding);
            }
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void hotbarRepeatFix$releaseRepeat(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (!RepeatConfig.isEnabled()) {
            hotbarRepeatFix$heldKeys.clear();
            hotbarRepeatFix$spaceInterrupted = false;
            return;
        }
        if (action != InputConstants.RELEASE || window != minecraft.getWindow().handle()) {
            return;
        }
        hotbarRepeatFix$heldKeys.removeIf(key -> !key.isDown());
        boolean releasedLatest = !hotbarRepeatFix$heldKeys.isEmpty()
            && hotbarRepeatFix$heldKeys.getLast().matches(input);
        boolean tapPending = false;
        for (KeyMapping binding : minecraft.options.keyHotbarSlots) {
            if (binding.matches(input)) {
                HotbarRepeat repeat = (HotbarRepeat) binding;
                tapPending |= repeat.hotbarRepeatFix$hasPhysicalPress();
                repeat.hotbarRepeatFix$clearRepeat();
                hotbarRepeatFix$heldKeys.remove(binding);
            }
        }
        if (releasedLatest && !hotbarRepeatFix$heldKeys.isEmpty() && minecraft.gui.screen() == null
            && (minecraft.player == null || (!minecraft.player.isSpectator() && (!minecraft.player.isCreative()
                || (!minecraft.options.keySaveHotbarActivator.isDown()
                    && !minecraft.options.keyLoadHotbarActivator.isDown()))))) {
            // The OS may have stopped sending repeats for the older key.
            if (hotbarRepeatFix$spaceInterrupted) {
                ((HotbarRepeat) hotbarRepeatFix$heldKeys.getLast()).hotbarRepeatFix$continueThroughSpace();
            }
            ((HotbarRepeat) hotbarRepeatFix$heldKeys.getLast()).hotbarRepeatFix$returnAfterTap(tapPending);
        }
        if (hotbarRepeatFix$heldKeys.isEmpty()) {
            hotbarRepeatFix$spaceInterrupted = false;
        }
    }
}
