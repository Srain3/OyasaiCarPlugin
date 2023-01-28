package com.github.srain3.rr.command

import com.github.srain3.rr.car.Event
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object GentukiCommand: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (command.name != "gentuki") return false

        val stringList = listOf(
            "TopSpeed: 68",
            "Power: 45",
            "Brake: 20",
            "Momentum: 10",
            "Handling: 15"
        )
        val minecartItem = ItemStack(Material.MINECART)
        val itemMeta = minecartItem.itemMeta?: return false
        itemMeta.lore = stringList
        itemMeta.setCustomModelData(831)
        minecartItem.itemMeta = itemMeta

        when (sender) {
            is Player -> {
                    Event.spawnCar(sender.location.add(0.5,0.07,0.5), minecartItem, sender.eyeLocation.yaw, null)
                    sender.sendMessage("[Machine] 原付きを出しました(降りると消えます)")
                }
            is ConsoleCommandSender -> {
                if (args.isNullOrEmpty()) {
                    sender.sendMessage("[Machine] 引数不足です")
                } else if (Bukkit.getPlayer(args[0])?.isOnline == true) {
                    val player = Bukkit.getPlayer(args[0]) ?: return false
                    Event.spawnCar(player.location.add(0.5,0.07,0.5), minecartItem, player.eyeLocation.yaw, null)
                    player.sendMessage("[Machine] 原付きを出しました(降りると消えます)")
                } else {
                    sender.sendMessage("[Machine] 存在しないプレイヤーです(ConsoleCommand:/gentuki)")
                }
            }
        }
        return true
    }
}