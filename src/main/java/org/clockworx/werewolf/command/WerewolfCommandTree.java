package org.clockworx.werewolf.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.commands.WerewolfCommand;
import static org.clockworx.werewolf.command.CommandSuggestions.*;

/**
 * Builds the Brigadier command tree for the Werewolf plugin.
 * This class creates the command structure using Paper's Brigadier API.
 */
public class WerewolfCommandTree {
    
    private final WerewolfPlugin plugin;
    private final WerewolfCommand commandHandler;
    
    public WerewolfCommandTree(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.commandHandler = new WerewolfCommand(plugin);
    }
    
    /**
     * Builds the complete command tree for /werewolf command.
     * 
     * @return The root command node
     */
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("werewolf");
        
        // Base command (no arguments) - shows version/help
        root.executes(context -> {
            CommandSender sender = context.getSource().getSender();
            sender.sendMessage("§6Werewolf Plugin v" + plugin.getPluginMeta().getVersion());
            sender.sendMessage("§7Use /werewolf help for command help.");
            return Command.SINGLE_SUCCESS;
        });
        
        // Help subcommand
        root.then(LiteralArgumentBuilder.<CommandSourceStack>literal("help")
            .executes(context -> {
                return executeSubcommand(context, new String[]{"help"});
            })
        );
        
        // Transform subcommand
        LiteralArgumentBuilder<CommandSourceStack> transform = LiteralArgumentBuilder.<CommandSourceStack>literal("transform");
        transform.executes(context -> {
            // Self transform
            return executeSubcommand(context, new String[]{"transform"});
        });
        transform.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
            .suggests(CommandSuggestions::suggestOnlinePlayers)
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "player");
                return executeSubcommand(context, new String[]{"transform", playerName});
            })
        );
        root.then(transform);
        
        // Cure subcommand
        LiteralArgumentBuilder<CommandSourceStack> cure = LiteralArgumentBuilder.<CommandSourceStack>literal("cure");
        cure.executes(context -> {
            // Self cure
            return executeSubcommand(context, new String[]{"cure"});
        });
        cure.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
            .suggests(CommandSuggestions::suggestOnlinePlayers)
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "player");
                return executeSubcommand(context, new String[]{"cure", playerName});
            })
        );
        root.then(cure);
        
        // Status subcommand
        LiteralArgumentBuilder<CommandSourceStack> status = LiteralArgumentBuilder.<CommandSourceStack>literal("status");
        status.executes(context -> {
            // Self status
            return executeSubcommand(context, new String[]{"status"});
        });
        status.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
            .suggests(CommandSuggestions::suggestOnlinePlayers)
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "player");
                return executeSubcommand(context, new String[]{"status", playerName});
            })
        );
        root.then(status);
        
        return root;
    }
    
    /**
     * Executes a subcommand by delegating to the existing WerewolfCommand handler.
     * 
     * @param context The Brigadier command context
     * @param args The command arguments
     * @return Command result code
     */
    private int executeSubcommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSender sender = context.getSource().getSender();
        
        // Create a dummy Command object for compatibility
        org.bukkit.command.Command command = new org.bukkit.command.Command(
            "werewolf", "", "", java.util.Collections.emptyList()) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return false; // Not used
            }
        };
        
        // Execute using the existing command handler
        boolean result = commandHandler.onCommand(sender, command, "werewolf", args);
        return result ? Command.SINGLE_SUCCESS : 0;
    }
}

