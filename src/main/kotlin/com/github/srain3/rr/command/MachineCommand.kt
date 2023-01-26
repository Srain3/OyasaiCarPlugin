package com.github.srain3.rr.command

import com.github.srain3.rr.car.Event
import org.bukkit.Material
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.PI

object MachineCommand: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name != "machine") return false
        if (args.isEmpty()) return false
        if (!sender.isOp) return false
        when (args[0]) {
            "give" -> {
                if (sender !is Player) return false
                // give TopSpeed Power Brake Momentum
                if (args.size == 6) {
                    val list = listOf(
                        args[1].toIntOrNull(),
                        args[2].toIntOrNull(),
                        args[3].toIntOrNull(),
                        args[4].toIntOrNull(),
                        args[5].toIntOrNull()
                    )
                    val stringList = listOf(
                        "TopSpeed: ",
                        "Power: ",
                        "Brake: ",
                        "Momentum: ",
                        "Handling: "
                    )

                    val minecartItem = ItemStack(Material.MINECART)
                    val itemMeta = minecartItem.itemMeta?: return false
                    val newLore = mutableListOf<String>()
                    list.forEachIndexed { index, int ->
                        if (int != null) {
                            newLore.add(stringList[index] + "$int")
                        }
                    }
                    itemMeta.lore = newLore
                    itemMeta.setCustomModelData(831)
                    minecartItem.itemMeta = itemMeta

                    sender.inventory.addItem(minecartItem)
                    sender.sendMessage("[Machine] 車を渡しました")
                } else {
                    sender.sendMessage("[Machine] 引数が違います!")
                }
            }
            "giveTuning" -> {
                if (sender !is Player) return false
                // giveTuning Name TopSpeed Power Brake Momentum
                if (args.size == 6) {
                    val material = Material.PAPER
                    val name = if (args[1] == "null") {
                        null
                    } else {
                        args[1]
                    }
                    val list = listOf(
                        args[2],
                        args[3],
                        args[4],
                        args[5]
                    )
                    val stringList = listOf(
                        "TopSpeed: ",
                        "Power: ",
                        "Brake: ",
                        "Momentum: "
                    )

                    val paperItem = ItemStack(material)
                    val paperMeta = paperItem.itemMeta?: return false
                    paperMeta.setDisplayName(name)
                    val newLore = mutableListOf<String>()
                    list.forEachIndexed { index, str ->
                        if (index == 3) {
                            if (str != "null") {
                                newLore.add(stringList[index]+str.filter { it == '+' || it == '-'})
                            }
                            return@forEachIndexed
                        }
                        if (str != "null") {
                            newLore.add(stringList[index]+str.filter { it == '+' })
                        }
                    }
                    paperMeta.lore = newLore
                    paperItem.itemMeta = paperMeta

                    sender.inventory.addItem(paperItem)
                    sender.sendMessage("[Machine] チューニング券を渡しました")
                }
            }
            "set" -> { // 一時的な車出すコマンド
                // set TopSpeed Power Brake Momentum Yaw
                if (args.size in 5..7) {
                    val list = listOf(
                        args[1].toIntOrNull(),
                        args[2].toIntOrNull(),
                        args[3].toIntOrNull(),
                        args[4].toIntOrNull(),
                        args[5].toIntOrNull()
                    )
                    val stringList = listOf(
                        "TopSpeed: ",
                        "Power: ",
                        "Brake: ",
                        "Momentum: ",
                        "Handling: "
                    )

                    val minecartItem = ItemStack(Material.MINECART)
                    val itemMeta = minecartItem.itemMeta?: return false
                    val newLore = mutableListOf<String>()
                    list.forEachIndexed { index, int ->
                        if (int != null) {
                            newLore.add(stringList[index] + "$int")
                        }
                    }
                    itemMeta.lore = newLore
                    itemMeta.setCustomModelData(831)
                    minecartItem.itemMeta = itemMeta

                    var yaw = 0F
                    if (args.size == 7) {
                        yaw = (args[6].toDoubleOrNull() ?: 0.0).toFloat()
                    }

                    when (sender) {
                        is BlockCommandSender -> {
                            val addVec = Vector(0.0,0.0,1.0).rotateAroundY(PI/180.0*(yaw))
                            Event.spawnCar(sender.block.location.add(0.5,2.07,0.5).add(addVec), minecartItem, yaw, null)
                            sender.sendMessage("[Machine] 車を出しました")
                        }
                        is Player -> {
                            Event.spawnCar(sender.location.add(0.5,0.07,0.5), minecartItem, sender.eyeLocation.yaw, null)
                            sender.sendMessage("[Machine] 車を出しました")
                        }
                        else -> {
                            sender.sendMessage("[Machine] BlockCommandSenderかPlayerのみ受け付けます")
                        }
                    }

                } else {
                    sender.sendMessage("[Machine] 引数が違います!")
                }
            }
        }
        return true
    }
}