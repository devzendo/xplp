/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;

/**
 * Create a Linux launcher directory structure.
 * @author matt
 *
 */
public class LinuxLauncherCreator extends LauncherCreator {
    public LinuxLauncherCreator(final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory) {
        super(outputDirectory, mainClassName, applicationName,
            libraryDirectory);
    }

    /**
     * {@inheritDoc}
     */
    public void createLauncher() throws IOException {
        // TODO Auto-generated method stub
    }
}
