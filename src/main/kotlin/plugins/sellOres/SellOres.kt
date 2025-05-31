package plugins.sellOres

import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

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
}
class SellOres : JavaPlugin() {

    companion object {
        lateinit var econ: Economy
            private set
    }

    override fun onEnable() {
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
