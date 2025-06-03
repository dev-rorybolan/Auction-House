package plugins.sellOres

import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.inventory.ItemStack

data class Auction(
    val id: Int,
    val sellerUUID: String,
    val item: ItemStack,
    val price: Int
)
object Database {
    private var connection: Connection? = null

    fun connect() {
        val pluginFolder = java.io.File("plugins/SellOres")
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs()
        }

        connection = DriverManager.getConnection("jdbc:sqlite:plugins/SellOres/auctions.db")
        val stmt = connection!!.createStatement()
        val executeUpdate = stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS auctions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                seller_uuid TEXT NOT NULL,
                item_data BLOB NOT NULL,
                price INTEGER NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
        stmt.close()
    }

    fun saveAuction(sellerUUID: String, itemData: ByteArray, price: Int) {
        val sql = "INSERT INTO auctions (seller_uuid, item_data, price) VALUES (?, ?, ?)"
        val ps: PreparedStatement = connection!!.prepareStatement(sql)
        ps.setString(1, sellerUUID)
        ps.setBytes(2, itemData)
        ps.setInt(3, price)
        ps.executeUpdate()
        ps.close()
    }
    fun getAllAuctions(): List<Auction> {
        val list = mutableListOf<Auction>()
        val rs = connection!!.createStatement().executeQuery("SELECT id, seller_uuid, item_data, price FROM auctions")
        while (rs.next()) {
            val id = rs.getInt("id")
            val uuid = rs.getString("seller_uuid")
            val itemData = rs.getBytes("item_data")
            val price = rs.getInt("price")
            val item = deserializeItemStack(itemData)
            list.add(Auction(id, uuid, item, price))
        }
        rs.close()
        return list
    }


    fun deserializeItemStack(bytes: ByteArray): ItemStack {
        val inputStream = bytes.inputStream()
        val dataInput = BukkitObjectInputStream(inputStream)
        val item = dataInput.readObject() as ItemStack
        dataInput.close()
        return item
    }
    fun deleteAuction(id: Int) {
        val sql = "DELETE FROM auctions WHERE id = ?"
        val ps = connection!!.prepareStatement(sql)
        ps.setInt(1, id)
        ps.executeUpdate()
        ps.close()
    }
    fun getConnection(): Connection {
        return connection ?: throw IllegalStateException("Database not connected!")
    }

    fun getAuctionById(id: Int): Auction? {
        val conn = getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM auctions WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()

        return if (rs.next()) {
            val sellerUUID = rs.getString("seller_uuid")
            val itemBytes = rs.getBytes("item_data")
            val price = rs.getInt("price")

            val item = deserializeItemStack(itemBytes)
            Auction(id, sellerUUID, item, price)
        } else {
            null
        }.also {
            rs.close()
            stmt.close()
        }
    }


}
class SellOres : JavaPlugin() {

    companion object {
        lateinit var plugin: SellOres
        lateinit var econ: Economy
            private set
    }

    override fun onEnable() {
        plugin = this
        if (!setupEconomy()) {
            logger.severe("Vault or compatible economy plugin not found! Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }
        Database.connect()
        this.getCommand("sellores")?.setExecutor(SellCommand(this))
        this.getCommand("auction")?.setExecutor(AuctionCommand(this))
        this.getCommand("sell")?.setExecutor(UploadCommand(this))

        SellGUI.register(this)
        AuctionGUI.register(this)
        AddToAuctionGUI.register(this)
        server.pluginManager.registerEvents(JoinListener(), this)

        logger.info("SellOres has been enabled!")
    }

    override fun onDisable() {
        logger.info("Rory why did you touch it? SellOres has been disabled.")
    }


    private fun setupEconomy(): Boolean {
        val rsp: RegisteredServiceProvider<Economy>? =
            server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }
        econ = rsp.provider
        return econ != null
    }
}
