@file:Suppress("UnstableApiUsage")

package cc.mewcraft.kommands

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.description.CommandDescription
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.DoubleParser
import org.incendo.cloud.parser.standard.StringParser
import kotlin.properties.Delegates

class Kommands : JavaPlugin() {

    /**
     * 已经载入的 [Kommand].
     */
    private val kommands = KommandMap()

    /**
     * 指令管理器 [LegacyPaperCommandManager].
     */
    private var manager: LegacyPaperCommandManager<CommandSender> by Delegates.notNull()

    /**
     * 日志记录器.
     */
    private val logger = slF4JLogger

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        //
        // Load config
        //
        loadKommands()

        //
        // Register commands
        //
        manager = LegacyPaperCommandManager(this, ExecutionCoordinator.simpleCoordinator(), SenderMapper.identity())
        manager.registerLegacyPaperBrigadier()
        registerCommands()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun loadKommands() {
        val commands = config.getConfigurationSection("commands")
        if (commands == null) {
            logger.warn("No commands found in config")
            return
        }

        for (key in commands.getKeys(false)) {
            val section = commands.getConfigurationSection(key)!!

            val simpleDescription = section.getString("simple_description") ?: "No simple description"
            val verboseDescription = section.getString("verbose_description") ?: "No verbose description"
            val commandFeedback = section.getString("feedback") ?: "No command feedback"

            val kommand = Kommand(
                simpleDescription,
                verboseDescription,
                commandFeedback
            )

            val mapKey = KommandType.valueOf(key.uppercase())
            val mapValue = kommand

            kommands[mapKey] = mapValue
        }
    }

    private fun registerCommands() {
        manager.buildAndRegister("kommands") {
            permission("kommands.admin")
            literal("reload")
            handler { ctx ->
                reloadConfig()
                loadKommands()
                ctx.sender().sendMessage(Component.text("Successfully reloaded config!").color(NamedTextColor.AQUA))
            }
        }

        manager.buildAndRegister("t") {
            senderType<Player>()
            literal("new")
            required("town_name", StringParser.stringParser())
            commandDescription(kommands[KommandType.TOWN_NEW].commandDescription)
            handler { ctx ->
                val kommand = kommands[KommandType.TOWN_NEW]
                val townName = ctx.get<String>("town_name")
                kommand.sendMessage(ctx.sender(), Placeholder.unparsed("town_name", townName))
            }
        }

        manager.buildAndRegister("t") {
            senderType<Player>()
            literal("add")
            required("player_name", StringParser.stringParser())
            commandDescription(kommands[KommandType.TOWN_ADD].commandDescription)
            handler { ctx ->
                val kommand = kommands[KommandType.TOWN_ADD]
                val playerName = ctx.get<String>("player_name")
                kommand.sendMessage(ctx.sender(), Placeholder.unparsed("player_name", playerName))
            }
        }

        manager.buildAndRegister("t") {
            senderType<Player>()
            literal("claim")
            commandDescription(kommands[KommandType.TOWN_CLAIM].commandDescription)
            handler { ctx ->
                val kommand = kommands[KommandType.TOWN_CLAIM]
                kommand.sendMessage(ctx.sender())
            }
        }

        manager.buildAndRegister("t") {
            senderType<Player>()
            literal("deposit")
            required("amount", DoubleParser.doubleParser(.0))
            commandDescription(kommands[KommandType.TOWN_DEPOSIT].commandDescription)
            handler { ctx ->
                val kommand = kommands[KommandType.TOWN_DEPOSIT]
                val amount = ctx.get<Double>("amount")
                kommand.sendMessage(ctx.sender(), Placeholder.component("amount", Component.text(amount)))
            }
        }

        manager.buildAndRegister("t") {
            senderType<Player>()
            literal("spawn")
            commandDescription(kommands[KommandType.TOWN_SPAWN].commandDescription)
            handler { ctx ->
                val kommand = kommands[KommandType.TOWN_SPAWN]
                kommand.sendMessage(ctx.sender())
            }
        }
    }
}

enum class KommandType {
    TOWN_ADD,
    TOWN_CLAIM,
    TOWN_DEPOSIT,
    TOWN_NEW,
    TOWN_SPAWN,
}

class KommandMap() {
    private val data = mutableMapOf<KommandType, Kommand>()

    operator fun get(type: KommandType): Kommand {
        return data[type] ?: throw IllegalArgumentException("No kommand found for type $type")
    }

    operator fun set(type: KommandType, kommand: Kommand) {
        data[type] = kommand
    }
}

/**
 * 封装了一个摹刻指令.
 */
class Kommand(
    simpleDescription: String,
    verboseDescription: String,
    val commandFeedback: String,
) {
    val commandDescription: CommandDescription = CommandDescription.commandDescription(simpleDescription, verboseDescription)

    fun sendMessage(audience: Audience, vararg placeholders: TagResolver) {
        val resolver = TagResolver.resolver(*placeholders)
        val component = MiniMessage.miniMessage().deserialize(commandFeedback, resolver)
        audience.sendMessage(component)
    }
}
