package com.github.kaspiandev.antipopup.velocity;

import com.github.kaspiandev.antipopup.config.APConfig;
import com.github.kaspiandev.antipopup.listener.PacketEventsListener;
import com.github.kaspiandev.antipopup.velocity.platform.VelocityPlatform;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "antipopup",
        name = "AntiPopup",
        authors = "KaspianDev",
        version = BuildConstants.VERSION,
        dependencies = {@Dependency(id = "packetevents")}
)
public class AntiPopup {

    private final ProxyServer server;
    private final APConfig config;

    @Inject
    public AntiPopup(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) throws IOException {
        this.server = server;
        this.config = new APConfig(dataDirectory.toFile(), this.getClass().getClassLoader());
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        VelocityPlatform platform = new VelocityPlatform(server, config);

        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(platform), PacketListenerPriority.LOW);
    }


}
