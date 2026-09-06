package net.chamosmp.chamoitemskins;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings("UnstableApiUsage")
public class ChamoItemSkinsLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:7.1.0"), null));

        MavenLibraryResolver chamoSmpRepo = new MavenLibraryResolver();

        chamoSmpRepo.addRepository(new RemoteRepository.Builder("chamoSmp", "default", "https://maven.chamosmp.net/releases").build());
        chamoSmpRepo.addDependency(new Dependency(new DefaultArtifact("net.chamosmp.sqdlib:sqd-lib:1.1.0"), null));

        classpathBuilder.addLibrary(resolver);
    }

}