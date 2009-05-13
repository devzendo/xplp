/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

/**
 * Create a Linux launcher directory structure.
 * @author matt
 *
 */
public class LinuxLauncherCreator extends LauncherCreator {
    public LinuxLauncherCreator(final AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories,
            final Properties parameterProperties) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories, parameterProperties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        final File osOutputDir = new File(getOutputDirectory(), "linux");
        final File binDir = new File(osOutputDir, "bin");
        final File libDir = new File(osOutputDir, "lib");
        osOutputDir.mkdirs();
        binDir.mkdirs();
        libDir.mkdirs();
        final boolean allDirsOK = osOutputDir.exists() &&
            binDir.exists() && libDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + getOutputDirectory().getAbsolutePath());
        }
        
        final File outputRunScript = new File(binDir, getApplicationName());
        copyInterpolatedPluginResource("linux/launcher.sh", outputRunScript);
        makeExecutable(outputRunScript);
        
        copyTransitiveArtifacts(libDir);
    }
}
