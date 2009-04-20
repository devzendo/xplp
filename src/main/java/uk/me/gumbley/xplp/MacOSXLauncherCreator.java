/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;

/**
 * Creates a Mac OS X launcher directory structure.
 * 
 * @author matt
 *
 */
public final class MacOSXLauncherCreator extends LauncherCreator {
    public MacOSXLauncherCreator(final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final String fileType,
            final String iconsFileName,
            final String bundleSignature,
            final String bundleOsType) {
        super(outputDirectory, mainClassName, applicationName,
            libraryDirectory);
    }

    /**
     * {@inheritDoc}
     */
    public void createLauncher() throws IOException {
        final File appDir = new File(getOutputDirectory(), getApplicationName() + ".app");
        final File contentsDir = new File(appDir, "Contents");
        final File macOSDir = new File(contentsDir, "MacOS");
        final File resourcesDir = new File(contentsDir, "Resources");
        appDir.mkdirs();
        contentsDir.mkdirs();
        macOSDir.mkdirs();
        resourcesDir.mkdirs();
        final boolean allDirsOK = appDir.exists() && contentsDir.exists() &&
            macOSDir.exists() && resourcesDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + appDir.getAbsolutePath());
        }
        // TODO Auto-generated method stub
    }
}
