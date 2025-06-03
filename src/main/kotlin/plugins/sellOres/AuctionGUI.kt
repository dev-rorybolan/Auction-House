package plugins.sellOres

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

object AuctionGUI : Listener {

    private val auctionIdKey = NamespacedKey(SellOres.plugin, "auction_id")

    fun openAuctionGUI(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 54, "Auction House")
        val auctions = Database.getAllAuctions()

        for ((index, auction) in auctions.withIndex()) {
            if (index >= 54) break
            val (id, sellerUUID, item, price) = auction

            val meta = item.itemMeta ?: continue
            val sellerName = Bukkit.getOfflinePlayer(UUID.fromString(sellerUUID)).name ?: "Unknown"

            val lore = mutableListOf<String>()
            lore.add("§7Seller: §f$sellerName")
            lore.add("§7Price: §e$$price")
            meta.lore = lore

            meta.persistentDataContainer.set(auctionIdKey, PersistentDataType.INTEGER, id)
            item.itemMeta = meta

            inv.setItem(index, item)
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

        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val auctionId = container.get(auctionIdKey, PersistentDataType.INTEGER) ?: return

        val auction = Database.getAuctionById(auctionId) ?: return
        val price = auction.price

        val balance = SellOres.econ.getBalance(player)

        if (balance >= price) {
            SellOres.econ.withdrawPlayer(player as OfflinePlayer, price.toDouble())

            val sellerUUID = UUID.fromString(auction.sellerUUID)
            val sellerOffline = Bukkit.getOfflinePlayer(sellerUUID)
            SellOres.econ.depositPlayer(sellerOffline, price.toDouble())
            val sellerOnline = Bukkit.getPlayer(sellerUUID)
            if (sellerOnline != null) {
                sellerOnline.sendMessage("§aYour item was sold for §e$$price§a!")
            }

            player.inventory.addItem(auction.item)
            Database.deleteAuction(auctionId)

            player.sendMessage("§aYou bought the item for §e$$price§a!")

            event.currentItem = null
        }
        else {
            player.sendMessage("§cYou need §e$$price §cto buy this item.")
        }
    }
}
