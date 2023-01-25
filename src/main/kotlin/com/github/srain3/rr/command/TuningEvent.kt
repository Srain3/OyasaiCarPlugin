package com.github.srain3.rr.command

import com.github.srain3.rr.car.Event
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag

object TuningEvent: Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun clickEvent(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (!(event.action == Action.LEFT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_AIR)) return
        if (!event.hasItem()) return
        if (event.item?.type != Material.PAPER) return
        val paper = event.item ?: return
        val offHand = event.player.inventory.itemInOffHand
        if (offHand.type != Material.MINECART) return
        if (offHand.itemMeta?.hasCustomModelData() != true) return
        if (offHand.itemMeta?.customModelData != 831) return
        if (offHand.itemMeta?.lore?.isNotEmpty() != true) return

        event.isCancelled = true

        val boatMeta = offHand.itemMeta ?: return
        val boatLore = boatMeta.lore ?: return
        val newLore = boatLore.toMutableList()

        paper.itemMeta?.lore?.forEach { line ->
            if (topSpeedPlusRegex.matches(line)) {
                // 最高速設定
                val rawStr = line.replace("TopSpeed: ","")
                boatLore.forEachIndexed { index, boatLine ->
                    if (Event.topSpeedRegex.matches(boatLine)) {
                        newLore[index] += rawStr
                        val count = newLore[index].count { it == '+' }
                        if (count > 10) {
                            val removeCount = count-10
                            newLore[index] = newLore[index].dropLast(removeCount)
                        }
                    }
                }
            } else if (powerPlusRegex.matches(line)) {
                // エンジンパワー設定(加速力)
                val rawStr = line.replace("Power: ","")
                boatLore.forEachIndexed { index, boatLine ->
                    if (Event.powerRegex.matches(boatLine)) {
                        newLore[index] += rawStr
                        val count = newLore[index].count { it == '+' }
                        if (count > 10) {
                            val removeCount = count-10
                            newLore[index] = newLore[index].dropLast(removeCount)
                        }
                    }
                }
            } else if (brakePlusRegex.matches(line)) {
                // ブレーキ力(減速力)
                val rawStr = line.replace("Brake: ","")
                boatLore.forEachIndexed { index, boatLine ->
                    if (Event.brakeRegex.matches(boatLine)) {
                        newLore[index] += rawStr
                        val count = newLore[index].count { it == '+' }
                        if (count > 10) {
                            val removeCount = count-10
                            newLore[index] = newLore[index].dropLast(removeCount)
                        }
                    }
                }
            } else if (slipPlusRegex.matches(line)) {
                // モーメント(スリップ)力
                val rawStr = line.replace("Momentum: ","")
                boatLore.forEachIndexed { index, boatLine ->
                    if (Event.slipRegex.matches(boatLine)) {
                        newLore[index] += rawStr
                        val count = newLore[index].count { it == '+' || it == '-' }
                        if (count > 10) {
                            val removeCount = count-10
                            newLore[index] = newLore[index].dropLast(removeCount)
                        }
                    }
                }
            }
        }

        boatMeta.lore = newLore
        boatMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        offHand.itemMeta = boatMeta
        offHand.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 0)

        paper.amount = paper.amount-1
    }

    private val topSpeedPlusRegex = Regex("""TopSpeed: (\+*)+""")
    private val powerPlusRegex = Regex("""Power: (\+*)+""")
    private val brakePlusRegex = Regex("""Brake: (\+*)+""")
    private val slipPlusRegex = Regex("""Momentum: (\+*|-*)+""")
}