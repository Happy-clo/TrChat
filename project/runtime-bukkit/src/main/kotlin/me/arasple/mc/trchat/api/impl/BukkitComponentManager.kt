package me.arasple.mc.trchat.api.impl

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.ComponentManager
import me.arasple.mc.trchat.api.event.TrChatReceiveEvent
import me.arasple.mc.trchat.api.nms.NMS
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.util.data
import me.arasple.mc.trchat.util.toUUID
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.CommandSender
import org.bukkit.command.ProxiedCommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.platform.*
import taboolib.common.platform.function.adaptCommandSender
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.chat.impl.DefaultComponent
import taboolib.module.configuration.ConfigNode
import java.util.*

/**
 * @author ItsFlicker
 * @since 2022/6/8 12:56
 */
@PlatformSide([Platform.BUKKIT])
object BukkitComponentManager : ComponentManager {

    @ConfigNode("Enable.Chat", "filter.yml")
    var isFilterEnabled = true

    init {
        PlatformFactory.registerAPI<ComponentManager>(this)
    }

    override fun sendComponent(receiver: Any, component: ComponentText, sender: Any?) {
        val commandSender = when (receiver) {
            is ProxyCommandSender -> receiver.cast()
            is ProxiedCommandSender -> receiver.callee
            is CommandSender -> receiver
            else -> error("Unknown receiver type $receiver.")
        }
        val uuid = when (sender) {
            is ProxyPlayer -> sender.uniqueId
            is Entity -> sender.uniqueId
            is String -> sender.toUUID()
            is UUID -> sender
            else -> null
        }
        if (commandSender is Player && uuid in commandSender.data.ignored) {
            return
        }
        val event = TrChatReceiveEvent(commandSender, uuid, component)
        if (!event.call()) {
            return
        }
        val newComponent = if (isFilterEnabled && commandSender is Player && commandSender.data.isFilterEnabled) {
            filterComponent(event.message, Settings.componentMaxLength)
        } else if (commandSender is Player) {
            validateComponent(event.message, Settings.componentMaxLength)
        } else {
            event.message
        }
        if (commandSender is Player) {
            NMS.instance.sendMessage(commandSender, newComponent, event.sender)
        } else {
            newComponent.sendTo(adaptCommandSender(commandSender))
        }
    }

    override fun filterComponent(component: ComponentText, maxLength: Int): ComponentText {
        return validateComponent(DefaultComponent(listOf(filterComponent(component.toSpigotObject()))), maxLength)
    }

    override fun validateComponent(component: ComponentText, maxLength: Int): ComponentText {
        if (maxLength <= 0) return component
        val length = component.toRawMessage().length
        return if (length > maxLength) {
            Components.text("This chat component is too big to show ($length > $maxLength).")
        } else {
            component
        }
    }

    private fun filterComponent(component: BaseComponent): BaseComponent {
        if (component is TextComponent && component.text.isNotEmpty()) {
            component.text = TrChat.api().getFilterManager().filter(component.text).filtered
        }
        if (!component.extra.isNullOrEmpty()) {
            component.extra = component.extra.map { filterComponent(it) }
        }
        return component
    }
}