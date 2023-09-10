package me.arasple.mc.trchat.api.nms

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.ComponentText
import taboolib.module.nms.nmsProxy
import java.util.*

abstract class NMS {

    /**
     * ComponentText -> IChatBaseComponent
     */
    abstract fun craftChatMessageFromComponent(component: ComponentText): Any

    /**
     * IChatBaseComponent -> raw string
     */
    abstract fun rawMessageFromCraftChatMessage(component: Any): String

    abstract fun sendMessage(receiver: Player, component: ComponentText, sender: UUID?)

    abstract fun hoverItem(component: ComponentText, itemStack: ItemStack): ComponentText

    abstract fun optimizeNBT(itemStack: ItemStack, nbtWhitelist: Array<String> = whitelistTags): ItemStack

    abstract fun addCustomChatCompletions(player: Player, entries: List<String>)

    abstract fun removeCustomChatCompletions(player: Player, entries: List<String>)

    abstract fun setCustomChatCompletions(player: Player, entries: List<String>)

    companion object {

        @JvmStatic
        val instance = nmsProxy<NMS>()

        val whitelistTags = arrayOf(
            // 附魔
            "ench",
            // 附魔 1.14
            "Enchantments",
            // 附魔书
            "StoredEnchantments",
            // 展示
            "display",
            // 属性
            "AttributeModifiers",
            // 药水
            "Potion",
            // 特殊药水
            "CustomPotionEffects",
            // 隐藏标签
            "HideFlags"
        )
    }
}
