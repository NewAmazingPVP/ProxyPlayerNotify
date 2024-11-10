package ppn.bungeecord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

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
        if (!Files.exists(Paths.get(getDataFolder() + "/IMPORTANT.txt"))) {
            try {
                Files.createFile(Path.of(getDataFolder() + "/IMPORTANT.txt"));
                Path filePath = Paths.get(getDataFolder() + "/IMPORTANT.txt");
                String message = "Please move over all settings to new config.yml format instead of the old config.yml to make the plugin work! If you didn't save it beforehand, it might have been overwritten";
                Files.writeString(filePath, message, StandardOpenOption.WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveDefaultConfig();
        config.addDefault("join_message", "%player% has joined the network (Logged in server: %server%) at %time%",
                "Network Join Message\nThis message is displayed when a player joins the network.\nPlaceholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.");

        config.addDefault("join_private_message", "&aWelcome, %player%!\n&bYou have joined the server %server% at %time%.\nEnjoy your stay!",
                "Network Private Join Message\nThis message is displayed only to the player who joins the network.\nIt has a higher priority than the public join message.\nPlaceholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.");

        config.addDefault("switch_message", "%player% has switched from %last_server% and joined to the %server% server at %time%",
                "Servers Switch Message\nThis message is displayed when a player switches to a different server.\nPlaceholders available: %player%, %last_server%, %server%, %time%, %lp_prefix%, %lp_suffix%.");

        config.addDefault("leave_message", "%player% has left the network (Last server: %last_server%) at %time%",
                "Network Leave Message\nThis message is displayed when a player leaves the network.\nPlaceholders available: %player%, %lp_prefix%, %lp_suffix%, %last_server%, %time%.");

        config.addDefault("join_message_delay", 45,
                "Delay for Join Messages\nThis option sets the delay before sending the join message after a player connects.\nFor example, join_message_delay: 49 will send the message after 49 ticks.\nWarning: Setting this value too low may cause messages not to be sent or be blank placeholder if the server name is not yet available.");

        config.addDefault("join_private_message_delay", 50,
                "Delay for Private Join Messages\nThis option sets the delay before sending the private join message to the joining player.\nFor example, join_private_message_delay: 50 will send the message after 50 ticks.\nWarning: Setting this value too low may cause messages not to be sent or be blank placeholder if the server name is not yet available.");

        config.addDefault("disable_vanish_notifications", false,
                "Disable messages for vanished players (Currently supports PremiumVanish and SuperVanish)");

        config.addDefault("join_last_server", false,
                "Option to let players rejoin the server they were on before they left the network.\nIf this is enabled, the player will be sent to the last server on join in which they were on before they left the network.\nIf enabled, the message delay options would need to be increased so that the messages can get the server");

        config.addDefault("permission.permissions", false,
                "Enable this if you want to use permissions and want to use next two options.");

        config.addDefault("permission.notify_message", false,
                "Notify Messages\nIf this is true and the player doesn't have ppn.notify permission, then their join/leave/message will not be sent.");

        config.addDefault("permission.hide_message", false,
                "Hide Messages\nIf this is true and the player doesn't have ppn.view permission, then they won't see the others' join/switch/leave messages.");

        Map<String, String> defaultServerNames = new HashMap<>();
        defaultServerNames.put("example", "example-1");
        defaultServerNames.put("lobby", "Hub");
        config.addDefault("ServerNames", defaultServerNames,
                "Server Names\nDefine custom server names here. Players can join/leave/switch to the server using the custom names specified below.");

        List<String> defaultDisabledServers = new ArrayList<>();
        defaultDisabledServers.add("example-1");
        defaultDisabledServers.add("other-backend-server");
        config.addDefault("DisabledServers", defaultDisabledServers,
                "Disabled Servers\nDefine the backend servers (lowercase) where the join/switch/leave messages should not be sent.\nIn simple words, no messages of this plugin will be sent to players on that server\nIn short: No activity notifications are sent to players on these servers.");

        List<String> defaultPrivateServers = new ArrayList<>();
        defaultPrivateServers.add("example");
        defaultPrivateServers.add("private-server");
        config.addDefault("PrivateServers", defaultPrivateServers,
                "Private Servers\nSpecify the private servers (lowercase) where if player joins, leaves, and switches from and to, notifications should not be sent.\nThink about them like admin servers\nWhen someone joins it, the whole proxy should not be notified about that because you kind of want to keep that server private/secret and not let the players know.\nIn short: Activity notifications related to these servers are not broadcasted across the entire network.");

        List<String> defaultLimboServers = new ArrayList<>();
        defaultLimboServers.add("limbo-afk");
        config.addDefault("LimboServers", defaultLimboServers,
                "Limbo Servers\nSpecify the limbo servers (lowercase) where player join, leave, and switch notifications should be managed differently.\nThese servers act as pass-throughs most of the time and can be configured to adjust notification behavior accordingly.\nWhen a player joins a limbo server, no network-wide join notification is sent.\nWhen a player switches from a limbo server to a game server, it should send a join notification as if the player is joining the network for the first time.\nConversely, when a player switches from a game server to a limbo server, it should send a leave notification as if the player is leaving the network.\nThis configuration helps avoid unnecessary notifications and prevents stealthy movements between public and private parts of the network.\nIn short: Join and leave notifications are sent based on transitions to and from these servers to manage network-wide notifications effectively.");

        List<String> defaultDisabledPlayers = new ArrayList<>();
        defaultDisabledPlayers.add("player1");
        defaultDisabledPlayers.add("player2");
        config.addDefault("DisabledPlayers", defaultDisabledPlayers,
                "Disabled Players\nSpecify the players (lowercase) that should not send any notification messages.\nThey will also will not recieve join_private_message.\nIt is not recommended to use this feature and instead use permissions for each group/player");

        config.saveConfig();
        loadConfig();

        serverNames = new HashMap<>();
        config.getKeys("ServerNames").forEach(server -> serverNames.put(server.toLowerCase(), config.getString("ServerNames." + server)));

        disabledServers = new HashSet<>(config.getStringList("DisabledServers"));
        privateServers = new HashSet<>(config.getStringList("PrivateServers"));
        limboServers = new HashSet<>(config.getStringList("LimboServers"));
        disabledPlayers = new HashSet<>(config.getStringList("DisabledPlayers"));

        noVanishNotifications = config.getBoolean("disable_vanish_notifications");

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
