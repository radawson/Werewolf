# Werewolf Plugin Configuration Guide

This document provides detailed explanations of all configuration settings in the Werewolf plugin.

## Configuration File

The main configuration file is located at `plugins/Werewolf/config.yml`. It is automatically created on first run with default values.

## Configuration Sections

### Database Configuration

```yaml
database:
  type: sqlite  # sqlite, mysql, postgres
  url: ""  # Auto-generated for sqlite, required for mysql/postgres
  user: ""  # Required for mysql/postgres
  password: ""  # Required for mysql/postgres
  table-prefix: ""  # Optional prefix for all database tables
```

**Settings**:
- **`type`**: Database type to use
  - `sqlite` - SQLite (default, no setup required)
  - `mysql` - MySQL database
  - `postgres` - PostgreSQL database
- **`url`**: Database connection URL
  - SQLite: Auto-generated if empty
  - MySQL: `jdbc:mysql://host:port/database`
  - PostgreSQL: `jdbc:postgresql://host:port/database`
- **`user`**: Database username (required for MySQL/PostgreSQL)
- **`password`**: Database password (required for MySQL/PostgreSQL)
- **`table-prefix`**: Prefix for all database tables (useful for shared databases)

### General Settings

```yaml
debug: false
save-on-quit: true
auto-save:
  enabled: true
  interval: 300  # Seconds between auto-saves
```

**Settings**:
- **`debug`**: Enable debug logging (shows detailed plugin information)
- **`save-on-quit`**: Save player data when they disconnect
- **`auto-save.enabled`**: Enable automatic periodic saving
- **`auto-save.interval`**: Time in seconds between auto-saves (default: 300 = 5 minutes)

### Werewolf Settings

```yaml
werewolf:
  transformation-enabled: true
  skin-change-enabled: true
```

**Settings**:
- **`transformation-enabled`**: Allow players to transform into werewolves
- **`skin-change-enabled`**: Enable skin changes when transforming

### Resource Pack Configuration

```yaml
resource-pack:
  server:
    enabled: true  # Enable embedded HTTP server for resource pack
    port: 8080     # Port for HTTP server (default: 8080)
    host: "localhost"  # Host (usually "localhost" for local access)
  url: ""  # Auto-populated if server enabled, or manual URL for external hosting
  hash: ""  # Auto-calculated if server enabled, or manual SHA-1 hash
  required: false  # Whether players must accept resource pack
```

**Server Settings**:
- **`server.enabled`**: Enable the embedded HTTP server (recommended)
- **`server.port`**: Port for the HTTP server (default: 8080)
- **`server.host`**: Host address (usually "localhost" or server IP)

**Resource Pack Settings**:
- **`url`**: Resource pack URL
  - Auto-populated if embedded server is enabled
  - Manual URL for external hosting
- **`hash`**: SHA-1 hash of the resource pack
  - Auto-calculated if embedded server is enabled
  - Manual hash for external hosting
- **`required`**: Whether players must accept the resource pack to play

**Note**: For detailed information about the resource pack server, see [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md).

### Vampire Integration Settings

```yaml
vampire-integration:
  enabled: true
  prevent-conflict: true  # Prevent players from being both vampire and werewolf
```

**Settings**:
- **`enabled`**: Enable integration with the Vampire plugin
- **`prevent-conflict`**: Prevent players from being both a vampire and a werewolf

### Skin Types Configuration

```yaml
skins:
  alpha: "alpha"
  witherfang: "witherfang"
  silvermane: "silvermane"
  bloodmoon: "bloodmoon"
```

**Settings**:
- Maps werewolf types to skin identifiers
- Used to determine which skin texture to apply
- Skin files should be named accordingly in the resource pack

## Configuration Reload

You can reload the configuration without restarting the server using:

```
/werewolf reload
```

**Note**: Some settings may require a server restart to take full effect (e.g., database type changes).

## Troubleshooting

### Configuration Not Loading

- Check that `config.yml` exists in `plugins/Werewolf/`
- Verify YAML syntax (no tabs, proper indentation)
- Check server logs for configuration errors

### Resource Pack Server Issues

- **Port in use**: Change `resource-pack.server.port` to an available port
- **Server won't start**: Check server logs for detailed error messages
- **URL not auto-populated**: Ensure `resource-pack.server.enabled` is `true`

See [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md) for detailed troubleshooting.

### Database Connection Issues

- **SQLite**: Check file permissions in `plugins/Werewolf/`
- **MySQL/PostgreSQL**: Verify connection details (host, port, credentials)
- **Connection refused**: Ensure database server is running and accessible

## Best Practices

1. **Backup your configuration** before making changes
2. **Test changes** on a development server first
3. **Use SQLite** for small servers (no setup required)
4. **Enable embedded resource pack server** for automatic management
5. **Set appropriate auto-save interval** based on server activity
6. **Enable debug mode** only when troubleshooting

## See Also

- [README.md](README.md) - General plugin information
- [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md) - Resource pack and web server documentation

