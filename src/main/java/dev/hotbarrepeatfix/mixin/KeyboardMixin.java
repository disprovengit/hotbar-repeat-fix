package dev.hotbarrepeatfix.mixin;

import dev.hotbarrepeatfix.HotbarRepeat;
import dev.hotbarrepeatfix.RepeatConfig;
import java.util.ArrayList;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;
    @Unique private final ArrayList<KeyBinding> hotbarRepeatFix$heldKeys = new ArrayList<>();
    @Unique private boolean hotbarRepeatFix$spaceInterrupted;

    @Inject(method = "onKey", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/option/KeyBinding;onKeyPressed(Lnet/minecraft/client/util/InputUtil$Key;)V",
        shift = At.Shift.AFTER))
    private void hotbarRepeatFix$handlePress(long window, int action, KeyInput input, CallbackInfo ci) {
        if (!RepeatConfig.isEnabled()) {
            hotbarRepeatFix$heldKeys.clear();
            hotbarRepeatFix$spaceInterrupted = false;
            return;
        }
        if (action == GLFW.GLFW_PRESS && input.key() == GLFW.GLFW_KEY_SPACE) {
            hotbarRepeatFix$heldKeys.removeIf(key -> !key.isPressed());
            if (!hotbarRepeatFix$heldKeys.isEmpty()) {
                hotbarRepeatFix$spaceInterrupted = true;
                ((HotbarRepeat) hotbarRepeatFix$heldKeys.getLast()).hotbarRepeatFix$continueThroughSpace();
            }
        }
        for (KeyBinding binding : client.options.hotbarKeys) {
            if (!binding.matchesKey(input)) {
                continue;
            }
            if (action == GLFW.GLFW_REPEAT) {
                ((HotbarRepeat) binding).hotbarRepeatFix$coalesceRepeat();
            } else if (action == GLFW.GLFW_PRESS) {
                hotbarRepeatFix$heldKeys.removeIf(key -> !key.isPressed());
                if (hotbarRepeatFix$heldKeys.isEmpty()) {
                    hotbarRepeatFix$spaceInterrupted = false;
                }
                for (KeyBinding held : hotbarRepeatFix$heldKeys) {
                    ((HotbarRepeat) held).hotbarRepeatFix$clearRepeat();
                }
                hotbarRepeatFix$heldKeys.remove(binding);
                hotbarRepeatFix$heldKeys.add(binding);
            }
        }
    }

    @Inject(method = "onKey", at = @At("HEAD"))
    private void hotbarRepeatFix$releaseRepeat(long window, int action, KeyInput input, CallbackInfo ci) {
        if (!RepeatConfig.isEnabled()) {
            hotbarRepeatFix$heldKeys.clear();
            hotbarRepeatFix$spaceInterrupted = false;
            return;
        }
        if (action != GLFW.GLFW_RELEASE || window != client.getWindow().getHandle()) {
            return;
        }
        hotbarRepeatFix$heldKeys.removeIf(key -> !key.isPressed());
        boolean releasedLatest = !hotbarRepeatFix$heldKeys.isEmpty()
            && hotbarRepeatFix$heldKeys.getLast().matchesKey(input);
        boolean tapPending = false;
        for (KeyBinding binding : client.options.hotbarKeys) {
            if (binding.matchesKey(input)) {
                HotbarRepeat repeat = (HotbarRepeat) binding;
                tapPending |= repeat.hotbarRepeatFix$hasPhysicalPress();
                repeat.hotbarRepeatFix$clearRepeat();
                hotbarRepeatFix$heldKeys.remove(binding);
            }
        }
        if (releasedLatest && !hotbarRepeatFix$heldKeys.isEmpty() && client.currentScreen == null
            && (client.player == null || (!client.player.isSpectator() && (!client.player.isInCreativeMode()
                || (!client.options.saveToolbarActivatorKey.isPressed()
                    && !client.options.loadToolbarActivatorKey.isPressed()))))) {
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
