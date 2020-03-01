package org.popcraft.lwctrust;

import com.griefcraft.lwc.LWC;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.lwctrust.locale.FileResourceLoader;
import org.popcraft.lwctrust.locale.UTF8Control;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public final class LWCTrust extends JavaPlugin {

    private ResourceBundle defaultBundle, localeBundle;
    private TrustCache trustCache, confirmCache;
    private Metrics metrics;

    @Override
    public void onEnable() {
        // Copy any missing configuration options
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        // Make the trusts directory if it doesn't already exist
        File trustDirectory = new File(this.getDataFolder() + File.separator + "trusts");
        if (!trustDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            trustDirectory.mkdir();
        }
        // Load locales
        String locale = this.getConfig().getString("locale", "en");
        File messageFile = new File(this.getDataFolder() + File.separator +
                "locale_" + locale + ".properties");
        InputStream localeResource = this.getResource("locale_" + locale + ".properties");
        // Default locale is English
        this.defaultBundle = ResourceBundle.getBundle("locale", Locale.ENGLISH, new UTF8Control());
        if (messageFile.exists()) {
            // Load a custom provided locale file from the plugin folder
            this.localeBundle = ResourceBundle.getBundle("locale", new Locale(locale),
                    new FileResourceLoader(this), new UTF8Control());
        } else if (localeResource != null) {
            // Load another valid locale that is included with the plugin
            this.localeBundle = ResourceBundle.getBundle("locale", new Locale(locale),
                    new UTF8Control());
        } else {
            // Fall back to the default locale
            this.localeBundle = this.defaultBundle;
        }
        // Set up caches used by the plugin
        int cacheSize = this.getConfig().getInt("cache-size", 1000);
        this.trustCache = new TrustCache(this, cacheSize);
        this.confirmCache = new TrustCache(this, cacheSize);
        // Hook into LWC
        try {
            LWC.getInstance().getModuleLoader().registerModule(this, new TrustModule(this));
        } catch (NoClassDefFoundError e) {
            this.getLogger().severe(getMessage("error.nolwc"));
            this.getLogger().severe(getMessage("url.lwc"));
            this.setEnabled(false);
        }
        // Enable bStats metrics
        int pluginId = 6614;
        this.metrics = new Metrics(this, pluginId);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can trust
        if (args.length < 1 || !(sender instanceof Player)) {
            sender.sendMessage(getMessage("trust.description"));
            return false;
        }
        boolean confirm = this.getConfig().getBoolean("confirm-action", true);
        Player player = (Player) sender;
        UUID playerUniqueId = player.getUniqueId();
        if ("add".equalsIgnoreCase(args[0])) {
            // Get a list of existing unique players to add from the arguments
            List<UUID> toTrust = new ArrayList<>();
            Arrays.stream(args).forEach(a -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(a);
                if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()
                        && !toTrust.contains(offlinePlayer.getUniqueId())) {
                    toTrust.add(offlinePlayer.getUniqueId());
                }
            });
            if (confirm) {
                // If we need to confirm, just add these to the confirmation cache
                confirmCache.put(playerUniqueId, toTrust);
                player.sendMessage(getMessage("trust.add.confirm"));
            } else {
                // Otherwise we are just going to directly save any new players to the player's trusts
                List<UUID> trusted = trustCache.load(playerUniqueId);
                toTrust.forEach(uuid -> {
                    if (!trusted.contains(uuid)) {
                        trusted.add(uuid);
                        player.sendMessage(getMessage("trust.add", Bukkit.getOfflinePlayer(uuid).getName()));
                    }
                });
                trustCache.save(playerUniqueId);
            }
        } else if ("remove".equalsIgnoreCase(args[0])) {
            // Load a player's trusts, remove any players matching the arguments, and save
            List<UUID> trusted = trustCache.load(playerUniqueId);
            Arrays.stream(args).forEach(a -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(a);
                if (trusted.remove(offlinePlayer.getUniqueId())) {
                    player.sendMessage(getMessage("trust.remove", offlinePlayer.getName()));
                }
            });
            trustCache.save(playerUniqueId);
        } else if ("list".equalsIgnoreCase(args[0])) {
            // Load a player's trusts and send them a list
            List<UUID> trusted = trustCache.load(playerUniqueId);
            if (trusted.isEmpty()) {
                player.sendMessage(getMessage("trust.list.empty"));
            } else {
                List<String> playerNames = trusted.stream()
                        .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList());
                player.sendMessage(getMessage("trust.list", StringUtils.join(playerNames, ", ")));
            }
        } else if ("confirm".equalsIgnoreCase(args[0])) {
            // Add any trusts from pending confirmations, if any
            if (confirmCache.containsKey(playerUniqueId)) {
                List<UUID> trusted = trustCache.load(playerUniqueId);
                confirmCache.get(playerUniqueId).forEach(uuid -> {
                    if (!trusted.contains(uuid)) {
                        trusted.add(uuid);
                        player.sendMessage(getMessage("trust.add", Bukkit.getOfflinePlayer(uuid).getName()));
                    }
                });
                confirmCache.remove(playerUniqueId);
                trustCache.save(playerUniqueId);
            } else {
                player.sendMessage(getMessage("trust.confirm.empty"));
            }
        } else if ("cancel".equalsIgnoreCase(args[0])) {
            // Cancel pending trust confirmations, if any
            if (confirmCache.containsKey(playerUniqueId)) {
                confirmCache.remove(playerUniqueId);
                player.sendMessage(getMessage("trust.cancel"));
            } else {
                player.sendMessage(getMessage("trust.confirm.empty"));
            }
        } else {
            sender.sendMessage(getMessage("trust.description"));
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("add", "remove", "list"));
            if (confirmCache.containsKey(player.getUniqueId())) {
                completions.addAll(Arrays.asList("confirm", "cancel"));
            }
            return completions.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        } else if (args.length > 1 && Arrays.asList("add", "remove").contains(args[0])) {
            return null;
        } else {
            return Collections.emptyList();
        }
    }

    public String getMessage(String key, Object... args) {
        String localMessage = localeBundle.containsKey(key)
                ? localeBundle.getString(key) : defaultBundle.getString(key);
        String formattedMessage = String.format(localMessage, args);
        return ChatColor.translateAlternateColorCodes('&', formattedMessage);
    }

    public TrustCache getTrustCache() {
        return trustCache;
    }

}
