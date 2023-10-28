package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.module.display.ChatSession
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.MinecraftVersion.majorLegacy
import taboolib.module.nms.PacketSendEvent

/**
 * @author Arasple
 * @date 2019/11/30 10:16
 */
@PlatformSide([Platform.BUKKIT])
object ListenerPackets {

    /**
     * 去除登录时右上角提示
     */
    @SubscribeEvent
    fun secure(e: PacketSendEvent) {
        if (majorLegacy >= 11902) {
            when (e.packet.name) {
                "ClientboundServerDataPacket" -> e.packet.write("enforcesSecureChat", true)
                "ClientboundPlayerChatHeaderPacket" -> e.isCancelled = true
            }
        }
    }

    /**
     * 记录玩家收到的消息
     */
    @SubscribeEvent
    fun recall(e: PacketSendEvent) {
        val session = ChatSession.sessions[e.player.uniqueId] ?: return
        when (e.packet.name) {
            "ClientboundSystemChatPacket" -> {
                session.addMessage(e.packet)
            }
            "PacketPlayOutChat" -> {
                val type = if (majorLegacy >= 11700) {
                    e.packet.read<Byte>("type/index")!!
                } else if (majorLegacy >= 11200) {
                    e.packet.read<Byte>("b/d")!!
                } else {
                    e.packet.read<Byte>("b")
                }
                if (type != 0.toByte()) {
                    return
                }
                session.addMessage(e.packet)
            }
        }
    }
}