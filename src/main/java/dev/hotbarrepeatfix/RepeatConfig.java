package dev.hotbarrepeatfix;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public final class RepeatConfig {
    private static final System.Logger LOGGER = System.getLogger("Hotbar Repeat Fix");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hotbar-repeat-fix.properties");
    private static boolean enabled = load();

    private RepeatConfig() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        // Discard pending custom events when either direction is toggled.
        for (KeyBinding binding : MinecraftClient.getInstance().options.hotbarKeys) {
            ((HotbarRepeat) binding).hotbarRepeatFix$clearRepeat();
        }
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(temporary, "enabled=" + enabled + "\n");
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not save hotbar repeat setting", e);
        }
    }

    private static boolean load() {
        if (!Files.exists(FILE)) {
            return true;
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            return !"false".equalsIgnoreCase(properties.getProperty("enabled", "true").trim());
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not read hotbar repeat setting", e);
            return true;
        }
    }
}
