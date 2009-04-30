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
 * Creates a Windows launcher directory structure.
 * @author matt
 *
 */
public class WindowsLauncherCreator extends LauncherCreator {
    private static final String MSVCR71_DLL = "msvcr71.dll";
    private final String mJanelType;

    public WindowsLauncherCreator(AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories,
            final Properties parameterProperties,
            final String janelType) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories,
            parameterProperties);
        mJanelType = janelType;
    }
    
    private void validate() {
        if (mJanelType == null ||
                mJanelType.length() == 0) {
            final String message = "No janelType specified - this is mandatory for Windows";
            getMojo().getLog().warn(message);
            throw new IllegalStateException(message);
        }
        if (! (mJanelType.equals("Console") || mJanelType.equals("GUI"))) {
            final String message = "janelType must be either 'Console' or 'GUI' (GUI is the default if not specified)";
            getMojo().getLog().warn(message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        validate();
        getMojo().getLog().info("Janel .EXE type:   " + mJanelType);
        
        final File libDir = new File(getOutputDirectory(), "lib");
        libDir.mkdirs();
        final boolean allDirsOK = libDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + getOutputDirectory().getAbsolutePath());
        }
        
        final File outputJanelEXE = new File(getOutputDirectory(), getApplicationName() + ".exe");
        final String janelEXEResource = "windows/" + (mJanelType.equals("Console") ? "JanelConsole.exe" : "JanelWindows.exe");
        copyPluginResource(janelEXEResource, outputJanelEXE);
        // TODO icon munging in the launcher .EXE
        copyPluginResource("windows/" + MSVCR71_DLL, new File(getOutputDirectory(), MSVCR71_DLL));
        
        copyInterpolatedPluginResource("windows/launcher.lap", new File(getOutputDirectory(), getApplicationName() + ".lap"));

        copyTransitiveArtifacts(libDir);
    }
}
