package me.arasple.mc.trchat.util

import me.arasple.mc.trchat.api.impl.BukkitComponentManager
import me.arasple.mc.trchat.module.display.ChatSession
import me.arasple.mc.trchat.module.internal.TrChatBukkit
import me.arasple.mc.trchat.module.internal.command.main.CommandMute
import me.arasple.mc.trchat.module.internal.data.PlayerData
import me.arasple.mc.trchat.module.internal.script.Condition
import me.arasple.mc.trchat.module.internal.script.kether.KetherHandler
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.ComponentText
import taboolib.module.nms.MinecraftVersion
import taboolib.module.ui.MenuHolder
import taboolib.module.ui.type.Basic
import taboolib.module.ui.type.Hopper
import taboolib.platform.util.sendLang

val isDragonCoreHooked by unsafeLazy { Bukkit.getPluginManager().isPluginEnabled("DragonCore") && MinecraftVersion.major < 8  }

fun createNoClickChest(rows: Int, title: String) =
    MenuHolder(object : Basic(title) {
        init {
            rows(rows)
            onClick(lock = true)
        }
    }).inventory

@Suppress("Deprecation")
fun createNoClickHopper(title: String) =
    Bukkit.createInventory(MenuHolder(object : Hopper(title) {
        init {
            rows(1)
            onClick(lock = true)
        }
    }), InventoryType.HOPPER, title)

fun String?.toCondition() = if (this == null) Condition.EMPTY else Condition(this)

fun Condition?.pass(commandSender: CommandSender): Boolean {
    return if (commandSender is Player) {
        this?.eval(commandSender) != false
    } else {
        true
    }
}

fun Player.passPermission(permission: String?): Boolean {
    return permission.isNullOrEmpty()
            || permission.equals("null", ignoreCase = true)
            || permission.equals("none", ignoreCase = true)
            || hasPermission(permission)
}

fun String.setPlaceholders(sender: CommandSender): String {
    return try {
        if (sender is OfflinePlayer) {
            PlaceholderAPI.setPlaceholders(sender, this)
        } else {
            this
        }
    } catch (t: Throwable) {
        t.print("Error occurred when parsing placeholder!This is not a bug of TrChat")
        this
    }
}

fun String.parseInline(sender: CommandSender, vars: Map<String, Any> = emptyMap()): String {
    return KetherHandler.parseInline(this, sender, vars)
}

inline val Player.session get() = ChatSession.getSession(this)

inline val Player.data get() = PlayerData.getData(this)

fun Player.checkMute(): Boolean {
    if (TrChatBukkit.isGlobalMuting && !hasPermission("trchat.bypass.globalmute")) {
        sendLang("General-Global-Muting")
        return false
    }
    val data = data
    if (data.isMuted) {
        sendLang("General-Muted", CommandMute.muteDateFormat.format(data.muteTime), data.muteReason)
        return false
    }
    return true
}

fun Any.sendComponent(sender: Any?, component: ComponentText) {
    BukkitComponentManager.sendComponent(this, component, sender)
}

fun Player.getCooldownLeft(type: CooldownType) = Cooldowns.getCooldownLeft(uniqueId, type.alias)

fun Player.isInCooldown(type: CooldownType) = Cooldowns.isInCooldown(uniqueId, type.alias)

fun Player.updateCooldown(type: CooldownType, lasts: Long) = Cooldowns.updateCooldown(uniqueId, type.alias, lasts)