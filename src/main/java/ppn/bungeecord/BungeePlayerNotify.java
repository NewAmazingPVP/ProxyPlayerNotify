package ppn.bungeecord;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.plugin.Plugin;
import ppn.ConfigManager;
import ppn.bungeecord.commands.Reload;
import ppn.bungeecord.commands.ToggleMessages;
import ppn.bungeecord.utils.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BungeePlayerNotify extends Plugin {

    private ConfigManager config;
    private LuckPerms luckPerms;
    private Map<String, String> serverNames;
    private Set<String> disabledServers;
    private Set<String> privateServers;
    private Set<String> limboServers;
    private Set<String> disabledPlayers;
    private boolean noVanishNotifications;

    @Override
    public void onEnable() {
        new Metrics(this, 18703);
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        loadConfig();

        serverNames = new HashMap<>();
        config.getKeys("ServerNames").forEach(server -> serverNames.put(server.toLowerCase(), config.getString("ServerNames." + server)));

        disabledServers = new HashSet<>(config.getStringList("DisabledServers"));
        privateServers = new HashSet<>(config.getStringList("PrivateServers"));
        limboServers = new HashSet<>(config.getStringList("LimboServers"));
        disabledPlayers = new HashSet<>(config.getStringList("DisabledPlayers"));

        noVanishNotifications = config.getBoolean("disable_vanish_notifications");
        config.addDefault("join_last_server", false);
        config.saveConfig();

        getProxy().getPluginManager().registerListener(this, new EventListener(this));
        if (config.getString("join_message").contains("%lp_prefix%") || config.getString("switch_message").contains("%lp_prefix%") || config.getString("leave_message").contains("%lp_prefix%")
                || config.getString("join_message").contains("%lp_suffix%") || config.getString("switch_message").contains("%lp_suffix%") || config.getString("leave_message").contains("%lp_suffix%")) {
            luckPerms = LuckPermsProvider.get();
        }

        getProxy().getPluginManager().registerCommand(this, new Reload(this));
        getProxy().getPluginManager().registerCommand(this, new ToggleMessages(this));
    }

    public void saveDefaultConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        config = new ConfigManager(getDataFolder(), "config.yml");
    }

    public ConfigManager getConfig() {
        return config;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public Map<String, String> getServerNames() {
        return serverNames;
    }

    public Set<String> getDisabledServers() {
        return disabledServers;
    }

    public Set<String> getPrivateServers() {
        return privateServers;
    }

    public void setDisabledServers(Set<String> disabledServers) {
        this.disabledServers = disabledServers;
    }

    public void setPrivateServers(Set<String> privateServers) {
        this.privateServers = privateServers;
    }

    public Set<String> getLimboServers() {
        return limboServers;
    }

    public Set<String> getDisabledPlayers() {
        return disabledPlayers;
    }

    public boolean isNoVanishNotifications() {
        return noVanishNotifications;
    }

    public void setLimboServers(Set<String> limboServers) {
        this.limboServers = limboServers;
    }

    public void setNoVanishNotifications(boolean noVanishNotifications) {
        this.noVanishNotifications = noVanishNotifications;
    }
}
