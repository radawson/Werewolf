package org.clockworx.werewolf.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Command class for displaying general information about the Werewolf plugin.
 * Shows details like version, authors, website, debug status, and Werewolf-specific features.
 */
public class CmdWerewolfInfo {
    
    private final WerewolfPlugin plugin;
    
    /**
     * Creates a new info command.
     *
     * @param plugin The plugin instance.
     */
    public CmdWerewolfInfo(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Executes the /werewolf info command.
     * Sends informational messages to the command sender.
     *
     * @param sender The command sender.
     * @param command The command being executed.
     * @param label The alias used for the command.
     * @param args The command arguments (ignored for this command).
     * @return True, as the command is always handled.
     */
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        // Send header
        sender.sendMessage("§7=== §6Werewolf Info §7===");
        
        // Plugin metadata
        sender.sendMessage("§7Version: §f" + plugin.getPluginMeta().getVersion());
        
        // Authors
        String authors = String.join(", ", plugin.getPluginMeta().getAuthors());
        if (!authors.isEmpty()) {
            sender.sendMessage("§7Authors: §f" + authors);
        }
        
        // Website
        String website = plugin.getPluginMeta().getWebsite();
        if (website != null && !website.isEmpty()) {
            sender.sendMessage("§7Website: §f" + website);
        }
        
        // Debug status
        boolean isDebug = plugin.getWerewolfConfig().isDebug();
        sender.sendMessage("§7Debug Mode: " + (isDebug ? "§aOn" : "§cOff"));
        
        // Resource pack server status
        org.clockworx.werewolf.server.ResourcePackServer rpServer = plugin.getResourcePackServer();
        if (rpServer != null) {
            boolean serverEnabled = plugin.getWerewolfConfig().getConfig().getBoolean("resource-pack.server.enabled", false);
            if (serverEnabled && rpServer.isStarted()) {
                int port = rpServer.getListeningPort();
                sender.sendMessage("§7Resource Pack Server: §aRunning §7(Port: §f" + port + "§7)");
            } else if (serverEnabled) {
                sender.sendMessage("§7Resource Pack Server: §eEnabled but not started");
            } else {
                sender.sendMessage("§7Resource Pack Server: §cDisabled");
            }
        } else {
            sender.sendMessage("§7Resource Pack Server: §cNot Available");
        }
        
        // Skin manager status
        org.clockworx.werewolf.manager.SkinManager skinManager = plugin.getSkinManager();
        if (skinManager != null) {
            int skinCount = skinManager.getLoadedSkinCount();
            sender.sendMessage("§7Skin Manager: §aLoaded §7(§f" + skinCount + " skin" + (skinCount != 1 ? "s" : "") + "§7)");
        } else {
            sender.sendMessage("§7Skin Manager: §cNot Available");
        }
        
        // Vampire integration status
        if (plugin.getVampireIntegration() != null) {
            boolean integrationEnabled = plugin.getWerewolfConfig().getConfig().getBoolean("vampire-integration.enabled", false);
            if (integrationEnabled) {
                sender.sendMessage("§7Vampire Integration: §aEnabled");
            } else {
                sender.sendMessage("§7Vampire Integration: §cDisabled");
            }
        } else {
            sender.sendMessage("§7Vampire Integration: §7Not Available");
        }
        
        // Database type
        String dbType = plugin.getWerewolfConfig().getDatabaseType();
        sender.sendMessage("§7Database: §f" + dbType.toUpperCase());
        
        return true;
    }
    
    /**
     * Provides tab completions for the /werewolf info command.
     * This command has no arguments, so it returns an empty list.
     *
     * @param sender The command sender.
     * @param command The command being executed.
     * @param label The alias used for the command.
     * @param args The command arguments.
     * @return An empty list, as there are no sub-arguments.
     */
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}

