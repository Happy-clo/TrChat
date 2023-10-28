package me.arasple.mc.trchat.util

import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.PlaceholderAPIPlugin
import org.bukkit.command.CommandSender
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.function.submit
import taboolib.module.configuration.ConfigNode
import taboolib.platform.util.sendLang

/**
 * @author Arasple
 * @date 2019/11/29 21:29
 */
@PlatformSide([Platform.BUKKIT])
object PAPIUtil {

    @ConfigNode("Options.Depend-Expansions", "settings.yml")
    var depends = emptyList<String>()
        private set

    fun checkExpansions(sender: CommandSender): Boolean {
        val registered = PlaceholderAPI.getRegisteredIdentifiers()
        val uninstalled = depends.filter { ex -> registered.none { it.equals(ex, true) } }.toTypedArray()
        return if (uninstalled.isEmpty()) {
            true
        } else {
            sender.sendLang("General-Expansions-Header", uninstalled.size)
            uninstalled.forEach { sender.sendLang("General-Expansions-Format", it) }
            false
        }
    }

    /**
     * 下载 PlaceholderAPI 拓展变量并注册
     *
     * @param expansions 拓展
     */
    @Deprecated("works badly")
    fun downloadExpansions(expansions: List<String>) {
        kotlin.runCatching {
            if (expansions.isNotEmpty()) {
                if (PlaceholderAPIPlugin.getInstance().cloudExpansionManager.cloudExpansions.isEmpty()) {
                    PlaceholderAPIPlugin.getInstance().cloudExpansionManager.fetch(false)
                }
                val unInstalled = expansions.filter { d ->
                    PlaceholderAPIPlugin.getInstance().localExpansionManager.expansions.none { e -> e.name.equals(d, ignoreCase = true) }
                            && PlaceholderAPIPlugin.getInstance().cloudExpansionManager.findCloudExpansionByName(d).isPresent
                            && !PlaceholderAPIPlugin.getInstance().cloudExpansionManager.isDownloading(
                        PlaceholderAPIPlugin.getInstance().cloudExpansionManager.findCloudExpansionByName(d).get()
                    )
                }
                if (unInstalled.isNotEmpty()) {
                    unInstalled.forEach { ex ->
                        val cloudExpansion = PlaceholderAPIPlugin.getInstance().cloudExpansionManager.cloudExpansions[ex]!!
                        PlaceholderAPIPlugin.getInstance().cloudExpansionManager.downloadExpansion(cloudExpansion, cloudExpansion.version)
                    }
                    submit(delay = 20) {
                        PlaceholderAPIPlugin.getInstance().localExpansionManager.expansions.forEach {
                            PlaceholderAPIPlugin.getInstance().localExpansionManager.register(it)
                        }
                    }
                }
            }
        }.onFailure {
            it.print("PlaceholderAPI expansion(s) $expansions installed failed!Please install manually.")
        }
    }

}