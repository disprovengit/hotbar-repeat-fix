package dev.hotbarrepeatfix;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class RepeatSettingsScreen extends Screen {
    private final Screen parent;

    public RepeatSettingsScreen(Screen parent) {
        super(Text.literal("Hotbar Repeat Fix"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(toggleText(), button -> {
            RepeatConfig.setEnabled(!RepeatConfig.isEnabled());
            button.setMessage(toggleText());
        }).dimensions(width / 2 - 100, height / 2 - 10, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
            .dimensions(width / 2 - 100, height - 40, 200, 20).build());
    }

    private static Text toggleText() {
        return Text.literal(RepeatConfig.isEnabled() ? "Mod On" : "Mod Off");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 30, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
