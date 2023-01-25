package com.github.srain3.rr.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

object MachineCmdTab: TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        if (command.name != "machine") return null
        if (!sender.isOp) return null
        return when (args.size) {
            0,1 -> {
                mutableListOf("give","giveTuning","set")
            }

            2 -> {
                return when (args[0]) {
                    "give" -> {
                        mutableListOf("TopSpeed(1～320)")
                    }
                    "giveTuning" -> {
                        mutableListOf("[Name(文字)]")
                    }
                    "set" -> {
                        mutableListOf("TopSpeed(1～320)")
                    }
                    else -> {
                        null
                    }
                }
            }

            3 -> {
                return when (args[0]) {
                    "give" -> {
                        mutableListOf("Power(1～500)")
                    }
                    "giveTuning" -> {
                        mutableListOf("[TopSpeed]")
                    }
                    "set" -> {
                        mutableListOf("Power(1～500)")
                    }
                    else -> {
                        null
                    }
                }
            }
            4 -> {
                return when (args[0]) {
                    "give" -> {
                        mutableListOf("Brake(1～500)")
                    }
                    "giveTuning" -> {
                        mutableListOf("[Power]")
                    }
                    "set" -> {
                        mutableListOf("Brake(1～500)")
                    }
                    else -> {
                        null
                    }
                }
            }
            5 -> {
                return when (args[0]) {
                    "give" -> {
                        mutableListOf("Momentum(0～150)")
                    }
                    "giveTuning" -> {
                        mutableListOf("[Brake]")
                    }
                    "set" -> {
                        mutableListOf("Momentum(0～150)")
                    }
                    else -> {
                        null
                    }
                }
            }
            6 -> {
                return when (args[0]) {
                    "give" -> {
                        mutableListOf("Handling(1～899)")
                    }
                    "giveTuning" -> {
                        mutableListOf("[Momentum]")
                    }
                    "set" -> {
                        mutableListOf("Handling(1～899)")
                    }
                    else -> {
                        null
                    }
                }
            }
            7 -> {
                return when (args[0]) {
                    "set" -> {
                        mutableListOf("Yaw(-180.0～180.0)")
                    }
                    else -> {
                        null
                    }
                }
            }
            8 -> {
                return when (args[0]) {
                    else -> {
                        null
                    }
                }
            }

            else -> {
                null
            }
        }
    }
}