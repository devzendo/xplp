/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

/**
 * Creates a Windows launcher directory structure.
 * @author matt
 *
 */
public class WindowsLauncherCreator extends LauncherCreator {
    public WindowsLauncherCreator(AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory, final Set<Artifact> transitiveArtifacts, Set<File> resourceDirectories) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        // TODO Auto-generated method stub
    }
}
