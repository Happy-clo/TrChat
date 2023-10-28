package me.arasple.mc.trchat.module.internal.command.main

import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.module.internal.TrChatBukkit
import me.arasple.mc.trchat.util.data
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.command.command
import taboolib.common.platform.command.suggest
import taboolib.common.platform.command.suggestUncheck
import taboolib.common5.Demand
import taboolib.common5.util.parseMillis
import taboolib.expansion.createHelper
import taboolib.platform.util.sendLang
import java.text.SimpleDateFormat

/**
 * CommandPrivateMessage
 * me.arasple.mc.trchat.module.internal.command
 *
 * @author ItsFlicker
 * @since 2021/7/21 10:40
 */
@PlatformSide([Platform.BUKKIT])
object CommandMute {

    val muteDateFormat = SimpleDateFormat()

    @Awake(LifeCycle.ENABLE)
    fun c() {
        command("mute", listOf("trmute"), description = "Mute a player", permission = "trchat.command.mute") {
            dynamic("player") {
                suggest {
                    BukkitProxyManager.getPlayerNames().keys.toList()
                }
                execute<CommandSender> { sender, ctx, _ ->
                    val player = Bukkit.getPlayer(ctx["player"])
                        ?: return@execute sender.sendLang("Command-Player-Not-Exist")
                    val data = player.data
                    data.updateMuteTime("999d".parseMillis())
                    sender.sendLang("Mute-Muted-Player", player.name, "999d", "null")
                    (player as? Player)
                        ?.sendLang("General-Muted", muteDateFormat.format(data.muteTime), data.muteReason)
                }
                dynamic("options", optional = true) {
                    suggestUncheck {
                        listOf("-t 1h", "-t 2d", "-t 15m", "-r Reason", "--cancel")
                    }
                    execute<CommandSender> { sender, ctx, argument ->
                        val player = Bukkit.getPlayer(ctx["player"])
                            ?: return@execute sender.sendLang("Command-Player-Not-Exist")
                        val data = player.data
                        val de = Demand("mute $argument")
                        if (de.tags.contains("cancel")) {
                            data.updateMuteTime(0)
                            sender.sendLang("Mute-Cancel-Muted-Player", player.name)
                            (player as? Player)?.sendLang("General-Cancel-Muted")
                        } else {
                            val time = de.get("t") ?: "999d"
                            val reason = de.get("r") ?: "null"
                            data.updateMuteTime(time.parseMillis())
                            data.setMuteReason(reason)
                            sender.sendLang("Mute-Muted-Player", player.name, time, reason)
                            (player as? Player)
                                ?.sendLang("General-Muted", muteDateFormat.format(data.muteTime), data.muteReason)
                        }
                    }
                }
            }
            incorrectCommand { _, _, _, _ ->
                createHelper()
            }
        }
        command("unmute", listOf("trunmute"), description = "Unmute a player", permission = "trchat.command.unmute") {
            dynamic("player") {
                suggest {
                    BukkitProxyManager.getPlayerNames().keys.toList()
                }
                execute<CommandSender> { sender, ctx, _ ->
                    val player = Bukkit.getPlayer(ctx["player"])
                        ?: return@execute sender.sendLang("Command-Player-Not-Exist")
                    player.data.updateMuteTime(0)
                    sender.sendLang("Mute-Cancel-Muted-Player", player.name)
                    (player as? Player)?.sendLang("General-Cancel-Muted")
                }
            }
        }
        command("muteall", listOf("globalmute"), "Mute all players", permission = "trchat.command.muteall") {
            execute<CommandSender> { sender, _, _ ->
                TrChatBukkit.isGlobalMuting = !TrChatBukkit.isGlobalMuting
                if (TrChatBukkit.isGlobalMuting) {
                    sender.sendLang("Mute-Muted-All")
                } else {
                    sender.sendLang("Mute-Cancel-Muted-All")
                }
            }
        }
    }
}