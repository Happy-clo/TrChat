package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.internal.service.Updater
import me.arasple.mc.trchat.util.data
import me.arasple.mc.trchat.util.passPermission
import me.arasple.mc.trchat.util.session
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submit
import taboolib.expansion.setupDataContainer

/**
 * @author ItsFlicker
 * @since 2021/12/11 23:19
 */
@PlatformSide([Platform.BUKKIT])
object ListenerJoin {

    private var hasFetched = false

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player

        player.setupDataContainer()
        player.data
        player.session

        submit(delay = 20) {
            if (!player.isOnline) return@submit
            if (!hasFetched) {
                BukkitProxyManager.sendMessage(player, arrayOf("FetchProxyChannels"))
                hasFetched = true
            }
            Channel.channels.values.filter { it.settings.autoJoin }.forEach {
                if (player.passPermission(it.settings.joinPermission)) {
                    it.listeners.add(player.name)
                }
            }
            Updater.notifyPlayer(adaptPlayer(player))
        }
    }
}