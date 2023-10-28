package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.util.color.MessageColors
import me.arasple.mc.trchat.util.parseSimple
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.configuration.ConfigNode
import taboolib.platform.util.isAir
import taboolib.platform.util.modifyMeta

/**
 * @author ItsFlicker
 * @date 2019/8/15 21:18
 */
@PlatformSide([Platform.BUKKIT])
object ListenerAnvilChange {

    @ConfigNode("Enable.Anvil", "filter.yml")
    var filter = true
        private set

    @ConfigNode("Color.Anvil", "settings.yml")
    var color = true
        private set

    @Suppress("Deprecation")
    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAnvilCraft(e: PrepareAnvilEvent) {
        val p = e.view.player
        val result = e.result

        if (e.inventory.type != InventoryType.ANVIL || result.isAir()) {
            return
        }
        result.modifyMeta<ItemMeta> {
            if (!hasDisplayName()) {
                return@modifyMeta
            }
            if (filter) {
                setDisplayName(TrChat.api().getFilterManager().filter(displayName, adaptPlayer(p)).filtered)
            }
            if (color) {
                if (p.hasPermission("trchat.color.simple")) {
                    setDisplayNameComponent(arrayOf(displayName.parseSimple().toSpigotObject()))
                } else {
                    setDisplayName(MessageColors.replaceWithPermission(p, displayName, MessageColors.Type.ANVIL))
                }
            }
        }
        e.result = result
    }
}