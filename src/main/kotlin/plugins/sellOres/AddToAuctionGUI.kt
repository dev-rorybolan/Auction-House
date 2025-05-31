package plugins.sellOres

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.util.io.BukkitObjectOutputStream

object AddToAuctionGUI : Listener {

    private val awaitingPriceInput = mutableMapOf<UUID, ItemStack>()

    fun openUploadGUI(player: Player) {
        val inv: Inventory = Bukkit.createInventory(player, 54, "Upload Auction - Shift-click item to list")
        player.openInventory(inv)
        player.sendMessage("Shift-click an item in your inventory to list it for auction.")
    }

    fun register(plugin: JavaPlugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.title != "Upload Auction - Shift-click item to list") return

        if (event.clickedInventory != player.inventory) {
            event.isCancelled = true
            return
        }

        if (!event.isShiftClick) {
            event.isCancelled = true
            return
        }

        val item = event.currentItem ?: return
        if (item.type == Material.AIR) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        player.closeInventory()

        awaitingPriceInput[player.uniqueId] = item.clone()
        player.sendMessage("Please type the price to list your ${item.type.name.lowercase().replace('_', ' ')} for auction in chat.")
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (!awaitingPriceInput.containsKey(uuid)) return

        event.isCancelled = true

        val priceString = event.message
        val price = priceString.toIntOrNull()

        if (price == null || price <= 0) {
            player.sendMessage("§cInvalid price. Please enter a positive number.")
            return
        }

        val item = awaitingPriceInput.remove(uuid) ?: return
        val itemBytes = serializeItemStack(item)

        Database.saveAuction(player.uniqueId.toString(), itemBytes, price)
        player.inventory.removeItem(item)

        player.sendMessage("§aYour ${item.type.name.lowercase().replace('_', ' ')} has been listed for §e$$price §afor auction!")
    }

    fun serializeItemStack(item: ItemStack): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val dataOutput = BukkitObjectOutputStream(outputStream)
        dataOutput.writeObject(item)
        dataOutput.close()
        return outputStream.toByteArray()
    }

}
