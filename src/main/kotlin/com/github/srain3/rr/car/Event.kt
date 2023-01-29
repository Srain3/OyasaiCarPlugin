package com.github.srain3.rr.car

import com.github.srain3.rr.ToolBox
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Rail
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

object Event: Listener {
    @EventHandler
    fun getRightClickIsMinecart(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (!event.hasItem()) return
        if (!event.hasBlock()) return
        if (event.item?.type != Material.MINECART) return
        if (event.item?.itemMeta?.hasCustomModelData() != true) return
        if (event.item?.itemMeta?.customModelData != 831) return
        if (event.clickedBlock?.blockData is Rail) {
            event.isCancelled = true
            return
        }
        if (event.blockFace != BlockFace.UP) return

        val player = event.player
        val spawnLoc = event.clickedBlock!!.location.add(0.5,1.07,0.5)
        val item = event.item!!.clone()

        spawnCar(spawnLoc, item, player.eyeLocation.yaw, player)

        player.inventory.itemInMainHand.amount = 0
    }

    fun spawnCar(spawnLoc: Location, item: ItemStack, yaw: Float, owner: Player?, debug: Boolean = false) {
        val dropItem = spawnLoc.world?.spawnEntity(spawnLoc, EntityType.DROPPED_ITEM) as Item
        dropItem.itemStack = ItemStack(Material.SNOWBALL)
        dropItem.isSilent = true
        dropItem.setGravity(false)
        dropItem.pickupDelay = Int.MAX_VALUE

        val minecart = spawnLoc.world?.spawnEntity(spawnLoc, EntityType.MINECART)
        if (minecart !is Minecart) return
        minecart.setRotation(yaw-90F, 0F)

        val nameSpacedKey = NamespacedKey(ToolBox.plugin, "${minecart.uniqueId}")
        val bossBar = Bukkit.createBossBar(nameSpacedKey, "| 000km/h |", BarColor.WHITE, BarStyle.SEGMENTED_10)

        val vehicleCar = convertCarStatus(getCarStatus(item.itemMeta!!))
        val driftColor = getDriftColor(item.itemMeta!!)
        val mainCar = MainCar(minecart, dropItem, bossBar, nameSpacedKey, vehicleCar, owner, item, debug = debug, driftColor = driftColor)
        mainCarList.add(mainCar)
        mainCar.start()
    }

    val mainCarList = mutableListOf<MainCar>()

    //ステータス取得用マッチング文字
    val topSpeedRegex = Regex("""TopSpeed: [0-9]+(\+*)?""")
    val powerRegex = Regex("""Power: [0-9]+(\+*)?""")
    val brakeRegex = Regex("""Brake: [0-9]+(\+*)?""")
    val slipRegex = Regex("""Momentum: [0-9]+(\+*|-*){0,300}""")
    private val handlingRegex = Regex("""Handling: [0-9]+""")
    val driftColorRegex = Regex("""DriftColor: """)

    /**
     * 車のステータス取得
     */
    private fun getCarStatus(meta: ItemMeta): MutableList<Int?> {
        var topSpeedInt = 50
        var powerInt = 40
        var brakeInt = 20
        var slipInt = 25
        var handlingInt = 27
        meta.lore?.forEach { line ->
            if (topSpeedRegex.matches(line)) {
                // 最高速設定
                val rawStr = line.replace("TopSpeed: ","")
                topSpeedInt = rawStr.replace("+","").toInt()
                topSpeedInt += rawStr.count { it == '+' } * 10
            } else if (powerRegex.matches(line)) {
                // エンジンパワー設定(加速力)
                val rawStr = line.replace("Power: ","")
                powerInt = rawStr.replace("+","").toInt()
                powerInt += rawStr.count { it == '+' } * 2
            } else if (brakeRegex.matches(line)) {
                // ブレーキ力(減速力)
                val rawStr = line.replace("Brake: ","")
                brakeInt = rawStr.replace("+","").toInt()
                brakeInt += rawStr.count { it == '+' } * 2
            } else if (slipRegex.matches(line)) {
                // モーメント(スリップ)力
                val rawStr = line.replace("Momentum: ","")
                slipInt = rawStr.replace("+","").replace("-","").toInt()
                slipInt += rawStr.count { it == '+' } * 2
                slipInt -= rawStr.count { it == '-' } * 2
                if (slipInt < 0) {
                    slipInt = 0
                }
            } else if (handlingRegex.matches(line)) {
                // ハンドリング性
                val rawStr = line.replace("Handling: ", "").toInt()
                handlingInt = rawStr
            }
        }

        if (topSpeedInt > 320) {
            topSpeedInt = 320
        }
        if (powerInt > 500) {
            powerInt = 500
        }
        if (brakeInt > 500) {
            brakeInt = 500
        }
        if (slipInt >150) {
            slipInt = 150
        }
        if (handlingInt > 150) {
            handlingInt = 150
        }

        return mutableListOf(topSpeedInt,powerInt,brakeInt,slipInt,handlingInt)
    }

    /**
     * ドリフトパーティクルの色取得
     */
    private fun getDriftColor(meta: ItemMeta): MutableList<Color> {
        val colorList = mutableListOf<Color>()
        meta.lore?.forEach { line ->
            if (driftColorRegex.containsMatchIn(line)) {
                // ドリフトパーティクルの色
                val colorStrList = driftColorRegex.replace(line, "").split(',')
                colorStrList.forEach { colorStr ->
                    when (colorStr) {
                        "AQUA" -> {
                            colorList.add(Color.AQUA)
                        }

                        "BLACK" -> {
                            colorList.add(Color.BLACK)
                        }

                        "BLUE" -> {
                            colorList.add(Color.BLUE)
                        }

                        "FUCHSIA" -> {
                            colorList.add(Color.FUCHSIA)
                        }

                        "GRAY" -> {
                            colorList.add(Color.GRAY)
                        }

                        "GREEN" -> {
                            colorList.add(Color.GREEN)
                        }

                        "LIME" -> {
                            colorList.add(Color.LIME)
                        }

                        "MAROON" -> {
                            colorList.add(Color.MAROON)
                        }

                        "NAVY" -> {
                            colorList.add(Color.NAVY)
                        }

                        "OLIVE" -> {
                            colorList.add(Color.OLIVE)
                        }

                        "ORANGE" -> {
                            colorList.add(Color.ORANGE)
                        }

                        "PURPLE" -> {
                            colorList.add(Color.PURPLE)
                        }

                        "RED" -> {
                            colorList.add(Color.RED)
                        }

                        "SILVER" -> {
                            colorList.add(Color.SILVER)
                        }

                        "TEAL" -> {
                            colorList.add(Color.TEAL)
                        }

                        "WHITE" -> {
                            colorList.add(Color.WHITE)
                        }

                        "YELLOW" -> {
                            colorList.add(Color.YELLOW)
                        }
                    }
                }
            }
        }
        if (colorList.size == 0) {
            colorList.add(Color.SILVER)
        }
        return colorList
    }

    /**
     * VehicleCarの値へステータス変換
     */
    private fun convertCarStatus(intList: MutableList<Int?>): VehicleCar {
        var topSpeed = 1.0
        var power = 0.4
        var brake = 0.2
        var slip = 2.5F
        var handling = 2.7

        intList.forEachIndexed { index, num ->
            when (index) {
                0 -> {
                    topSpeed = (num ?: return@forEachIndexed) * 0.01
                }
                1 -> {
                    power = (num ?: return@forEachIndexed) * 0.001
                }
                2 -> {
                    brake = (num ?: return@forEachIndexed) * 0.001
                }
                3 -> {
                    slip = ((num ?: return@forEachIndexed) * 0.1).toFloat()
                }
                4 -> {
                    handling = ((num ?: return@forEachIndexed) * 0.1)
                }
            }
        }

        return VehicleCar(topSpeed,power,brake,slip,handling)
    }

    val offlineFixList = mutableMapOf<UUID,MutableList<ItemStack>>()

    /**
     * オフライン時に車が壊された時などでアイテムがインベントリに入らない場合の修正
     */
    @EventHandler
    fun joinPlayer(event: PlayerJoinEvent) {
        val itemList = offlineFixList[event.player.uniqueId] ?: return
        itemList.forEach {
            event.player.inventory.addItem(it)
        }
        offlineFixList.remove(event.player.uniqueId)
    }

    /**
     * 雪玉が壊れるの防止1
     */
    @EventHandler
    fun snowBallDeath(event: ProjectileHitEvent) {
        if (mainCarList.any { it.dropItem == event.entity }) {
            event.isCancelled = true
            event.entity.setBounce(true)
        }
    }

    /**
     * 壁に当たるダメージ軽減
     */
    @EventHandler
    fun wallDamageCancel(event: EntityDamageEvent) {
        if (!(event.cause == EntityDamageEvent.DamageCause.FALL ||
            event.cause == EntityDamageEvent.DamageCause.SUFFOCATION)) return
        if (event.entity !is Player) return
        if (!mainCarList.any { it.getControlPlayer()?.uniqueId == event.entity.uniqueId }) return
        event.isCancelled = true
    }
}