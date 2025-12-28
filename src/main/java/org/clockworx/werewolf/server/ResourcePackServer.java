package org.clockworx.werewolf.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.clockworx.werewolf.WerewolfPlugin;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Embedded HTTP server for serving the Werewolf resource pack.
 * Extracts the resource pack from the JAR, creates a ZIP file,
 * calculates its hash, and serves it via HTTP.
 */
public class ResourcePackServer extends NanoHTTPD {
    
    private final WerewolfPlugin plugin;
    private final File resourcePackFile;
    private final int port;
    private byte[] resourcePackHash;
    private String resourcePackHashHex;
    private boolean started;
    
    /**
     * Creates a new ResourcePackServer.
     *
     * @param plugin The WerewolfPlugin instance.
     * @param port The port to serve on.
     */
    public ResourcePackServer(WerewolfPlugin plugin, int port) {
        super(port);
        this.plugin = plugin;
        this.port = port;
        this.resourcePackFile = new File(plugin.getDataFolder(), "resourcepack.zip");
        this.resourcePackHash = null;
        this.resourcePackHashHex = null;
        this.started = false;
    }
    
    /**
     * Starts the HTTP server and extracts the resource pack if needed.
     *
     * @return True if the server started successfully, false otherwise.
     */
    public boolean startServer() {
        if (started) {
            return true;
        }
        
        try {
            // Extract resource pack if it doesn't exist
            if (!extractResourcePack()) {
                plugin.getLogger().severe("Failed to extract resource pack. HTTP server will not start.");
                return false;
            }
            
            // Calculate hash
            if (!calculateHash()) {
                plugin.getLogger().warning("Failed to calculate resource pack hash. Continuing without hash.");
            }
            
            // Start the HTTP server
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            started = true;
            plugin.getLogger().info("Resource pack HTTP server started on port " + getListeningPort());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start resource pack HTTP server", e);
            return false;
        }
    }
    
    /**
     * Stops the HTTP server.
     */
    public void stopServer() {
        if (started) {
            stop();
            started = false;
            plugin.getLogger().info("Resource pack HTTP server stopped.");
        }
    }
    
    /**
     * Handles HTTP requests.
     *
     * @param session The HTTP session.
     * @return The HTTP response.
     */
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        
        // Serve resourcepack.zip
        if (uri.equals("/resourcepack.zip") || uri.equals("/")) {
            if (session.getMethod() == Method.GET || session.getMethod() == Method.HEAD) {
                return serveResourcePack(session.getMethod() == Method.HEAD);
            }
        }
        
        // 404 for other paths
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found");
    }
    
    /**
     * Serves the resource pack ZIP file.
     *
     * @param headOnly If true, return only headers (HEAD request).
     * @return The HTTP response.
     */
    private Response serveResourcePack(boolean headOnly) {
        if (!resourcePackFile.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Resource pack not found");
        }
        
        try {
            long fileSize = resourcePackFile.length();
            Response response = newFixedLengthResponse(
                Response.Status.OK,
                "application/zip",
                headOnly ? null : new FileInputStream(resourcePackFile),
                fileSize
            );
            response.addHeader("Content-Disposition", "attachment; filename=\"resourcepack.zip\"");
            return response;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error serving resource pack", e);
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error serving resource pack"
            );
        }
    }
    
    /**
     * Extracts the resource pack from the JAR and creates a ZIP file.
     *
     * @return True if extraction was successful, false otherwise.
     */
    private boolean extractResourcePack() {
        // Check if resource pack already exists
        if (resourcePackFile.exists()) {
            plugin.getLogger().info("Resource pack already exists, skipping extraction.");
            return true;
        }
        
        plugin.getLogger().info("Extracting resource pack from JAR...");
        
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        try {
            // Get the JAR file location
            File jarFile = new File(getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            
            if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
                plugin.getLogger().warning("Could not find plugin JAR file. Resource pack extraction may fail.");
            }
            
            // Create ZIP file from JAR resources
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(resourcePackFile))) {
                // Read from JAR
                if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                    try (JarFile jar = new JarFile(jarFile)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            
                            // Only include resourcepack/ directory
                            if (name.startsWith("resourcepack/") && !entry.isDirectory()) {
                                String zipEntryName = name.substring("resourcepack/".length());
                                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                                zos.putNextEntry(zipEntry);
                                
                                try (InputStream is = jar.getInputStream(entry)) {
                                    byte[] buffer = new byte[8192];
                                    int len;
                                    while ((len = is.read(buffer)) > 0) {
                                        zos.write(buffer, 0, len);
                                    }
                                }
                                zos.closeEntry();
                            }
                        }
                    }
                } else {
                    // Fallback: try to read from classpath resources
                    InputStream packMeta = plugin.getResource("resourcepack/pack.mcmeta");
                    if (packMeta != null) {
                        // Create minimal resource pack structure
                        addResourceToZip(zos, "pack.mcmeta", packMeta);
                        packMeta.close();
                    } else {
                        plugin.getLogger().warning("Could not find resourcepack/pack.mcmeta in JAR. Creating empty resource pack.");
                        // Create minimal pack.mcmeta
                        String packMetaContent = "{\n" +
                            "    \"pack\": {\n" +
                            "        \"pack_format\": 15,\n" +
                            "        \"description\": \"Werewolf Plugin Resource Pack\"\n" +
                            "    }\n" +
                            "}";
                        zos.putNextEntry(new ZipEntry("pack.mcmeta"));
                        zos.write(packMetaContent.getBytes());
                        zos.closeEntry();
                    }
                }
            }
            
            plugin.getLogger().info("Resource pack extracted successfully to: " + resourcePackFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to extract resource pack from JAR", e);
            return false;
        }
    }
    
    /**
     * Adds a resource to the ZIP file.
     *
     * @param zos The ZipOutputStream.
     * @param entryName The entry name in the ZIP.
     * @param inputStream The input stream to read from.
     * @throws IOException If an I/O error occurs.
     */
    private void addResourceToZip(ZipOutputStream zos, String entryName, InputStream inputStream) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }
        
        zos.closeEntry();
    }
    
    /**
     * Calculates the SHA-1 hash of the resource pack ZIP file.
     *
     * @return True if hash calculation was successful, false otherwise.
     */
    private boolean calculateHash() {
        if (!resourcePackFile.exists()) {
            return false;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (FileInputStream fis = new FileInputStream(resourcePackFile)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }
            }
            
            resourcePackHash = digest.digest();
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : resourcePackHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            resourcePackHashHex = hexString.toString();
            
            plugin.getLogger().info("Resource pack hash calculated: " + resourcePackHashHex);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to calculate resource pack hash", e);
            return false;
        }
    }
    
    /**
     * Gets the resource pack URL.
     *
     * @param host The host to use (usually "localhost" or server IP).
     * @return The resource pack URL.
     */
    public String getResourcePackUrl(String host) {
        int actualPort = getListeningPort();
        if (actualPort == -1) {
            actualPort = this.port; // Use configured port if not started yet
        }
        return "http://" + host + ":" + actualPort + "/resourcepack.zip";
    }
    
    /**
     * Gets the resource pack hash as a byte array.
     *
     * @return The hash as byte array, or null if not calculated.
     */
    public byte[] getResourcePackHash() {
        return resourcePackHash != null ? resourcePackHash.clone() : null;
    }
    
    /**
     * Gets the resource pack hash as a hex string.
     *
     * @return The hash as hex string, or null if not calculated.
     */
    public String getResourcePackHashHex() {
        return resourcePackHashHex;
    }
    
    /**
     * Gets the resource pack file.
     *
     * @return The resource pack ZIP file.
     */
    public File getResourcePackFile() {
        return resourcePackFile;
    }
    
    /**
     * Checks if the server is started.
     *
     * @return True if started, false otherwise.
     */
    public boolean isStarted() {
        return started;
    }
}

