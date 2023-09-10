@file:Suppress("Deprecation")

package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.function.Function
import me.arasple.mc.trchat.module.internal.TrChatBukkit
import me.arasple.mc.trchat.util.*
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.Strings
import taboolib.module.configuration.ConfigNode
import taboolib.platform.util.sendLang

/**
 * @author ItsFlicker
 * @date 2019/11/30 12:10
 */
@PlatformSide([Platform.BUKKIT])
object ListenerBukkitChat {

    @ConfigNode("Options.Always-Cancel-Chat-Event", "settings.yml")
    var cancel = false
        private set

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBukkitChat(e: AsyncPlayerChatEvent) {
        if (e.isCancelled) return
        e.recipients.clear()
        if (cancel) {
            e.isCancelled = true
        }
        val player = e.player
        val session = player.session

        if (!player.checkMute()) {
            return
        }
        if (!checkLimits(player, e.message)) {
            return
        }
        Channel.channels.values.forEach { channel ->
            channel.bindings.prefix?.forEach {
                if (e.message.startsWith(it, ignoreCase = true)) {
                    if (channel.settings.isPrivate) e.isCancelled = true
                    channel.execute(player, e.message.substring(it.length), TrChatBukkit.isPaperEnv || cancel)
                    return
                }
            }
        }
        session.getChannel()?.let {
            if (it.settings.isPrivate) e.isCancelled = true
            it.execute(player, e.message, TrChatBukkit.isPaperEnv || cancel)
        }
    }

    private fun checkLimits(player: Player, message: String): Boolean {
        if (player.hasPermission("trchat.bypass.*")) {
            return true
        }
        if (!player.hasPermission("trchat.bypass.chatlength")) {
            if (message.length > Settings.chatLengthLimit) {
                player.sendLang("General-Too-Long", message.length, Settings.chatLengthLimit)
                return false
            }
        }
        if (!player.hasPermission("trchat.bypass.repeat")) {
            val lastMessage = player.session.lastPublicMessage
            if (Settings.chatSimilarity > 0 && Strings.similarDegree(lastMessage, message) > Settings.chatSimilarity) {
                player.sendLang("General-Too-Similar")
                return false
            }
        }
        if (!player.hasPermission("trchat.bypass.chatcd")) {
            val chatCooldown = player.getCooldownLeft(CooldownType.CHAT)
            if (chatCooldown > 0) {
                player.sendLang("Cooldowns-Chat", chatCooldown / 1000)
                return false
            }
        }
        if (Function.functions.any { !it.checkCooldown(player, message) }) {
            return false
        }
        player.updateCooldown(CooldownType.CHAT, Settings.chatCooldown.get())
        return true
    }

}