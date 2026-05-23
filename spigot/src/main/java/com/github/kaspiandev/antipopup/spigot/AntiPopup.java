package com.github.kaspiandev.antipopup.spigot;

import com.github.kaspiandev.antipopup.config.APConfig;
import com.github.kaspiandev.antipopup.listener.PacketEventsListener;
import com.github.kaspiandev.antipopup.log.LogFilter;
import com.github.kaspiandev.antipopup.message.ConsoleMessages;
import com.github.kaspiandev.antipopup.spigot.api.Api;
import com.github.kaspiandev.antipopup.spigot.hook.HookManager;
import com.github.kaspiandev.antipopup.spigot.hook.viaversion.ViaVersionHook;
import com.github.kaspiandev.antipopup.spigot.hook.viaversion.Via_1_19_to_1_19_1;
import com.github.kaspiandev.antipopup.spigot.hook.viaversion.Via_1_20_4_to_1_20_5;
import com.github.kaspiandev.antipopup.spigot.listeners.ChatListener;
import com.github.kaspiandev.antipopup.spigot.platform.SpigotPlatform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.tcoded.folialib.FoliaLib;
import org.apache.logging.log4j.LogManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class AntiPopup extends JavaPlugin {

    private static AntiPopup instance;
    private static File propertiesFile;
    private static APConfig config;
    private static FoliaLib foliaLib;

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public static File getPropertiesFile() {
        return propertiesFile;
    }

    public static AntiPopup getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        foliaLib = new FoliaLib(this);
        try {
            config = new APConfig(getDataFolder(), this.getClassLoader());
            getLogger().info("Config enabled.");
        } catch (IOException ex) {
            getLogger().warning("Config file could not be initialized.");
            throw new RuntimeException(ex);
        }
        SpigotPlatform spigotPlatform = new SpigotPlatform(config);
        propertiesFile = new File(Bukkit.getWorldContainer().getParentFile(), config.getPropertiesLocation());

        if (config.isBstats()) {
            Metrics metrics = new Metrics(this, 16308);
            metrics.addCustomChart(new SimplePie("runs_viaversion",
                    () -> Bukkit.getPluginManager().isPluginEnabled("ViaVersion") ? "Yes" : "No"));
            getLogger().info("Loaded optional metrics.");
        }

        ServerManager serverManager = PacketEvents.getAPI().getServerManager();

        HookManager hookManager = new HookManager();
        ViaVersionHook viaVersionHook = new ViaVersionHook();
        viaVersionHook.addModifier(Via_1_19_to_1_19_1.class);
        viaVersionHook.addModifier(Via_1_20_4_to_1_20_5.class);
        hookManager.addHook(viaVersionHook);
        hookManager.load();

        if (config.isClickableUrls()) {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
            getLogger().info("Enabled URL support.");
        }

        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(spigotPlatform), PacketListenerPriority.LOW);

        if (config.isBlockChatReports()
                && serverManager.getVersion().isOlderThan(ServerVersion.V_1_19_1)) {
            config.setBlockChatReports(false);
            ConsoleMessages.log(ConsoleMessages.BLOCKING_REPORTS_UNSUPPORTED, getLogger()::severe);
            throw new IllegalStateException("Blocking chat reports was enabled but your server version isn't supported!");
        }

        Objects.requireNonNull(this.getCommand("antipopup")).setExecutor(new CommandRegister(config));
        getLogger().info("Commands registered.");

        if (config.isFilterNotSecure()) {
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addFilter(new LogFilter());
            getLogger().info("Logger filter enabled.");
        }

        foliaLib.getImpl().runLater(() -> {
            if (config.isAutoSetup()) Api.setupAntiPopup(80, true);
            if (config.isFirstRun()) {
                try {
                    FileInputStream in = new FileInputStream(propertiesFile);
                    Properties props = new Properties();
                    props.load(in);
                    if (Boolean.parseBoolean(props.getProperty("enforce-secure-profile"))) {
                        ConsoleMessages.log(ConsoleMessages.ASK_SETUP, getLogger()::warning);
                    }
                    in.close();
                    config.setFirstRun(false);
                    config.save();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (config.isAskBstats()) {
                ConsoleMessages.log(ConsoleMessages.ASK_BSTATS, getLogger()::warning);
                config.setAskBstats(false);
                config.save();
            }
        }, 5 * 50L, TimeUnit.MILLISECONDS);
    }
}
