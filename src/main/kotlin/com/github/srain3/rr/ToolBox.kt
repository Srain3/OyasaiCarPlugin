package com.github.srain3.rr

import org.bukkit.ChatColor


object ToolBox {
    lateinit var plugin : RealRacing

    /**
     * 色付きメッセージ
     */
    fun colorMessage(msg: String): String {
        return ChatColor.translateAlternateColorCodes('&', msg)
    }

}