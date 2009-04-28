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
    public LinuxLauncherCreator(AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory, final Set<Artifact> transitiveArtifacts, Set<File> resourceDirectories, Properties parameterProperties) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories, parameterProperties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        // TODO Auto-generated method stub
    }
}
