package plugins.sellOres

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer

object AuctionGUI : Listener {
    fun openAuctionGUI(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 54, "Auction House")
        for (i in 0..53) {
            inv.setItem(i, ItemStack(Material.ANCIENT_DEBRIS))
        }
        player.openInventory(inv)
    }
    fun register(plugin: JavaPlugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title != "Auction House") return
        event.isCancelled = true

        val item = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return

        val type = item.type
        val pricePerStack = 45
        val totalCost = pricePerStack * 64

        val balance = SellOres.econ.getBalance(player)

        if (balance >= totalCost) {
            SellOres.econ.withdrawPlayer(player as OfflinePlayer, totalCost.toDouble())

            val stack = ItemStack(Material.ANCIENT_DEBRIS, 64)
            player.inventory.addItem(stack)

            player.sendMessage("§aPurchased 1 stack of ${type.name.lowercase().replace('_', ' ')} for §e$${totalCost}")
        } else {
            player.sendMessage("§cYou need $${totalCost} to buy this stack (45 stacks of value).")
        }
    }
    }

