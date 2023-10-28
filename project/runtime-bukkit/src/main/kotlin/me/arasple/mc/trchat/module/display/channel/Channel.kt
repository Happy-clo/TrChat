package me.arasple.mc.trchat.module.display.channel

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.event.TrChatEvent
import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.display.channel.obj.ChannelBindings
import me.arasple.mc.trchat.module.display.channel.obj.ChannelEvents
import me.arasple.mc.trchat.module.display.channel.obj.ChannelSettings
import me.arasple.mc.trchat.module.display.channel.obj.Range
import me.arasple.mc.trchat.module.display.format.Format
import me.arasple.mc.trchat.module.internal.data.ChatLogs
import me.arasple.mc.trchat.module.internal.service.Metrics
import me.arasple.mc.trchat.util.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.unregisterCommand
import taboolib.common.util.subList
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.lang.sendLang
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.sendLang

/**
 * @author ItsFlicker
 * @since 2021/12/11 22:27
 */
open class Channel(
    val id: String,
    val settings: ChannelSettings,
    val bindings: ChannelBindings,
    val events: ChannelEvents,
    val formats: List<Format>,
    val console: Format? = null,
    val listeners: MutableSet<String> = mutableSetOf()
) {

    init {
        if (settings.autoJoin) {
            onlinePlayers.forEach {
                if (it.passPermission(settings.joinPermission)) {
                    listeners.add(it.name)
                }
            }
        } else {
            onlinePlayers.filter { it.session.channel == id }.forEach {
                join(it, id, hint = false)
            }
        }
        if (!bindings.command.isNullOrEmpty()) {
            command(
                name = bindings.command[0],
                aliases = subList(bindings.command, 1),
                description = "TrChat channel $id",
                permission = settings.joinPermission,
                permissionDefault = if (settings.joinPermission.isEmpty()) PermissionDefault.TRUE else PermissionDefault.OP
            ) {
                execute<Player> { sender, _, _ ->
                    if (sender.session.channel == this@Channel.id) {
                        quit(sender)
                    } else {
                        join(sender, this@Channel)
                    }
                }
                dynamic("message", optional = true) {
                    execute<CommandSender> { sender, _, argument ->
                        if (sender is Player) {
                            execute(sender, argument)
                        } else {
                            execute(sender, argument)
                        }
                    }
                }
                incorrectSender { sender, _ ->
                    sender.sendLang("Command-Not-Player")
                }
            }
        }
    }

    open fun unregister() {
        bindings.command?.forEach { unregisterCommand(it) }
        listeners.clear()
    }

    open fun execute(sender: CommandSender, message: String) {
        if (sender is Player) {
            execute(sender, message)
            return
        }
        val component = Components.empty()
        console?.let { format ->
            format.prefix.forEach { prefix ->
                component.append(prefix.value[0].content.toTextComponent(sender)) }
            component.append(format.msg.createComponent(sender, message, settings.disabledFunctions))
            format.suffix.forEach { suffix ->
                component.append(suffix.value[0].content.toTextComponent(sender)) }
        } ?: return

        if (settings.proxy && BukkitProxyManager.processor != null) {
            BukkitProxyManager.sendBroadcastRaw(
                onlinePlayers.firstOrNull(),
                nilUUID,
                component,
                settings.joinPermission,
                settings.doubleTransfer,
                settings.ports
            )
        } else if (!settings.forceProxy) {
            listeners.forEach { getProxyPlayer(it)?.sendComponent(null, component) }
            sender.sendComponent(null, component)
        }
    }

    open fun execute(player: Player, message: String, toConsole: Boolean = true): Pair<ComponentText, ComponentText?>? {
        if (!player.checkMute()) {
            return null
        }
        if (!settings.speakCondition.pass(player)) {
            player.sendLang("Channel-No-Speak-Permission")
            return null
        }
        if (settings.filterBeforeSending && TrChat.api().getFilterManager().filter(message, adaptPlayer(player)).sensitiveWords > 0) {
            player.sendLang("Channel-Bad-Language")
            return null
        }
        val session = player.session
        session.lastChannel = this
        session.lastPublicMessage = message
        val event = TrChatEvent(this, session, message)
        if (!event.call()) {
            return null
        }
        val msg = events.process(player, event.message)?.replace("{{", "\\{{") ?: return null
        ChatLogs.logNormal(player.name, msg)
        Metrics.increase(0)

        val component = Components.empty()
        formats.firstOrNull { it.condition.pass(player) }?.let { format ->
            format.prefix
                .mapNotNull { prefix -> prefix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { prefix -> component.append(prefix) }
            component.append(format.msg.createComponent(player, msg, settings.disabledFunctions))
            format.suffix
                .mapNotNull { suffix -> suffix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { suffix -> component.append(suffix) }
        } ?: return null
        if (session.cancelChat) {
            session.cancelChat = false
            return null
        }
        // TODO: 跨服事件传递
        // Proxy
        if (settings.proxy) {
            if (BukkitProxyManager.processor != null || settings.forceProxy) {
                BukkitProxyManager.sendBroadcastRaw(
                    player,
                    player.uniqueId,
                    component,
                    settings.joinPermission,
                    settings.doubleTransfer,
                    settings.ports
                )
                return component to null
            }
        }
        // Local
        when (settings.range.type) {
            Range.Type.ALL -> {
                listeners.filter { events.send(player, it, msg) }.forEach {
                    getProxyPlayer(it)?.sendComponent(player, component)
                }
            }
            Range.Type.SINGLE_WORLD -> {
                onlinePlayers.filter { it.name in listeners
                        && it.world == player.world
                        && events.send(player, it.name, msg) }.forEach {
                    it.sendComponent(player, component)
                }
            }
            Range.Type.DISTANCE -> {
                onlinePlayers.filter { it.name in listeners
                        && it.world == player.world
                        && it.location.distance(player.location) <= settings.range.distance
                        && events.send(player, it.name, msg) }.forEach {
                    it.sendComponent(player, component)
                }
            }
            Range.Type.SELF -> {
                if (events.send(player, player.name, msg)) {
                    player.sendComponent(player, component)
                }
            }
        }
        if (toConsole || settings.isPrivate) {
            console().sendComponent(player, component)
        }
        return component to null
    }

    companion object {

        val channels = mutableMapOf<String, Channel>()

        fun join(player: Player, channel: String, hint: Boolean = true): Boolean {
            val id = channels.keys.firstOrNull { channel.equals(it, ignoreCase = true) }
            channels[id]?.let {
                return join(player, it, hint)
            }
            quit(player)
            return false
        }

        fun join(player: Player, channel: Channel, hint: Boolean = true): Boolean {
            if (!player.passPermission(channel.settings.joinPermission)) {
                player.sendLang("General-No-Permission")
                return false
            }
            player.session.setChannel(channel)
            channel.listeners.add(player.name)
            channel.events.join(player)

            if (hint) {
                player.sendLang("Channel-Join", channel.id)
            }
            return true
        }

        fun quit(player: Player) {
            player.session.getChannel()?.let {
                if (!it.settings.autoJoin) {
                    it.listeners -= player.name
                }
                it.events.quit(player)
                player.sendLang("Channel-Quit", it.id)
            }
            if (!join(player, Settings.defaultChannel)) {
                player.session.setChannel(null)
            }
        }
    }
}