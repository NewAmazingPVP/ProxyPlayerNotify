# ProxyPlayerNotify

**ProxyPlayerNotify** is a plugin that notifies players when someone joins, leaves, or switches servers on your network. It is compatible with both Bungeecord and Velocity for Minecraft versions 1.8 and above.

### Features
- Join, leave, and switch notifications.
- Customizable messages with placeholders.
- Permission-based message visibility.
- Ability to disable notifications on specific servers.
- Option to keep certain servers private from notifications.

### Installation
1. Download the latest release from the [Spigot Plugin Page](https://www.spigotmc.org/resources/bungeeplayernotify.108035/).
2. Place the plugin `.jar` file into your server’s `plugins` directory.
3. Restart your server to generate the `config.yml` file.
4. Customize the `config.yml` file to suit your needs.

### Configuration
Here’s a sample `config.yml`:

```yaml
# ProxyPlayerNotify Config

# This config file contains settings for the BungeePlayer Notify plugin.
# Use this file to customize the join/leave messages and permissions.

# Network Join Message
# This message is displayed when a player joins the network.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.
join_message: "%player% has joined the network (Logged in server: %server%) at %time%"

# Servers Switch Message
# This message is displayed when a player switches to a different server.
# Placeholders available: %player%, %last_server%, %server%, %time%, %lp_prefix%, %lp_suffix%.
switch_message: "%player% has switched from %last_server% and joined to the %server% server at %time%"

# Network Leave Message
# This message is displayed when a player leaves the network.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %last_server%, %time%.
leave_message: "%player% has left the network (Last server: %last_server%) at %time%"

# Disable messages for vanished players (Currently supports PremiumVanish and SuperVanish)
disable_vanish_notifications: true

# Permissions
# Use these settings to control who can see the join/leave messages.
permission:
  # Enable this if you want to use permissions and want to use next two options.
  permissions: false

  # Notify Messages
  # If this is true and the player doesn't have ppn.notify permission, then their join/leave/message will not be sent.
  notify_message: false

  # Hide Messages
  # If this is true and the player doesn't have ppn.view permission, then they won't see the others' join/switch/leave messages.
  hide_message: false

# Server Names
# Define custom server names here. Players can join/leave/switch to the server using the custom names specified below.
ServerNames:
  example: "example-1"
  lobby: "Hub"

# Disabled Servers
# Define the backend servers (lowercase) where the join/switch/leave messages should not be sent.
# In simple words, no messages of this plugin will be sent to players on that server
# In short: No activity notifications are sent to players on these servers.
DisabledServers:
  - "example-1"
  - "other-backend-server"

# Private Servers
# Specify the private servers (lowercase) where if player joins, leaves, and switches from and to, notifications should not be sent.
# Think about them like admin servers
# When someone joins it, the whole proxy should not be notified about that because you kind of want to keep that server private/secret and not let the players know.
# In short: Activity notifications related to these servers are not broadcasted across the entire network.
PrivateServers:
  - "example"
  - "private-server"
```

### Contributing
Please feel free to submit issues and pull requests on [GitHub](https://github.com/your-repo).

### Support
Join our community on [Discord](https://discord.gg/tuVvmawsRX) for help and discussions.

### License
This project is licensed under the MIT License.
