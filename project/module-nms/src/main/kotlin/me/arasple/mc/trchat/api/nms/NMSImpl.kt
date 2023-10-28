package me.arasple.mc.trchat.api.nms

import me.arasple.mc.trchat.util.reportOnce
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket
import net.minecraft.server.v1_12_R1.ChatMessageType
import net.minecraft.server.v1_12_R1.PacketPlayOutChat
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.module.chat.ComponentText
import taboolib.module.nms.MinecraftVersion.isUniversal
import taboolib.module.nms.MinecraftVersion.majorLegacy
import taboolib.module.nms.sendPacket
import taboolib.platform.util.isAir
import java.util.*

private typealias IChatBaseComponent12 = net.minecraft.server.v1_12_R1.IChatBaseComponent
private typealias ChatSerializer12 = net.minecraft.server.v1_12_R1.IChatBaseComponent.ChatSerializer
private typealias ChatSerializer16 = net.minecraft.server.v1_16_R1.IChatBaseComponent.ChatSerializer
private typealias CraftPlayer19 = org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer
private typealias CraftItemStack12 = org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
private typealias CraftItemStack19 = org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack
private typealias CraftChatMessage19 = org.bukkit.craftbukkit.v1_19_R2.util.CraftChatMessage
private typealias NBTTagCompound12 = net.minecraft.server.v1_12_R1.NBTTagCompound
private typealias NBTTagCompound19 = net.minecraft.nbt.NBTTagCompound
private typealias NMSIChatBaseComponent = net.minecraft.network.chat.IChatBaseComponent

@Suppress("unused")
class NMSImpl : NMS() {

    override fun craftChatMessageFromComponent(component: ComponentText): Any {
        return try {
            if (majorLegacy >= 11604) {
                CraftChatMessage19.fromJSON(component.toRawMessage())
            } else if (majorLegacy >= 11600) {
                ChatSerializer16.a(component.toRawMessage())!!
            } else {
                ChatSerializer12.a(component.toRawMessage())!!
            }
        } catch (t: Throwable) {
            throw IllegalStateException("Got an error translating component!Please report!", t)
        }
    }

    override fun rawMessageFromCraftChatMessage(component: Any): String {
        return try {
            if (majorLegacy >= 11604) {
                CraftChatMessage19.toJSON(component as NMSIChatBaseComponent)
            } else {
                ChatSerializer12.a(component as IChatBaseComponent12)!!
            }
        } catch (t: Throwable) {
            throw IllegalStateException("Got an error translating component!Please report!", t)
        }
    }

    override fun sendMessage(receiver: Player, component: ComponentText, sender: UUID?) {
        if (majorLegacy >= 11900) {
            val player = (receiver as CraftPlayer19).handle
            player.sendSystemMessage(craftChatMessageFromComponent(component) as NMSIChatBaseComponent)
        } else if (majorLegacy >= 11600) {
            receiver.sendPacket(PacketPlayOutChat::class.java.invokeConstructor(
                craftChatMessageFromComponent(component),
                ChatMessageType.CHAT,
                sender
            ))
        } else if (majorLegacy >= 11200) {
            receiver.sendPacket(PacketPlayOutChat::class.java.invokeConstructor(
                craftChatMessageFromComponent(component),
                ChatMessageType.CHAT
            ))
        } else {
            receiver.sendPacket(PacketPlayOutChat::class.java.invokeConstructor(
                craftChatMessageFromComponent(component),
                0.toByte()
            ))
        }
    }

    override fun hoverItem(component: ComponentText, itemStack: ItemStack): ComponentText {
        val nmsItem = CraftItemStack19.asNMSCopy(itemStack)
        val nbtTag = NBTTagCompound19()
        nmsItem.save(nbtTag)
        val id = nbtTag.getString("id") ?: "minecraft:air"
        val nbt = nbtTag.get("tag")?.toString() ?: "{}"
        return component.hoverItem(id, nbt)
    }

    override fun optimizeNBT(itemStack: ItemStack, nbtWhitelist: Array<String>): ItemStack {
        if (itemStack.isAir()) return itemStack
        try {
            if (isUniversal) {
                val nmsItem = CraftItemStack19.asNMSCopy(itemStack)
                if (nmsItem.hasTag()) {
                    val nbtTag = NBTTagCompound19()
                    nmsItem.tag!!.allKeys.forEach { key ->
                        if (key in nbtWhitelist) {
                            nbtTag.put(key, nmsItem.tag!!.get(key))
                        }
                    }
                    nmsItem.tag = nbtTag
                    return CraftItemStack19.asBukkitCopy(nmsItem)
                }
            } else {
                val nmsItem = CraftItemStack12.asNMSCopy(itemStack)
                if (nmsItem.hasTag()) {
                    val nbtTag = NBTTagCompound12()
                    nmsItem.tag!!.c().forEach { key ->
                        if (key in nbtWhitelist) {
                            nbtTag.set(key, nmsItem.tag!!.get(key))
                        }
                    }
                    nmsItem.tag = nbtTag
                    return CraftItemStack12.asBukkitCopy(nmsItem)
                }
            }

        } catch (t: Throwable) {
            t.reportOnce("Got an error optimizing item nbt")
        }
        return itemStack
    }

    override fun addCustomChatCompletions(player: Player, entries: List<String>) {
        if (majorLegacy < 11901) return
        try {
            player.sendPacket(ClientboundCustomChatCompletionsPacket::class.java.invokeConstructor(
                ClientboundCustomChatCompletionsPacket.Action.ADD,
                entries
            ))
        } catch (_: NoClassDefFoundError) {
        }
    }

    override fun removeCustomChatCompletions(player: Player, entries: List<String>) {
        if (majorLegacy < 11901) return
        try {
            player.sendPacket(ClientboundCustomChatCompletionsPacket::class.java.invokeConstructor(
                ClientboundCustomChatCompletionsPacket.Action.REMOVE,
                entries
            ))
        } catch (_: NoClassDefFoundError) {
        }
    }

    override fun setCustomChatCompletions(player: Player, entries: List<String>) {
        if (majorLegacy < 11901) return
        try {
            player.sendPacket(ClientboundCustomChatCompletionsPacket::class.java.invokeConstructor(
                ClientboundCustomChatCompletionsPacket.Action.SET,
                entries
            ))
        } catch (_: NoClassDefFoundError) {
        }
    }
}