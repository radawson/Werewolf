package org.clockworx.werewolf.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;

/**
 * Command handler for the /werewolf command.
 * Provides basic werewolf transformation functionality.
 */
public class WerewolfCommand implements CommandExecutor, TabCompleter {
    
    private final WerewolfPlugin plugin;
    private final org.clockworx.werewolf.commands.CmdWerewolfInfo infoCommand;
    
    /**
     * Creates a new WerewolfCommand.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public WerewolfCommand(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.infoCommand = new org.clockworx.werewolf.commands.CmdWerewolfInfo(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6Werewolf Plugin v" + plugin.getDescription().getVersion());
            sender.sendMessage("§7Use /werewolf help for command help.");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "transform":
                return handleTransform(sender, args);
            case "cure":
                return handleCure(sender, args);
            case "status":
                return handleStatus(sender, args);
            case "info":
                return infoCommand.execute(sender, command, label, args);
            case "help":
                return handleHelp(sender);
            default:
                sender.sendMessage("§cUnknown command. Use /werewolf help for help.");
                return true;
        }
    }
    
    /**
     * Handles the transform subcommand.
     *
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled successfully.
     */
    private boolean handleTransform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage("§cYou must specify a player when using this command from console.");
            return true;
        }
        
        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }
        } else {
            target = (Player) sender;
        }
        
        // Check for vampire conflict
        if (plugin.getVampireIntegration() != null && 
            plugin.getVampireIntegration().isVampire(target)) {
            sender.sendMessage("§cCannot transform " + target.getName() + ": player is a vampire.");
            return true;
        }
        
        // Transform is a FORM toggle: human <-> wolf. Use /werewolf cure to clear werewolf status.
        Boolean nowWolf = plugin.getWerewolfManager().toggleForm(target.getUniqueId());

        if (nowWolf == null) {
            sender.sendMessage("§cFailed to transform " + target.getName() + ".");
        } else if (nowWolf) {
            sender.sendMessage("§aTransformed " + target.getName() + " into werewolf form.");
            if (!target.equals(sender)) {
                target.sendMessage("§aYou transform into a werewolf!");
            }
        } else {
            sender.sendMessage("§a" + target.getName() + " reverted to human form.");
            if (!target.equals(sender)) {
                target.sendMessage("§aYou revert to human form.");
            }
        }

        return true;
    }
    
    /**
     * Handles the cure subcommand.
     *
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled successfully.
     */
    private boolean handleCure(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage("§cYou must specify a player when using this command from console.");
            return true;
        }
        
        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }
        } else {
            target = (Player) sender;
        }
        
        boolean success = plugin.getWerewolfManager().setWerewolfStatus(
            target.getUniqueId(), false, "Command: " + sender.getName());
        
        if (success) {
            sender.sendMessage("§aCured " + target.getName() + " of werewolf status.");
            if (!target.equals(sender)) {
                target.sendMessage("§aYou have been cured of werewolf status!");
            }
        } else {
            sender.sendMessage("§cFailed to cure " + target.getName() + " (player may not be a werewolf).");
        }
        
        return true;
    }
    
    /**
     * Handles the status subcommand.
     *
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled successfully.
     */
    private boolean handleStatus(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage("§cYou must specify a player when using this command from console.");
            return true;
        }
        
        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }
        } else {
            target = (Player) sender;
        }
        
        WerewolfPlayer wp = plugin.getWerewolfManager().getCachedWerewolfPlayer(target.getUniqueId());
        if (wp == null) {
            sender.sendMessage("§7" + target.getName() + " is not a werewolf.");
            return true;
        }
        
        sender.sendMessage("§6=== Werewolf Status: " + target.getName() + " ===");
        sender.sendMessage("§7Is Werewolf: " + (wp.isWerewolf() ? "§aYes" : "§cNo"));
        sender.sendMessage("§7Type: " + wp.getWerewolfType().name());
        sender.sendMessage("§7State: " + wp.getTransformationState().name());
        
        return true;
    }
    
    /**
     * Handles the help subcommand.
     *
     * @param sender The command sender.
     * @return True if the command was handled successfully.
     */
    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("§6=== Werewolf Commands ===");
        sender.sendMessage("§7/werewolf transform [player] - Transform into a werewolf");
        sender.sendMessage("§7/werewolf cure [player] - Cure werewolf status");
        sender.sendMessage("§7/werewolf status [player] - Check werewolf status");
        sender.sendMessage("§7/werewolf info - Show plugin information");
        sender.sendMessage("§7/werewolf help - Show this help");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("transform", "cure", "status", "info", "help");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("transform") || 
                                        args[0].equalsIgnoreCase("cure") || 
                                        args[0].equalsIgnoreCase("status"))) {
            List<String> players = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        return new ArrayList<>();
    }
}

