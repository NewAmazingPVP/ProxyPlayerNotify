# ProxyPlayerNotify

**ProxyPlayerNotify** is a plugin that notifies players when someone joins, leaves, or switches servers on your network. It is compatible with both Bungeecord and Velocity.

### Features
- Join, leave, and switch notifications.
- Customizable messages with placeholders.
- Permission-based message visibility.
- Ability to disable notifications on specific servers.
- Option to keep certain servers private from notifications.
- Unique first join messages for new players.
- - Discord webhook notifications on player join with vanish support.

### Installation
1. Download the latest release from the [Spigot Plugin Page](https://www.spigotmc.org/resources/bungeeplayernotify.108035/).
2. Place the plugin `.jar` file into your server’s `plugins` directory.
3. Restart your server to generate the `config.yml` file.
4. Customize the `config.yml` file to suit your needs.

### Configuration
Here’s a sample `config.yml`:

```yaml
# ProxyPlayerNotify Config

# This config file contains settings for the ProxyPlayerNotify plugin.
# Use this file to customize the join/leave messages and permissions.
# Use \n to create multiple lines/messages to players.
# Set message to "" to not send any message/empty.

# Network Join Message
# This message is displayed when a player joins the network.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.
join_message: "%player% has joined the network (Logged in server: %server%) at %time%"

# Network Private Join Message
# This message is displayed only to the player who joins the network.
# It has a higher priority than the public join message.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.
join_private_message:
  - "&aWelcome, %player%!"
  - "&bYou have joined the server %server% at %time%."
  - "Enjoy your stay!"

# First Join Message
# This message is displayed when a player joins the network for the first time.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.
first_join_message: "%player% has joined the network for the first time on %server% at %time%"

# First Join Private Message
# This message is displayed only to the player joining for the first time.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %server%, %time%.
first_join_private_message: "&aWelcome for the first time, %player%!"

# Servers Switch Message
# This message is displayed when a player switches to a different server.
# Placeholders available: %player%, %last_server%, %server%, %time%, %lp_prefix%, %lp_suffix%.
switch_message: "%player% has switched from %last_server% and joined to the %server% server at %time%"

# Network Leave Message
# This message is displayed when a player leaves the network.
# Placeholders available: %player%, %lp_prefix%, %lp_suffix%, %last_server%, %time%.
leave_message: "%player% has left the network (Last server: %last_server%) at %time%"

# Delay for Join Messages
# This option sets the delay before sending the join message after a player connects.
# For example, join_message_delay: 49 will send the message after 49 ticks.
# Warning: Setting this value too low may cause messages not to be sent or be blank placeholder if the server name is not yet available.
join_message_delay: 49

# Delay for First Join Messages
first_join_message_delay: 10

# Delay for Private Join Messages
# This option sets the delay before sending the private join message to the joining player.
# For example, join_private_message_delay: 50 will send the message after 50 ticks.
# Warning: Setting this value too low may cause messages not to be sent or be blank placeholder if the server name is not yet available.
join_private_message_delay: 50

# Delay for First Join Private Messages
first_join_private_message_delay: 10

# Disable messages for vanished players (Currently supports PremiumVanish and SuperVanish)
disable_vanish_notifications: false

# Option to let players rejoin the server they were on before they left the network.
# If this is enabled, the player will be sent to the last server on join in which they were on before they left the network.
# If enabled, the message delay options would need to be increased so that the messages can get the server
join_last_server: false

# Enable MiniMessage format parsing for all messages
use_minimessage: false

# Permissions
# Use these settings to control who can see the join/leave messages.
# Enable permissions if you want to use permissions and want to use next two options.
# If permissions enabled Then if notify_message is true and the player doesn't have ppn.notify permission, then their join/leave/message will not be sent.
# If permissions enabled Then if hide_message is true and the player doesn't have ppn.view permission, then they won't see the others' join/switch/leave messages.
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

# Limbo Servers
# Specify the limbo servers (lowercase) where player join, leave, and switch notifications should be managed differently.
# These servers act as pass-throughs most of the time and can be configured to adjust notification behavior accordingly.
# When a player joins a limbo server, no network-wide join notification is sent.
# When a player switches from a limbo server to a game server, it should send a join notification as if the player is joining the network for the first time.
# Conversely, when a player switches from a game server to a limbo server, it should send a leave notification as if the player is leaving the network.
# This configuration helps avoid unnecessary notifications and prevents stealthy movements between public and private parts of the network.
# In short: Join and leave notifications are sent based on transitions to and from these servers to manage network-wide notifications effectively.
LimboServers:
  - "limbo-afk"

# Disabled Players
# Specify the players (lowercase) that should not send any notification messages.
# They will also will not recieve join_private_message.
# It is not recommended to use this feature and instead use permissions for each group/player
DisabledPlayers:
  - "player1"
  - "player2"

# Webhook
# Configure a webhook notification sent when a player joins the network.
# Placeholders available: %player%, %server%, %time%.
webhook:
  enabled: false
  url: ""
  message: "%player% joined %server% at %time%"
```

### Contributing
Please feel free to submit issues and pull requests on [GitHub](https://github.com/your-repo).

### Support
Join our community on [Discord](https://discord.gg/tuVvmawsRX) for help and discussions.

### License
This project is licensed under the MIT License.
