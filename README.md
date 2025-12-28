# Werewolf Plugin

A modern Minecraft plugin that adds werewolf mechanics to your server, allowing players to transform into werewolves with custom skins and abilities.

## Features

- **Werewolf Transformation**: Players can transform into werewolves with custom skin changes
- **Resource Pack System**: Automatic resource pack serving via embedded HTTP server
- **Skin Management**: Dynamic skin changes using Paper API
- **Database Support**: SQLite, MySQL, and PostgreSQL with Hibernate ORM
- **Vampire Integration**: Cross-plugin awareness with the Vampire plugin
- **Modern Architecture**: Built on Paper API with experimental plugin system

## Quick Start

1. **Download** the plugin JAR file
2. **Place** it in your server's `plugins/` directory
3. **Start** your server - the plugin will create default configuration
4. **Configure** the resource pack server in `plugins/Werewolf/config.yml` (optional - enabled by default)
5. **Use** `/werewolf transform <player>` to transform a player into a werewolf

## Skin System

**Important**: Resource packs cannot reskin players in Minecraft. The Werewolf plugin uses texture values from `skins.yml` (obtained from MineSkin.org) to change player skins.

### Quick Setup

1. **Edit skin PNG files** in `plugins/Werewolf/skins_source/` (create this directory if needed)
2. **Upload PNGs to MineSkin.org** (https://mineskin.org/)
3. **Copy texture values** to `plugins/Werewolf/skins.yml`
4. **Reload the plugin** with `/werewolf reload`

### Skin File Structure

```
plugins/Werewolf/
├── skins.yml          ← Texture values (from MineSkin.org)
└── skins_source/      ← PNG files (for editing)
    ├── alpha.png
    ├── witherfang.png
    └── ...
```

For detailed information about configuring skins, see [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md#skin-configuration).

## Resource Pack (Optional)

The Werewolf plugin includes an embedded HTTP server for serving resource packs (useful for custom sounds, models, etc.). This is **not required** for skin changes.

### Configuration

The resource pack server can be configured in `config.yml`:

```yaml
resource-pack:
  server:
    enabled: true  # Enable embedded HTTP server
    port: 8080     # Port for HTTP server
    host: "localhost"  # Host (usually "localhost")
```

For detailed information about the resource pack system, see [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md).

## Commands

- `/werewolf transform <player>` - Transform a player into a werewolf
- `/werewolf cure <player>` - Cure a werewolf
- `/werewolf status [player]` - Check werewolf status

## Permissions

- `werewolf.use` - Use werewolf commands
- `werewolf.transform` - Transform players
- `werewolf.cure` - Cure werewolves
- `werewolf.admin` - Administrative commands

## Configuration

The plugin uses `config.yml` for all settings. Key configuration sections:

- **Database**: Configure database type and connection
- **Resource Pack**: Configure embedded HTTP server and resource pack settings
- **Werewolf Settings**: Enable/disable transformations and skin changes
- **Vampire Integration**: Configure cross-plugin integration

For complete configuration documentation, see [CONFIG.md](CONFIG.md).

## Database

The plugin supports multiple database types:

- **SQLite** (default) - No setup required, perfect for small servers
- **MySQL** - For larger servers or shared databases
- **PostgreSQL** - Enterprise-grade database support

Database migrations are handled automatically using Flyway.

## Vampire Integration

The Werewolf plugin is aware of the Vampire plugin and can prevent conflicts:

- Players cannot be both a vampire and a werewolf (configurable)
- Cross-plugin event handling
- Mutual exclusivity enforcement

## Development

### Building

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

### Project Structure

```
Werewolf/
├── src/main/
│   ├── java/org/clockworx/werewolf/
│   │   ├── server/          # Resource pack HTTP server
│   │   ├── manager/         # Core logic managers
│   │   ├── config/          # Configuration handling
│   │   ├── database/        # Database management
│   │   ├── integration/     # Cross-plugin integration
│   │   └── commands/        # Command handlers
│   └── resources/
│       ├── resourcepack/    # Resource pack assets
│       └── config.yml       # Default configuration
└── docs/                     # Documentation
```

## Requirements

- **Minecraft**: 1.21.11+
- **Server**: Paper or compatible fork
- **Java**: 21+

## Documentation

- [README.md](README.md) - This file
- [CONFIG.md](CONFIG.md) - Complete configuration guide
- [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md) - Resource pack and web server documentation

## License

[Add your license information here]

## Credits

[Add credits/attributions here]

## Support

For issues, questions, or contributions, please [add your support channels here].

