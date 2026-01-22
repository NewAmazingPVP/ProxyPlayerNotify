<div align="center">

# **ProxyPlayerNotify v2.5.0**

*Network‑wide join/leave/switch notifications for proxy servers.*

![Platforms](https://img.shields.io/badge/Platforms-BungeeCord%20%7C%20Velocity-5A67D8)
![MC](https://img.shields.io/badge/Minecraft-1.8%E2%86%92Latest-2EA043)
![Java](https://img.shields.io/badge/Java-17+-1F6FEB)
![License](https://img.shields.io/badge/License-MIT-0E8A16)

</div>

> TL;DR
> Drop the jar on your proxy (BungeeCord/Velocity). It broadcasts join/leave/switch messages (with placeholders,
> MiniMessage or legacy colors), supports per‑player toggles, obeys vanish, and can ping a Discord‑style webhook.

---

## Table of Contents

* [Highlights](#highlights)
* [Platforms & Requirements](#platforms--requirements)
* [Installation](#installation)
* [Quick Start](#quick-start)
* [Configuration](#configuration)
    * [Messages & Delays](#messages--delays)
    * [Servers & Routing](#servers--routing)
    * [Placeholders](#placeholders)
    * [Permissions](#permissions)
    * [Webhook](#webhook)
* [Commands](#commands)
* [Troubleshooting](#troubleshooting)
* [Building from Source](#building-from-source)
* [Contributing & Support](#contributing--support)
* [License](#license)

---

## Highlights

- Join, leave, and server‑switch broadcasts
- First‑join public/private messages
- MiniMessage or legacy color codes (`use_minimessage`)
- LuckPerms placeholders: `%lp_prefix%`, `%lp_suffix%` (optional)
- PAPIProxyBridge placeholders (optional)
- Private/Disabled/Limbo servers for smart routing
- Per‑player toggle command; permission‑gated visibility
- Webhook on join (Discord‑compatible)

> Note: This is a proxy plugin. It runs on BungeeCord/Velocity, not on Spigot/Paper/Folia.

---

## Platforms & Requirements

- Platforms: BungeeCord/Waterfall, Velocity 3.x
- Java: 17+
- Optional integrations (soft‑depends): LuckPerms, PAPIProxyBridge, PremiumVanish/SuperVanish

---

## Installation

1. Download the latest jar from Releases (or Actions artifacts).
2. Place it in your proxy `plugins/` folder.
3. Start the proxy to generate `config.yml`.
4. Adjust config, then reload:
    - Bungee: `/reloadProxyNotifyConfig`
    - Velocity: `reloadProxyNotifyConfig`

---

## Quick Start

Key options in `config.yml`:

- Messages: `join_message`, `leave_message`, `switch_message`
- First‑join: `first_join_message`, `first_join_private_message`
- Private message to the joining player: `join_private_message` (YAML list → multi‑line)
- Delays (ticks): `join_message_delay`, `switch_message_delay`, etc.
- Formatting: `use_minimessage: true|false`
- Routing: `ServerNames`, `DisabledServers`, `PrivateServers`, `LimboServers`
- Vanish: `disable_vanish_notifications: true|false`
- Permissions: `permission.permissions`, `permission.notify_message`, `permission.hide_message`
- Webhook: `webhook.enabled`, `webhook.url`, `webhook.message`

Tip: For multi‑line messages, use YAML lists - the plugin joins them with newlines.

---

## Configuration

### Messages & Delays

- Public: `join_message`, `leave_message`, `switch_message`
- Private (to the joining player): `join_private_message` (list)
- First‑join variants: `first_join_*`
- Delays are in ticks (`x 50ms`): `join_message_delay`, `switch_message_delay`, etc.

### Servers & Routing

- `ServerNames`: map backend → display name
- `DisabledServers`: players on these servers won’t receive broadcasts
- `PrivateServers`: activity from/to these servers isn’t broadcast network‑wide
- `LimboServers`: moving into a limbo server acts like leaving; moving out acts like joining

### Placeholders

- Built‑in: `%player%`, `%server%`, `%last_server%`, `%time%`
- LuckPerms (optional): `%lp_prefix%`, `%lp_suffix%`
- PAPIProxyBridge (optional): all PAPI placeholders available on your proxy

### Permissions

- Enable checks: `permission.permissions: true`
- Hide unless viewer has `ppn.view`: `permission.hide_message: true`
- Only broadcast for players with `ppn.notify`: `permission.notify_message: true`

### Webhook

- `webhook.enabled: true|false`
- `webhook.url: "https://..."`
- `webhook.message`: supports placeholders and formatting

---

## Commands

- BungeeCord
    - `/reloadProxyNotifyConfig` - reloads config (perm: `ppn.reloadProxyNotifyConfig`)
    - `/togglemessages` - per‑player toggle
- Velocity
    - `reloadProxyNotifyConfig` - reloads config (console or permissioned source)
    - `togglemessages` - per‑player toggle

---

## Troubleshooting

- No placeholders? Ensure LuckPerms/PAPIProxyBridge is installed on the proxy (optional). The plugin runs fine without
  them.
- Vanish not detected? PremiumVanish/SuperVanish is optional; if absent, notifications aren’t blocked.
- Messages not visible? Check `permission.*` settings and viewer permissions.
- Switch/first‑join timing odd? Increase delays to ensure server names are available.

---

## Building from Source

```bash
mvn -DskipTests package
```

## Contributing & Support

- Spigot: https://www.spigotmc.org/resources/bungeeplayernotify.108035/
- Discord: https://discord.gg/tuVvmawsRX
- Issues/PRs welcome.

---

## License

MIT
