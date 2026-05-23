package com.github.kaspiandev.antipopup.listener;

import com.github.kaspiandev.antipopup.platform.Platform;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.chat.ChatType;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_1;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_3;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerServerData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;

public class PacketEventsListener implements PacketListener {

    private final Platform platform;

    public PacketEventsListener(Platform platform) {
        this.platform = platform;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        ClientVersion clientVersion = event.getUser().getClientVersion();
        if (packetType == PacketType.Status.Server.RESPONSE
                && clientVersion.isNewerThan(ClientVersion.V_1_18_2)) {
            WrapperStatusServerResponse wrapper = new WrapperStatusServerResponse(event);
            JsonObject newObj = wrapper.getComponent();
            newObj.addProperty("preventsChatReports", true);
            wrapper.setComponent(newObj);
        } else if (packetType == PacketType.Play.Server.SERVER_DATA
                && clientVersion.isOlderThan(ClientVersion.V_1_20_5)
                && !platform.getApConfig().isShowPopup()) {
            WrapperPlayServerServerData wrapper = new WrapperPlayServerServerData(event);
            wrapper.setEnforceSecureChat(true);
        } else if (packetType == PacketType.Play.Server.JOIN_GAME
                && clientVersion.isNewerThan(ClientVersion.V_1_20_3)
                && !platform.getApConfig().isShowPopup()) {
            WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
            wrapper.setEnforcesSecureChat(true);
        } else if (packetType == PacketType.Play.Server.CHAT_MESSAGE
                && platform.getApConfig().isBlockChatReports()) {
            sendUnsignedChatMessage(event);
        } else if (packetType == PacketType.Play.Server.PLAYER_CHAT_HEADER
                && !platform.getApConfig().isSendHeader()) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.CHAT_SESSION_UPDATE) {
            event.setCancelled(true);
        }
    }

    private void sendUnsignedChatMessage(PacketSendEvent event) {
        WrapperPlayServerChatMessage wrapper = new WrapperPlayServerChatMessage(event);
        ChatMessage message = wrapper.getMessage();
        Component content = getUnsignedContent(message);
        ChatType.Bound chatType = getChatType(message);

        event.setCancelled(true);
        event.getUser().sendPacket(new WrapperPlayServerSystemChatMessage(false,
                chatType.getType().getChatDecoration().decorate(content, chatType)));
    }

    private Component getUnsignedContent(ChatMessage message) {
        if (message instanceof ChatMessage_v1_19_3 modernMessage) {
            return modernMessage.getUnsignedChatContent()
                    .orElseGet(() -> Component.text(modernMessage.getPlainContent()));
        }
        if (message instanceof ChatMessage_v1_19_1 legacyMessage) {
            Component unsignedContent = legacyMessage.getUnsignedChatContent();
            return unsignedContent == null ? Component.text(legacyMessage.getPlainContent()) : unsignedContent;
        }
        return message.getChatContent();
    }

    private ChatType.Bound getChatType(ChatMessage message) {
        if (message instanceof ChatMessage_v1_19_3 modernMessage) {
            return modernMessage.getChatFormatting();
        }
        if (message instanceof ChatMessage_v1_19_1 legacyMessage) {
            return legacyMessage.getChatFormatting();
        }
        throw new IllegalStateException("Unsupported chat message type: " + message.getClass().getName());
    }

}
