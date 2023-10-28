package me.arasple.mc.trchat.module.display

import me.arasple.mc.trchat.module.adventure.getComponent
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.channel.PrivateChannel
import me.arasple.mc.trchat.util.color.CustomColor
import me.arasple.mc.trchat.util.color.MessageColors
import me.arasple.mc.trchat.util.data
import me.arasple.mc.trchat.util.reportOnce
import org.bukkit.entity.Player
import taboolib.expansion.getDataContainer
import taboolib.module.nms.Packet
import taboolib.module.nms.sendPacket
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author ItsFlicker
 * @since 2021/12/11 22:44
 */
class ChatSession(val player: Player) {

    val receivedMessages = mutableListOf<ChatMessage>()
    var lastChannel: Channel? = null
    var lastPublicMessage = ""
    var lastPrivateMessage = ""
    var lastPrivateTo = ""
    var cancelChat = false
    var channel: String?
        private set

    init {
        val data = player.data
        channel = if (data.channel != null && Channel.channels[data.channel] != null) {
            data.channel
        } else {
            Settings.defaultChannel
        }
    }

    fun getColor(default: CustomColor?): CustomColor {
        val forces = MessageColors.getForceColors(player)
        return if (forces.isNotEmpty()) {
            CustomColor.get(forces[0])
        } else {
            val selectedColor = player.getDataContainer()["color"].takeIf { it != "null" }
            if (selectedColor != null && player.hasPermission(MessageColors.COLOR_PERMISSION_NODE + selectedColor)) {
                CustomColor.get(selectedColor)
            } else {
                default ?: CustomColor(CustomColor.ColorType.NORMAL, "§r")
            }
        }
    }

    fun getChannel(): Channel? {
        channel ?: return null
        return Channel.channels[channel]
    }

    fun setChannel(channel: Channel?) {
        this.channel = channel?.id
        if (channel != null && channel !is PrivateChannel) {
            player.data.setChannel(channel)
        }
    }

    fun addMessage(packet: Packet) {
        try {
            receivedMessages += ChatMessage(
                packet.source,
                packet.getComponent()?.toPlainText()?.replace("\\s".toRegex(), "")?.takeLast(48)
            )
            if (receivedMessages.size > 100) {
                receivedMessages.removeFirstOrNull()
            }
        } catch (t: Throwable) {
            t.reportOnce("Error occurred while caching chat packet!Maybe your server doesn't support it")
        }
    }

    fun removeMessage(message: String) {
        receivedMessages.removeIf { it.message == message }
    }

    fun releaseMessage() {
        val messages = ArrayList(receivedMessages)
        receivedMessages.clear()
        repeat(100) { player.sendMessage("") }
        messages.forEach { player.sendPacket(it.packet) }
    }

    companion object {

        @JvmField
        val sessions = ConcurrentHashMap<UUID, ChatSession>()

        fun getSession(player: Player): ChatSession {
            return sessions.computeIfAbsent(player.uniqueId) { ChatSession(player) }
        }

        fun removeSession(player: Player) {
            sessions -= player.uniqueId
        }

        data class ChatMessage(val packet: Any, val message: String?)
    }
}