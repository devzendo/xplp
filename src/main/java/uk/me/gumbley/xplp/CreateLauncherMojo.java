package uk.me.gumbley.xplp;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * A Maven plugin that creates launchers for Windows
 * (using Janel), Mac OS X (creating a .app structure) or Linux
 * (using a shell script)
 * 
 * @author matt
 * @phase generate-resources
 * @goal createlauncher
 *
 */
public final class CreateLauncherMojo extends AbstractMojo {

    
    /**
     * The OS to generate a launcher for: Windows, MacOSX or Linux.
     * 
     * @required
     * @parameter expression="${xplp.os}" default-value="none" 
     * 
     */
    private String os;
    
    /**
     * The directory into which the output files will be placed.
     * By default, this is the target directory.
     * 
     * @parameter expression=${project.build.directory}
     */
    private File outputDirectory;
    
    /**
     * The fully-qualified name of the main class of your application,
     * i.e. contains a public static void main...
     * 
     * @required
     * @parameter expression="${xplp.mainclassname}
     */
    private String mainClassName;

    /**
     * The name of the application for whom this launcher is to be
     * generated. This is used to name the application menu on
     * Mac OS X, to name the Janel .exe/.lap files.
     * By default, take the client project's artifact id.
     * 
     * @required
     * @parameter expression="${xplp.applicationname}"
     */
    private String applicationName;
    
    /**
     * The directory where the application's jars are.
     * By default, assume lib/
     * 
     * @parameter expression="${xplp.librarydirectory} default-value="lib"
     */
    private String libraryDirectory;
    
    // Mac OS X Specific parameters -------------------------------
    
    /**
     * Any file type that is associated with this application.
     * This is registered in the Mac OS X Info.plist.
     * 
     * @parameter expression="${xplp.filetype}"
     */
    private String fileType;
    
    /**
     * The name of the icons file.
     * 
     * @parameter expression="${xplp.iconsfilename}"
     */
    private String iconsFileName;
    
    /**
     * The bundle signature.
     * 
     * @parameter expression="${xplp.bundlesignature}"
     */
    private String bundleSignature;
    
    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Cross Platform Launcher Plugin");
        if (os == null || os.equals("none")) {
            throw new MojoExecutionException("No <os>Windows|MacOSX|Linux</os> specified in the <configuration>");
        }
        getLog().info("Operating System:  " + os);
        getLog().info("Output directory:  " + outputDirectory);
        getLog().info("Main class name:   " + mainClassName);
        getLog().info("Application name:  " + applicationName);
        getLog().info("Library directory: " + libraryDirectory);
        
    }
}
