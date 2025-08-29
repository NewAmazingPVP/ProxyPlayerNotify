# ProxyPlayerNotify

Proxy-wide player activity notifications for BungeeCord and Velocity. Clean, configurable, and fast — now with
MiniMessage support, webhook integration, and CI-built jars on every push.

## Features

- Join, leave, and server switch broadcasts
- First-join public and private messages
- MiniMessage or legacy color support (`use_minimessage`)
- LuckPerms placeholders: `%lp_prefix%`, `%lp_suffix%`
- PAPIProxyBridge placeholders (optional)
- Private, disabled, and limbo server handling
- Per-player toggle command; permission-based visibility
- Discord-compatible webhook on network join
- GitHub Actions builds jars automatically on every push

## Supported Platforms

- BungeeCord/Waterfall (bungeecord-api 1.18+)
- Velocity 3.x

## Install

- Download the latest jar from GitHub Actions artifacts or Releases.
- Drop into your proxy `plugins/` folder.
- Restart to generate `config.yml`.
- Adjust settings, then `/reloadProxyNotifyConfig` (Bungee) or `reloadProxyNotifyConfig` (Velocity).

## Quick Config Reference

- `join_message`: Public message when a player joins
- `join_private_message`: List of lines sent to the joining player only
- `first_join_message` / `first_join_private_message`: First-time variants
- `switch_message` / `leave_message`: Server switch and leave
- `join_message_delay`, `switch_message_delay`, etc.: Millisecond-based delays (ticks × 50)
- `ServerNames`: Map backend names to pretty names
- `DisabledServers` / `PrivateServers` / `LimboServers`: Fine-grained routing
- `permission.permissions`: Enables permission checks
    - `permission.notify_message`: Require `ppn.notify` to broadcast your own join/leave
    - `permission.hide_message`: Require `ppn.view` to see network notifications
- `disable_vanish_notifications`: Suppress vanished player activity (PremiumVanish/SuperVanish)
- `join_last_server`: Reconnect players to their last server
- `use_minimessage`: Enable MiniMessage parsing for all outputs
- `webhook.enabled`, `webhook.url`, `webhook.message`: Send join messages to a webhook

Tip: For multi-line messages, use YAML lists. The plugin joins lists with newlines.

## Placeholders

- Built-in: `%player%`, `%server%`, `%last_server%`, `%time%`
- LuckPerms: `%lp_prefix%`, `%lp_suffix%`
- PAPIProxyBridge: All configured PAPI placeholders (if installed)

## Commands

- BungeeCord
    - `/reloadProxyNotifyConfig` — reloads config (perm: `ppn.reloadProxyNotifyConfig`)
    - `/togglemessages` — toggles receiving notifications per-player
- Velocity
    - `reloadProxyNotifyConfig` — reloads config (console or permissioned source)
    - `togglemessages` — toggles receiving notifications per-player

## CI & Releases

- Every push builds with GitHub Actions and uploads the jar as an artifact.
- Pushing a tag like `v2.4.0` also creates a GitHub Release with the jar attached.

## Example Config

See `src/main/resources/config.yml` for a fully-commented example you can copy.

## Support

- Spigot: https://www.spigotmc.org/resources/bungeeplayernotify.108035/
- Discord: https://discord.gg/tuVvmawsRX

## License

MIT
