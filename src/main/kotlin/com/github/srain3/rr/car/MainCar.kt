package com.github.srain3.rr.car

import com.github.srain3.rr.ToolBox
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BossBar
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.geysermc.floodgate.api.FloodgateApi
import org.geysermc.geyser.GeyserImpl
import org.geysermc.geyser.translator.protocol.java.entity.JavaMoveVehicleTranslator
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

// MEMO: トロッコ->プレイヤーは+90F プレイヤー->トロッコは-90F 左にトロッコの視線(F3+B)が来るのが正面向き

/**
 * トロッコを車っぽくさせるメインclass
 */
data class MainCar(
    val minecart: Minecart,
    var dropItem: Item,
    val bossBar: BossBar,
    val bossBarKey: NamespacedKey,
    val vehicleCar: VehicleCar,
    val owner: Player?,
    val item: ItemStack,

    var exit: Boolean = false,
    var removeFlag: Boolean = false,
    var deathLoc: Location = Location(null,0.0,0.0,0.0)
) {
    /**
     * 開始の処理
     */
    fun start() {
        dropItem.addPassenger(minecart)
        timerTask()
    }

    /**
     * タイマーの処理
     */
    private fun timerTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (minecart.isDead) {
                    if (!exit) {
                        exitTask()
                    }
                    object : BukkitRunnable() {
                        override fun run() {
                            deathLoc.world?.getNearbyEntities(deathLoc, 0.5,0.5,0.5) {
                                it is Item
                            }?.toSet()?.forEach {
                                val item = (it as Item)
                                if (item.itemStack.type == Material.MINECART) {
                                    it.remove()
                                }
                            }
                        }
                    }.runTaskLater(ToolBox.plugin, 1)
                    cancel() ; return
                }

                if (dropItem.isDead) {
                    val newEntity = minecart.world.spawnEntity(minecart.location, EntityType.DROPPED_ITEM) as Item
                    newEntity.itemStack = ItemStack(Material.SNOWBALL)
                    newEntity.setGravity(false)
                    newEntity.pickupDelay = Int.MAX_VALUE
                    dropItem = newEntity

                    dropItem.addPassenger(minecart)

                    vehicleCar.speed.multiply(0.9)
                }

                val player = getControlPlayer()
                updateBar()
                updateBarPlayer(player)

                if (player == null) {
                    val speed = vehicleCar.controlSpeed(null)
                    val addVec = speed.rotateAroundY(PI/180.0*-(minecart.location.yaw+90F))

                    var jumpY = vehicleCar.jump(dropItem, minecart)
                    if (jumpY == 0.0) {
                        jumpY = vehicleCar.down(dropItem)
                    }
                    if (jumpY != 0.0) {
                        addVec.y = jumpY
                    }
                    if(dropItem.velocity.z.isNaN()) {
                        dropItem.velocity = Vector()
                    }
                    dropItem.velocity = addVec

                    if (removeFlag) {
                        exit = true
                        exitTask()
                    }
                    return
                }

                if (owner == null) {
                    removeFlag = true
                }

                val wasd = getWASD(player)
                //player.sendMessage(wasd)

                updateBar(vehicleCar.slipstream(minecart))

                val speed = vehicleCar.controlSpeed(wasd)
                val addYaw = vehicleCar.handling(wasd)
                val yaw = minecart.location.yaw + addYaw

                brakeLamp(wasd, minecart.location.yaw)
                minecart.setRotation(yaw, 0F)

                //snowBall.velocity = speed.rotateAroundY(PI/180.0*-(yaw + 90F + vehicleCar.slip(wasd)))
                val addVec = speed.rotateAroundY(PI/180.0*-(yaw + 90F + vehicleCar.slip(wasd)))

                var jumpY = vehicleCar.jump(dropItem, minecart)
                if (jumpY == 0.0) {
                    jumpY = vehicleCar.down(dropItem)
                }
                if (jumpY != 0.0) {
                    addVec.y = jumpY
                }
                if(dropItem.velocity.z.isNaN()) {
                    dropItem.velocity = Vector()
                }
                dropItem.velocity = addVec

                updateHeadLight(player)
                smokeParticle(vehicleCar.speed.z, vehicleCar.slipAngle)

                if (FloodgateApi.getInstance().isFloodgateId(player.uniqueId)) {
                    bedRockConvert(player, addYaw)
                }
            }
        }.runTaskTimer(ToolBox.plugin, 1, 1)
    }

    /**
     * Task終了処理
     */
    fun exitTask() {
        bossBar.removeAll()
        Bukkit.removeBossBar(bossBarKey)
        deathLoc = minecart.location.clone()
        minecart.remove() ; dropItem.remove()
        if (owner != null) {
            if (owner.isOnline) {
                owner.inventory.addItem(item)
                //Bukkit.getLogger().info("exitTask:Online")
            } else {
                val itemList = Event.offlineFixList[owner.uniqueId] ?: mutableListOf()
                itemList.add(item)
                Event.offlineFixList[owner.uniqueId] = itemList
                //Bukkit.getLogger().info("exitTask:null")
            }
        }
        Event.mainCarList.remove(this@MainCar)
    }

    /**
     * コントロールプレイヤー取得
     */
    fun getControlPlayer(): Player? {
        return minecart.passengers.filterIsInstance<Player>().firstOrNull()
    }

    /**
     * PlayerのWASD取得
     */
    private fun getWASD(player: Player): String {
        val pVec = player.velocity.clone()
        pVec.y = 0.0
        pVec.rotateAroundY(-PI /180*(-player.eyeLocation.yaw))
        pVec.multiply(115)
        return when {
            pVec.z >= 1.0 -> { // W
                when {
                    pVec.x >= 1.0 -> { // A
                        "WA"
                    }
                    pVec.x <= -1.0 -> { // D
                        "WD"
                    }
                    else -> {
                        "W"
                    }
                }
            }
            pVec.z <= -1.0 -> { // S
                when {
                    pVec.x >= 1.0 -> { // A
                        "SA"
                    }
                    pVec.x <= -1.0 -> { // D
                        "SD"
                    }
                    else -> {
                        "S"
                    }
                }
            }
            else -> {
                when {
                    pVec.x >= 1.0 -> { // A
                        "A"
                    }
                    pVec.x <= -1.0 -> { // D
                        "D"
                    }
                    else -> {
                        ""
                    }
                }
            }
        }
    }

    /**
     * ブレーキランプの処理
     */
    private fun brakeLamp(wasd: String, yaw: Float) {
        val leftVec = Vector(-0.4,0.15,-0.65).rotateAroundY(PI/180.0*-(yaw+90F))
        val rightVec = Vector(0.4,0.15,-0.65).rotateAroundY(PI/180.0*-(yaw+90F))

        val leftLoc = minecart.location.clone().add(leftVec)
        val rightLoc = minecart.location.clone().add(rightVec)

        val data =  when (wasd) {
            "S", "SA", "SD" -> {
                Particle.DustOptions(Color.RED, 1.75F)
            }
            else -> {
                Particle.DustOptions(Color.RED, 0.65F)
            }
        }
        minecart.world.spawnParticle(Particle.REDSTONE,leftLoc,1, data)
        minecart.world.spawnParticle(Particle.REDSTONE,rightLoc,1, data)
    }

    /**
     * タイヤスモーク(っぽいやつ)の処理
     */
    private fun smokeParticle(speed: Double, slipAngle: Float) {
        if (slipAngle.absoluteValue >= 24.5F) {
            if (speed <= 0.1) return
            val speedToCount = (speed * 0.025).roundToInt()
            val angleToCount = (0.0001F + slipAngle.absoluteValue * 0.0625).roundToInt()
            Particle.DustOptions(
                Color.SILVER, 1F+ min((speed + 0.001) * 0.5, 1.0).toFloat())
            minecart.world.spawnParticle(
                Particle.REDSTONE,
                minecart.location,
                (speedToCount+angleToCount),
                (0..10).random()*0.1,
            (0..4).random()*0.1,
            (0..10).random()*0.1,
                Particle.DustOptions(
                    Color.SILVER, 0.25F+ min((speed + 0.001) * 3.0, 1.5).toFloat()
                )
            )
        }
    }

    /**
     * ボスバーの更新
     */
    private fun updateBar(slipstreamSwitch: Boolean = false) {
        if(vehicleCar.speed.z.isNaN()) {
            vehicleCar.speed = Vector()
        }
        val barPercent = vehicleCar.speed.z / vehicleCar.speedLimit
        var speedString = (vehicleCar.speed.z * 100).roundToInt().toString()
        if (barPercent in -0.1..0.1) {
            bossBar.color = BarColor.WHITE
            speedString = "&r&l${speedString}&rkm/h"
        } else if (barPercent > 0.05) {
            bossBar.color = BarColor.GREEN
            speedString = "&a&l${speedString}&r&akm/h"
        } else if (barPercent < -0.05) {
            bossBar.color = BarColor.RED
            speedString = "&c&l${speedString}&r&ckm/h"
        }
        bossBar.progress = barPercent.absoluteValue

        if (slipstreamSwitch) {
            bossBar.color = BarColor.YELLOW
        }

        bossBar.setTitle(ToolBox.colorMessage("| $speedString &r|"))
    }

    /**
     * ボスバーの表示プレイヤー更新
     */
    private fun updateBarPlayer(player: Player?) {
        if (player != null) {
            if (!bossBar.players.contains(player)) {
                bossBar.addPlayer(player)
            }
        } else {
            bossBar.removeAll()
        }
    }

    /**
     * ヘッドライト(暗視効果)の更新
     */
    private fun updateHeadLight(player: Player) {
        if (player.location.clone().apply { y += 0.75 }.block.lightFromSky < 3 && player.location.clone().apply { y += 0.75 }.block.lightFromBlocks < 1) {
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                if (player.getPotionEffect(PotionEffectType.NIGHT_VISION)!!.duration <= 4) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, 4, 0, false, false, false))
                }
            } else {
                player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, 4, 0, false, false, false))
            }
        }
    }

    /**
     * 統合版対応コード
     */
    private fun bedRockConvert(player: Player, yaw: Float) {
        val session = GeyserImpl.getInstance().connectionByUuid(player.uniqueId)
        if (session != null) {
            val x = dropItem.location.x + (dropItem.velocity.x * 3)
            val y = minecart.location.y + (dropItem.velocity.y * 3)
            val z = dropItem.location.z + (dropItem.velocity.z * 3)
            val pitch = minecart.location.pitch

            val bedrockBoat = session.entityCache.getEntityByJavaId(minecart.entityId)
            bedrockBoat.updateRotation((minecart.location.yaw)+yaw,0F,bedrockBoat.isOnGround)
            JavaMoveVehicleTranslator().translate(session, ClientboundMoveVehiclePacket(x,y,z,bedrockBoat.yaw,pitch))
        }
    }
}