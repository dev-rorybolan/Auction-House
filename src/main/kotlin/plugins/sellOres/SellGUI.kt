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

object SellGUI : Listener {

    private val sellableOres = mapOf(
        Material.IRON_ORE to 5,
        Material.GOLD_ORE to 8,
        Material.DIAMOND_ORE to 25,
        Material.EMERALD_ORE to 30,
        Material.COPPER_ORE to 3
    )

    fun openSellGUI(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 9, "Sell Your Ores")

        val startSlot = 2
        for ((index, material) in sellableOres.keys.withIndex()) {
            if (startSlot + index >= 9) break
            inv.setItem(startSlot + index, ItemStack(material))
        }

        player.openInventory(inv)
    }

    fun register(plugin: JavaPlugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title != "Sell Your Ores") return
        event.isCancelled = true

        val item = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return

        if (item.type in sellableOres.keys) {
            val count = removeItems(player, item.type)
            val pricePerItem = sellableOres[item.type] ?: 0
            val profit = count * pricePerItem
            SellOres.econ.depositPlayer(player, profit.toDouble())

            player.sendMessage("§aSold $count ${item.type.name.lowercase().replace('_', ' ')} for §e$${profit}")
        }
    }

    private fun removeItems(player: Player, material: Material): Int {
        var removed = 0
        val inv = player.inventory

        for (item in inv.contents) {
            if (item != null && item.type == material) {
                removed += item.amount
                inv.remove(item)
            }
        }

        return removed
    }
}
