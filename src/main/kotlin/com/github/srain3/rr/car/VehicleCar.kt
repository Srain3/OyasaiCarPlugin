package com.github.srain3.rr.car

import org.bukkit.entity.Boat
import org.bukkit.entity.Item
import org.bukkit.entity.Minecart
import org.bukkit.util.Vector
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * トロッコを動かすclass
 */
data class VehicleCar(
    val speedLimit: Double,
    val power: Double,
    val brake: Double,
    val slip: Float,
    val handling: Double,

    var speed: Vector = Vector(),
    var slipAngle: Float = 0F
) {
    /**
     * スピード計算
     */
    fun controlSpeed(wasd: String?): Vector {
        //speed.multiply(0.99) // 転がり抵抗
        val addSpeed = Vector(0.0,0.0,0.9)
        when (wasd) {
            "W", "WA", "WD" -> {
                speed.multiply(0.965) // 摩擦的なやつ
                addSpeed.multiply(
                    power * min(
                        1.0,
                        max(0.35, (speed.z * (0.038 - (speed.z * 0.0034)) / (power * (0.8 - power))))
                    )
                )
            }

            "S", "SA", "SD" -> {
                speed.multiply(0.965) // 摩擦的なやつ
                addSpeed.multiply(
                    -brake * min(
                        1.0,
                        max(
                            0.35,
                            (speed.z.absoluteValue * (0.038 - (speed.z.absoluteValue * 0.0034)) / (brake * (0.8 - brake)))
                        )
                    )
                )
            }
            null -> { //無乗車(NoPlayer)
                addSpeed.zero()
                speed.multiply(0.625)
                if (speed.z in -0.03..0.03) {
                    speed.z = 0.0
                }
            }
            else -> {
                if (speed.z in -0.03..0.03) {
                    speed.multiply(0)
                }
                speed.multiply(0.9975) // 摩擦的なやつ
                addSpeed.multiply(0)
            }
        }
        speed.add(addSpeed)

        if (speed.z >= speedLimit) {
            speed.z = speedLimit
        } else if (speed.z <= -(speedLimit * 0.4)) {
            speed.z = -(speedLimit * 0.4)
        }

        return speed.clone()
    }

    /**
     * ハンドル操作
     */
    fun handling(wasd: String): Float {
        var float = handling.toFloat() * (max(0.5F, 3F - speed.z.toFloat()))
        if (float >= 12.5F) {
            float = 12.5F
        }
        return when (wasd) {
            "A", "WA", "SA" -> { //ハンドル左
                -float
            }
            "D", "WD", "SD" -> { //ハンドル右
                float
            }
            else -> { //まっすぐ
                0F
            }
        }
    }

    /**
     * スリップの挙動
     */
    fun slip(wasd: String): Float {
        // スリップ(滑る)または曲がる挙動用
        when (wasd) {
            "A", "WA", "SA" -> {
                slipAngle = (slipAngle + slip + min(1.5F,speed.z.absoluteValue.toFloat()*1.2F)) * 0.899F
                if (wasd != "A") {
                    if (wasd == "SA") {
                        slipAngle *= 0.97F
                    }
                    //if (!jumpSwitch) {
                        speed.multiply(0.9925)
                    //}
                } else {
                    //if (!jumpSwitch) {
                        speed.multiply(0.995)
                    //}
                }
            }
            "D", "WD", "SD" -> {
                slipAngle = (slipAngle - slip - min(1.5F,speed.z.absoluteValue.toFloat()*1.2F)) * 0.899F
                if (wasd != "D") {
                    if (wasd == "SD") {
                        slipAngle *= 0.97F
                    }
                    //if (!jumpSwitch) {
                        speed.multiply(0.9925)
                    //}
                } else {
                    //if (!jumpSwitch) {
                        speed.multiply(0.995)
                    //}
                }
            }

            "W" -> {
                slipAngle *= min(0.975F,(slip/100.0F+0.9F))
            }

            "S" -> {
                slipAngle *= min(0.925F,(slip/100.0F+0.85F))
            }

            else -> {
                slipAngle *= 0.95F
            }
        }
        return slipAngle
    }

    /**
     * 段差で登らせる
     */
    fun jump(dropItem: Item, minecart: Minecart): Double {
        if (speed.z <= 0.1) return 0.0

        val selectVec = Vector(0.0,0.0,1.0).rotateAroundY(-PI /180*(minecart.location.yaw+90F+slipAngle))

        val rtb = dropItem.world.rayTraceBlocks(dropItem.location, selectVec, 2.0) ?: return 0.0
        val hitBlock = rtb.hitBlock ?: return 0.0

        if (hitBlock.isPassable) return 0.0
        val upBlock = hitBlock.location.add(0.0,1.0,0.0).block
        if (!upBlock.isEmpty) {
            if (!upBlock.isPassable) return 0.0
        }

        return 0.45 * (1 + (speed.z * 0.25))
    }

    /**
     * 落下処理
     */
    fun down(dropItem: Item): Double {
        val selectVec = Vector(0.0,-1.0,0.0)

        val rtb = dropItem.world.rayTraceBlocks(dropItem.location, selectVec, 0.07) ?: return -0.525
        val hitBlock = rtb.hitBlock ?: return -0.525

        if (hitBlock.isPassable) return -0.525
        //if (minecart.velocity.x in -0.02..0.02 && minecart.velocity.z in -0.02..0.02) return -0.525

        return 0.0
    }

    /**
     * スリップストリームの処理
     */
    fun slipstream(minecart: Minecart): Boolean {
        if (speed.z <= 0.1) return false
        val loc = minecart.location.clone()
        val vec = Vector(0.0,0.0,3.65).rotateAroundY(-PI /180*(minecart.location.yaw+90F+slipAngle))
        loc.world?.rayTraceEntities(loc.add(vec), vec, ((speed.z + 0.01) * 7.5), 2.0) {
            it is Boat || it is Minecart
        } ?: return false
        speed.z += 0.001
        if (speed.z >= speedLimit) {
            speed.z = speedLimit
        }
        return true
    }
}
