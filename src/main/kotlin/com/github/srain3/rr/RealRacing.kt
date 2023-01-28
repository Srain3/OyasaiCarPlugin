package com.github.srain3.rr

import com.github.srain3.rr.car.Event
import com.github.srain3.rr.command.GentukiCommand
import com.github.srain3.rr.command.MachineCmdTab
import com.github.srain3.rr.command.MachineCommand
import com.github.srain3.rr.command.TuningEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class RealRacing : JavaPlugin() {
    override fun onEnable() {
        ToolBox.plugin = this
        server.pluginManager.registerEvents(Event, this)
        server.pluginManager.registerEvents(TuningEvent, this)

        server.getPluginCommand("machine")?.setExecutor(MachineCommand)
        server.getPluginCommand("machine")?.tabCompleter = MachineCmdTab
        server.getPluginCommand("gentuki")?.setExecutor(GentukiCommand)

        val offlineFixYML = FileBox.getCfg("offlinePlayerFix.yml")
        offlineFixYML.getKeys(false).forEach {
            val list = offlineFixYML.getList(it) ?: return@forEach
            val itemList = mutableListOf<ItemStack>()
            list.forEach { any ->
                if (any is ItemStack) {
                    itemList.add(any)
                }
            }
            Event.offlineFixList[UUID.fromString(it)] = itemList
        }
    }

    override fun onDisable() {
        Event.mainCarList.toSet().forEach {
            it.exit = true
            it.exitTask()
        }

        val offlineFixYML = FileBox.getCfg("offlinePlayerFixNew.yml")
        Event.offlineFixList.forEach { (uuid, itemList) ->
            offlineFixYML.set(uuid.toString(), itemList)
        }
        FileBox.saveFile("offlinePlayerFix.yml", offlineFixYML)
    }
}