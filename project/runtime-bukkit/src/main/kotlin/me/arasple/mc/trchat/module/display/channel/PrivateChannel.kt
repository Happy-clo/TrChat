package me.arasple.mc.trchat.module.display.channel

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.event.TrChatEvent
import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.module.display.channel.obj.ChannelBindings
import me.arasple.mc.trchat.module.display.channel.obj.ChannelEvents
import me.arasple.mc.trchat.module.display.channel.obj.ChannelSettings
import me.arasple.mc.trchat.module.display.format.Format
import me.arasple.mc.trchat.module.internal.command.main.CommandReply
import me.arasple.mc.trchat.module.internal.data.ChatLogs
import me.arasple.mc.trchat.module.internal.data.PlayerData
import me.arasple.mc.trchat.module.internal.service.Metrics
import me.arasple.mc.trchat.util.checkMute
import me.arasple.mc.trchat.util.pass
import me.arasple.mc.trchat.util.sendComponent
import me.arasple.mc.trchat.util.session
import org.bukkit.entity.Player
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.command.suggest
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.util.subList
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.lang.sendLang
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.sendLang

/**
 * @author ItsFlicker
 * @since 2022/2/8 11:03
 */
class PrivateChannel(
    id: String,
    settings: ChannelSettings,
    bindings: ChannelBindings,
    events: ChannelEvents,
    val sender: List<Format>,
    val receiver: List<Format>
) : Channel(id, settings, bindings, events, emptyList()) {

    init {
        onlinePlayers.filter { it.session.channel == id }.forEach {
            join(it, id, hint = false)
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
                    if (sender.session.channel == this@PrivateChannel.id) {
                        quit(sender)
                    } else {
                        sender.sendLang("Private-Message-No-Player")
                    }
                }
                dynamic("player", optional = true) {
                    suggest {
                        BukkitProxyManager.getPlayerNames().keys.filter { it !in PlayerData.vanishing }
                    }
                    execute<Player> { sender, _, argument ->
                        sender.session.lastPrivateTo = BukkitProxyManager.getExactName(argument)
                            ?: return@execute sender.sendLang("Command-Player-Not-Exist")
                        join(sender, this@PrivateChannel)
                    }
                    dynamic("message", optional = true) {
                        execute<Player> { sender, ctx, argument ->
                            BukkitProxyManager.getExactName(ctx["player"])?.let {
                                sender.session.lastPrivateTo = it
                                execute(sender, argument)
                            } ?: sender.sendLang("Command-Player-Not-Exist")
                        }
                    }
                }
                incorrectSender { sender, _ ->
                    sender.sendLang("Command-Not-Player")
                }
            }
        }
    }

    override fun execute(player: Player, message: String, toConsole: Boolean): Pair<ComponentText, ComponentText?>? {
        if (!player.checkMute()) {
            return null
        }
        if (!settings.speakCondition.pass(player)) {
            player.sendLang("Channel-No-Speak-Permission")
            return null
        }
        if (settings.filterBeforeSending && TrChat.api().getFilterManager().filter(message).sensitiveWords > 0) {
            player.sendLang("Channel-Bad-Language")
            return null
        }
        val session = player.session
        val to = session.lastPrivateTo
        session.lastChannel = this
        session.lastPrivateMessage = message
        val event = TrChatEvent(this, session, message)
        if (!event.call()) {
            return null
        }
        val msg = events.process(player, event.message)?.replace("{{", "\\{{") ?: return null

        val send = Components.empty()
        var msgComponent: ComponentText? = null
        sender.firstOrNull { it.condition.pass(player) }?.let { format ->
            format.prefix
                .mapNotNull { prefix -> prefix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { prefix -> send.append(prefix) }
            msgComponent = format.msg.createComponent(player, msg, settings.disabledFunctions)
            send.append(msgComponent!!)
            format.suffix
                .mapNotNull { suffix -> suffix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { suffix -> send.append(suffix) }
        } ?: return null

        val receive = Components.empty()
        receiver.firstOrNull { it.condition.pass(player) }?.let { format ->
            format.prefix
                .mapNotNull { prefix -> prefix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { prefix -> receive.append(prefix) }
            receive.append(format.msg.createComponent(player, msg, settings.disabledFunctions))
            format.suffix
                .mapNotNull { suffix -> suffix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { suffix -> receive.append(suffix) }
        } ?: return null

        if (session.cancelChat) {
            session.cancelChat = false
            return null
        }
        // Channel event
        if (!events.send(player, to, msg)) {
            return null
        }
        player.sendComponent(player, send)

        PlayerData.data.filterValues { it.isSpying }.entries.forEach { (_, v) ->
            v.player.player
                ?.sendLang("Private-Message-Spy-Format", player.name, to, msgComponent!!.toLegacyText())
        }
        console().sendLang("Private-Message-Spy-Format", player.name, to, msgComponent!!.toLegacyText())

        CommandReply.lastMessageFrom[to] = player.name
        ChatLogs.logPrivate(player.name, to, message)
        Metrics.increase(0)

        if (settings.proxy && BukkitProxyManager.processor != null) {
            BukkitProxyManager.sendRaw(
                player,
                to,
                receive,
                settings.doubleTransfer
            )
            BukkitProxyManager.sendProxyLang(player, to, "Private-Message-Receive", player.name)
        } else {
            getProxyPlayer(to)?.let {
                it.sendComponent(player, receive)
                it.sendLang("Private-Message-Receive", player.name)
            }
        }
        return send to receive
    }
}