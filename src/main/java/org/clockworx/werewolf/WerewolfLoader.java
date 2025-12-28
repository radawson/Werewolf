package org.clockworx.werewolf;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Plugin loader for the Werewolf Paper plugin.
 * This class is responsible for setting up the plugin's classpath,
 * including any external libraries that need to be loaded.
 * 
 * Currently, this is a placeholder for future classpath management.
 * External libraries are handled via shadowJar in the build process.
 */
public class WerewolfLoader implements PluginLoader {

    /**
     * Configures the plugin's classpath by adding external libraries.
     * This method is called during plugin loading to set up dependencies.
     *
     * @param classpathBuilder The classpath builder for adding libraries.
     */
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        // External libraries are handled via shadowJar in the build process
        // This method can be used to add libraries dynamically if needed in the future
        
        // Example of how to add a Maven library (commented out for now):
        /*
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addDependency(new Dependency(
            new DefaultArtifact("com.example:example:version"), 
            null
        ));
        resolver.addRepository(new RemoteRepository.Builder(
            "paper", 
            "default", 
            "https://repo.papermc.io/repository/maven-public/"
        ).build());
        classpathBuilder.addLibrary(resolver);
        */
    }
}

