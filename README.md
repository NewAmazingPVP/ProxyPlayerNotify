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

# Network Join Message
# Placeholders available: %player%, %lp_prefix%, %server%, %time%.
join_message: "%player% has joined the network (Logged in server: %server%) at %time%"

# Server Switch Message
# Placeholders available: %player%, %last_server%, %server%, %time%, %lp_prefix%.
switch_message: "%player% has switched from %last_server% and joined to the %server% server at %time%"

# Network Leave Message
# Placeholders available: %player%, %lp_prefix%, %last_server%, %time%.
leave_message: "%player% has left the network (Last server: %last_server%) at %time%"

# Permissions
permission:
  permissions: false
  notify_message: false
  hide_message: false

# Server Names
ServerNames:
  example: "example-1"
  lobby: "Hub"

# Disabled Servers
DisabledServers:
  - "example-1"
  - "other-backend-server"

# Private Servers
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
