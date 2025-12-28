# Resource Pack and Web Server Documentation

This document provides comprehensive information about the Werewolf plugin's resource pack system and embedded HTTP server.

## Overview

The Werewolf plugin includes an embedded HTTP server that automatically serves the resource pack to players. This eliminates the need for external hosting while providing automatic hash calculation and seamless integration with the skin transformation system.

## Embedded HTTP Server

### How It Works

The plugin includes a lightweight HTTP server (NanoHTTPD) that:

1. **Extracts the resource pack** from the plugin JAR on first run
2. **Creates a ZIP file** containing all resource pack assets
3. **Calculates the SHA-1 hash** automatically
4. **Serves the resource pack** via HTTP on a configurable port
5. **Auto-populates** the URL and hash in the configuration

### Configuration

The resource pack server can be configured in `config.yml`:

```yaml
resource-pack:
  server:
    enabled: true  # Enable embedded HTTP server
    port: 8080     # Port for HTTP server (default: 8080)
    host: "localhost"  # Host (usually "localhost" for local access)
  url: ""  # Auto-populated if server enabled
  hash: ""  # Auto-calculated if server enabled
  required: false  # Whether players must accept resource pack
```

### Port Configuration

- **Default Port**: 8080
- **Port Selection**: Choose a port that is not in use by other services
- **Firewall**: If players connect from outside the server, ensure the port is open in your firewall

### Localhost vs External IP

- **Localhost** (`host: "localhost"`): Best for local testing or when players connect from the same machine
- **Server IP**: For external access, change `host` to your server's IP address or domain name

### Automatic Configuration

When the embedded server is enabled:

1. The resource pack is extracted to `plugins/Werewolf/resourcepack.zip`
2. The SHA-1 hash is calculated automatically
3. The URL is set to `http://<host>:<port>/resourcepack.zip`
4. Both URL and hash are saved to `config.yml`

### Troubleshooting

#### Server Won't Start (Port in Use)

**Problem**: The HTTP server fails to start because the port is already in use.

**Solutions**:
1. Change the port in `config.yml` to an available port
2. Stop the service using the port
3. Check for other plugins using the same port

**Example**:
```yaml
resource-pack:
  server:
    port: 8081  # Use a different port
```

#### Resource Pack Not Downloading

**Problem**: Players don't receive the resource pack.

**Solutions**:
1. Check that the server started successfully (check server logs)
2. Verify the URL is correct in `config.yml`
3. Test the URL in a browser: `http://localhost:8080/resourcepack.zip`
4. Check firewall settings if players connect externally
5. Ensure the resource pack file exists: `plugins/Werewolf/resourcepack.zip`

#### Hash Verification Errors

**Problem**: Minecraft reports hash verification failures.

**Solutions**:
1. The hash is calculated automatically - if it fails, check server logs
2. If using manual hash, ensure it's the correct SHA-1 hash of the ZIP file
3. Re-extract the resource pack by deleting `plugins/Werewolf/resourcepack.zip`
4. Restart the server to recalculate the hash

#### Firewall Configuration

**Problem**: External players can't download the resource pack.

**Solutions**:
1. Open the configured port in your firewall
2. For Linux (iptables):
   ```bash
   sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
   ```
3. For Windows Firewall: Add an inbound rule for the port
4. Update `host` in config to your server's external IP or domain

## Resource Pack Structure

The resource pack follows the standard Minecraft resource pack structure:

```
resourcepack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        └── textures/
            └── entity/
                └── player/
                    ├── slim/
                    │   ├── alpha.png
                    │   ├── witherfang.png
                    │   ├── silvermane.png
                    │   └── bloodmoon.png
                    └── wide/
                        ├── alpha.png
                        ├── witherfang.png
                        ├── silvermane.png
                        └── bloodmoon.png
```

### Skin Texture Format

#### File Requirements

- **Format**: PNG (Portable Network Graphics)
- **Dimensions**: 
  - Classic: 64x64 pixels
  - Modern: 64x128 pixels (supports more detail)
- **Model Support**: Both slim (Alex) and wide (Steve) models

#### File Naming

Skin textures should be named according to the werewolf type:
- `alpha.png` - Alpha werewolf skin
- `witherfang.png` - Witherfang werewolf skin
- `silvermane.png` - Silvermane werewolf skin
- `bloodmoon.png` - Bloodmoon werewolf skin

#### Directory Structure

- **`slim/`**: Contains skins for the slim (Alex) player model
- **`wide/`**: Contains skins for the wide (Steve) player model

Both directories should contain the same skin files for maximum compatibility.

### Adding Custom Skin Textures

1. **Create or obtain PNG skin files** (64x64 or 64x128 pixels)
2. **Place them in the appropriate directories**:
   - `src/main/resources/resourcepack/assets/minecraft/textures/entity/player/slim/`
   - `src/main/resources/resourcepack/assets/minecraft/textures/entity/player/wide/`
3. **Rebuild the plugin** - The resource pack will be included in the JAR
4. **Restart the server** - The resource pack will be extracted and served automatically

### Skin Texture Creation Tips

- Use a Minecraft skin editor (e.g., Minecraft Skin Editor, Skindex)
- Ensure transparency is preserved for overlay layers
- Test both slim and wide models
- Keep file sizes reasonable (< 100KB per texture)

## Alternative Hosting Methods

### Using Apache HTTP Server

If you prefer to use Apache instead of the embedded server:

1. **Disable the embedded server** in `config.yml`:
   ```yaml
   resource-pack:
     server:
       enabled: false
   ```

2. **Configure Apache** to serve the resource pack:
   - Copy `plugins/Werewolf/resourcepack.zip` to your web root
   - Configure Apache to serve the file
   - Set the URL in `config.yml`:
     ```yaml
     resource-pack:
       url: "http://your-domain.com/resourcepack.zip"
     ```

3. **Calculate the hash manually**:
   ```bash
   sha1sum resourcepack.zip
   ```
   Then set it in `config.yml`:
   ```yaml
   resource-pack:
     hash: "your-sha1-hash-here"
   ```

### External Hosting

You can host the resource pack on:
- GitHub Releases
- Cloud storage (Dropbox, Google Drive with direct links)
- CDN services
- Your own web server

**Configuration**:
```yaml
resource-pack:
  server:
    enabled: false
  url: "https://your-hosting-url.com/resourcepack.zip"
  hash: "your-sha1-hash-here"
```

## Migration from External Hosting

To migrate from external hosting to the embedded server:

1. **Enable the embedded server** in `config.yml`:
   ```yaml
   resource-pack:
     server:
       enabled: true
   ```

2. **Remove manual URL and hash** (they will be auto-populated):
   ```yaml
   resource-pack:
     url: ""  # Will be auto-populated
     hash: ""  # Will be auto-calculated
   ```

3. **Restart the server** - The plugin will extract the resource pack and start serving it

4. **Verify** - Check that the URL and hash are populated in `config.yml`

## Technical Details

### NanoHTTPD Integration

- **Lightweight**: ~50KB library size
- **Single-threaded by default**: Sufficient for resource pack serving
- **No external dependencies**: Uses only Java standard library
- **Thread pool support**: Can handle concurrent requests

### Hash Calculation

- **Algorithm**: SHA-1 (required by Minecraft)
- **Format**: Hexadecimal string
- **Caching**: Hash is calculated once and cached
- **Recalculation**: Automatically recalculated if resource pack file changes

### Resource Pack Extraction

- **Location**: `plugins/Werewolf/resourcepack.zip`
- **Extraction**: Happens on first server start
- **Update**: Resource pack is re-extracted if the JAR is updated
- **Persistence**: Resource pack persists between server restarts

## Security Considerations

### Localhost Access

By default, the server binds to `localhost`, which means:
- **Secure**: Only accessible from the same machine
- **Local testing**: Perfect for development
- **External access**: Requires changing `host` to server IP

### External Access

If you enable external access:
- **Firewall**: Ensure the port is properly secured
- **HTTPS**: Consider using a reverse proxy with HTTPS
- **Rate limiting**: The embedded server is lightweight but consider rate limiting for production

## Troubleshooting Checklist

- [ ] Server started successfully (check logs)
- [ ] Port is not in use by another service
- [ ] Resource pack file exists: `plugins/Werewolf/resourcepack.zip`
- [ ] URL is accessible: Test in browser
- [ ] Hash is calculated: Check `config.yml`
- [ ] Firewall allows connections (if external access)
- [ ] Resource pack structure is correct
- [ ] Skin textures are valid PNG files
- [ ] Both slim and wide model skins exist

## See Also

- [README.md](../README.md) - General plugin information
- [CONFIG.md](../CONFIG.md) - Complete configuration guide

