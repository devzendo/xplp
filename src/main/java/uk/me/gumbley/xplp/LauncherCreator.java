/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;

/**
 * A LauncherCreator is given all the parameters, extracted from
 * the plugin configuration, and creates an appropriate
 * launcher filesystem structure under the output directory.
 * 
 * @author matt
 *
 */
public abstract class LauncherCreator {
    private final File mOutputDirectory;
    private final String mMainClassName;
    private final String mApplicationName;
    private final String mLibraryDirectory;

    public LauncherCreator(final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory) {
                mOutputDirectory = outputDirectory;
                mMainClassName = mainClassName;
                mApplicationName = applicationName;
                mLibraryDirectory = libraryDirectory;
    }

    /**
     * @return the outputDirectory
     */
    protected final File getOutputDirectory() {
        return mOutputDirectory;
    }

    /**
     * @return the mainClassName
     */
    protected final String getMainClassName() {
        return mMainClassName;
    }

    /**
     * @return the applicationName
     */
    protected final String getApplicationName() {
        return mApplicationName;
    }

    /**
     * @return the libraryDirectory
     */
    protected final String getLibraryDirectory() {
        return mLibraryDirectory;
    }

    public abstract void createLauncher() throws IOException;
}
