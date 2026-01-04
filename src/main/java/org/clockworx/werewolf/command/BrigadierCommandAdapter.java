package org.clockworx.werewolf.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

/**
 * Adapter to bridge existing command executors to Brigadier command system.
 * This allows us to reuse existing command logic while using Paper's Brigadier API.
 */
public class BrigadierCommandAdapter {
    
    /**
     * Creates a Brigadier Command executor that wraps a Bukkit CommandExecutor.
     * 
     * @param executor The Bukkit CommandExecutor to wrap
     * @param commandName The name of the command
     * @return A Brigadier Command executor
     */
    public static Command<CommandSourceStack> wrapExecutor(
            org.bukkit.command.CommandExecutor executor,
            String commandName) {
        return (CommandContext<CommandSourceStack> context) -> {
            CommandSourceStack source = context.getSource();
            CommandSender sender = source.getSender();
            
            // Build arguments array from context
            String[] args = buildArgs(context, commandName);
            
            // Create a dummy Command object for compatibility
            org.bukkit.command.Command command = new org.bukkit.command.Command(
                commandName, "", "", java.util.Collections.emptyList()) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return false; // Not used
                }
            };
            
            // Execute the wrapped command
            boolean result = executor.onCommand(sender, command, commandName, args);
            return result ? Command.SINGLE_SUCCESS : 0;
        };
    }
    
    /**
     * Builds a String array of arguments from the Brigadier context.
     * Extracts all arguments after the command name.
     * 
     * @param context The Brigadier command context
     * @param commandName The base command name
     * @return Array of argument strings
     */
    private static String[] buildArgs(CommandContext<CommandSourceStack> context, String commandName) {
        // Get the input string and parse arguments
        String input = context.getInput();
        if (input == null || input.isEmpty()) {
            return new String[0];
        }
        
        // Remove the command name and split by spaces
        String remaining = input.substring(commandName.length()).trim();
        if (remaining.isEmpty()) {
            return new String[0];
        }
        
        // Split by spaces, handling quoted strings if needed
        return remaining.split("\\s+");
    }
    
    /**
     * Creates a suggestion provider that wraps a TabCompleter.
     * 
     * @param completer The TabCompleter to wrap
     * @param commandName The name of the command
     * @return A suggestion provider function
     */
    public static com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> wrapCompleter(
            org.bukkit.command.TabCompleter completer,
            String commandName) {
        return (context, builder) -> {
            CommandSourceStack source = context.getSource();
            CommandSender sender = source.getSender();
            
            // Build arguments array
            String[] args = buildArgs(context, commandName);
            
            // Create a dummy Command object
            org.bukkit.command.Command command = new org.bukkit.command.Command(
                commandName, "", "", java.util.Collections.emptyList()) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return false;
                }
            };
            
            // Get completions from the TabCompleter
            java.util.List<String> completions = completer.onTabComplete(sender, command, commandName, args);
            if (completions == null) {
                completions = java.util.Collections.emptyList();
            }
            
            // Add suggestions to the builder
            String remaining = builder.getRemaining().toLowerCase();
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(remaining)) {
                    builder.suggest(completion);
                }
            }
            
            return builder.buildFuture();
        };
    }
}

