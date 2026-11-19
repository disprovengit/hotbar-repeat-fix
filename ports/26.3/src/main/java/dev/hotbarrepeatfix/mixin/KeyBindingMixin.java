package dev.hotbarrepeatfix.mixin;

import dev.hotbarrepeatfix.HotbarRepeat;
import dev.hotbarrepeatfix.RepeatConfig;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyMapping.class, remap = false)
abstract class KeyBindingMixin implements HotbarRepeat {
    @Shadow private int clickCount;
    @Shadow private boolean isDown;
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
        if (clickCount > 0) {
            clickCount--;
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
        return clickCount > 0;
    }

    @Override
    public void hotbarRepeatFix$returnAfterTap(boolean defer) {
        hotbarRepeatFix$pendingRepeat = !defer;
        hotbarRepeatFix$deferredReturn = defer;
    }

    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
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
        if (hotbarRepeatFix$spaceRepeat && isDown) {
            long now = System.nanoTime();
            if (cir.getReturnValueZ() || now >= hotbarRepeatFix$nextRepeat) {
                // Schedule from now so a stalled frame never creates a backlog.
                hotbarRepeatFix$nextRepeat = now + hotbarRepeatFix$repeatInterval;
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "release", at = @At("HEAD"))
    private void hotbarRepeatFix$resetRepeat(CallbackInfo ci) {
        hotbarRepeatFix$clearRepeat();
    }
}
