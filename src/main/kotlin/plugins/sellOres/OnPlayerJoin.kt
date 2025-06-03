package plugins.sellOres

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class JoinListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.sendMessage("§aWelcome back, ${player.name}!")
        player.sendMessage("§2To put items up for auction run /sell")
        player.sendMessage("§2To sell ores run /sellores")
        player.sendMessage("§2To view the Auction House and shop run /auction")
    }
}
