package me.arasple.mc.trchat.module.internal.hook.impl

import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.Subscribe
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent
import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.module.internal.hook.HookAbstract
import me.arasple.mc.trchat.util.session
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptPlayer

class HookDiscordSRV : HookAbstract() {

    override fun init() {
        if (!isHooked) return
        DiscordSRV.api.subscribe(object {
            @Subscribe
            fun onChatPreProcess(e: GameChatMessagePreProcessEvent) {
                val channel = e.player.session.lastChannel ?: return
                if (channel.settings.sendToDiscord) {
                    e.message = TrChat.api().getFilterManager().filter(e.message, adaptPlayer(e.player)).filtered
                } else {
                    e.isCancelled = true
                }
            }
            @Subscribe
            fun onMessagePreBroadcast(e: DiscordGuildMessagePreBroadcastEvent) {
                e.recipients.removeIf {
                    (it as? Player)?.session?.getChannel()?.settings?.receiveFromDiscord == false
                }
            }
        })
    }

}