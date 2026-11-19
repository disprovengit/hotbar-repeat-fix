package dev.hotbarrepeatfix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class RepeatSettingsScreen extends Screen {
    private final Screen parent;

    public RepeatSettingsScreen(Screen parent) {
        super(Component.literal("Hotbar Repeat Fix"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(toggleText(), button -> {
            RepeatConfig.setEnabled(!RepeatConfig.isEnabled());
            button.setMessage(toggleText());
        }).bounds(width / 2 - 100, height / 2 - 10, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(width / 2 - 100, height - 40, 200, 20).build());
    }

    private static Component toggleText() {
        return Component.literal(RepeatConfig.isEnabled() ? "Mod On" : "Mod Off");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 30, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
