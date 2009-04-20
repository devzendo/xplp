/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;

/**
 * Creates a Windows launcher directory structure.
 * @author matt
 *
 */
public class WindowsLauncherCreator extends LauncherCreator {
    public WindowsLauncherCreator(final File outputDirectory,
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
