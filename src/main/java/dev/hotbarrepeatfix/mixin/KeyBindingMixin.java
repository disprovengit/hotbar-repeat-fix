package dev.hotbarrepeatfix.mixin;

import dev.hotbarrepeatfix.HotbarRepeat;
import dev.hotbarrepeatfix.RepeatConfig;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
abstract class KeyBindingMixin implements HotbarRepeat {
    @Shadow private int timesPressed;
    @Shadow private boolean pressed;
    @Unique private boolean hotbarRepeatFix$pendingRepeat;
    @Unique private boolean hotbarRepeatFix$deferredReturn;
    @Unique private boolean hotbarRepeatFix$spaceRepeat;
    @Unique private long hotbarRepeatFix$lastNativeRepeat;
    @Unique private long hotbarRepeatFix$repeatInterval = 33_333_333L;
    @Unique private long hotbarRepeatFix$nextRepeat;

    @Override
    public void hotbarRepeatFix$continueThroughSpace() {
        hotbarRepeatFix$spaceRepeat = true;
        hotbarRepeatFix$nextRepeat = System.nanoTime() + hotbarRepeatFix$repeatInterval;
    }

    @Override
    public void hotbarRepeatFix$coalesceRepeat() {
        // Vanilla just counted this event. Move it out of the queue of physical presses.
        if (timesPressed > 0) {
            timesPressed--;
            hotbarRepeatFix$pendingRepeat = !hotbarRepeatFix$deferredReturn;
            long now = System.nanoTime();
            long interval = now - hotbarRepeatFix$lastNativeRepeat;
            if (hotbarRepeatFix$lastNativeRepeat != 0 && interval >= 5_000_000L && interval <= 200_000_000L) {
                hotbarRepeatFix$repeatInterval = interval;
            }
            hotbarRepeatFix$lastNativeRepeat = now;
            hotbarRepeatFix$nextRepeat = now + hotbarRepeatFix$repeatInterval;
        }
    }

    @Override
    public void hotbarRepeatFix$clearRepeat() {
        hotbarRepeatFix$pendingRepeat = false;
        hotbarRepeatFix$deferredReturn = false;
        hotbarRepeatFix$spaceRepeat = false;
        hotbarRepeatFix$lastNativeRepeat = 0;
    }

    @Override
    public boolean hotbarRepeatFix$hasPhysicalPress() {
        return timesPressed > 0;
    }

    @Override
    public void hotbarRepeatFix$returnAfterTap(boolean defer) {
        hotbarRepeatFix$pendingRepeat = !defer;
        hotbarRepeatFix$deferredReturn = defer;
    }

    @Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
    private void hotbarRepeatFix$consumeRepeat(CallbackInfoReturnable<Boolean> cir) {
        if (!RepeatConfig.isEnabled()) {
            hotbarRepeatFix$clearRepeat();
            return;
        }
        if (hotbarRepeatFix$deferredReturn) {
            // Let the tapped slot be processed before returning on the next input pass.
            hotbarRepeatFix$deferredReturn = false;
            hotbarRepeatFix$pendingRepeat = true;
            return;
        } else if (hotbarRepeatFix$pendingRepeat) {
            hotbarRepeatFix$pendingRepeat = false;
            cir.setReturnValue(true);
        }
        if (hotbarRepeatFix$spaceRepeat && pressed) {
            long now = System.nanoTime();
            if (cir.getReturnValueZ() || now >= hotbarRepeatFix$nextRepeat) {
                // Schedule from now so a stalled frame never creates a backlog.
                hotbarRepeatFix$nextRepeat = now + hotbarRepeatFix$repeatInterval;
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void hotbarRepeatFix$resetRepeat(CallbackInfo ci) {
        hotbarRepeatFix$clearRepeat();
    }
}
