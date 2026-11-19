package dev.hotbarrepeatfix;

public interface HotbarRepeat {
    void hotbarRepeatFix$continueThroughSpace();

    void hotbarRepeatFix$coalesceRepeat();

    void hotbarRepeatFix$clearRepeat();

    boolean hotbarRepeatFix$hasPhysicalPress();

    void hotbarRepeatFix$returnAfterTap(boolean defer);
}
