package uk.me.gumbley.xplp;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

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
     * The Maven project.
     * @parameter expression="${project}"
     */ 
    private org.apache.maven.project.MavenProject mavenProject; 
    /** @component */ 
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory; 
    /** @component */ 
    private org.apache.maven.artifact.resolver.ArtifactResolver artifactResolver; 
    /** @parameter expression="${localRepository}"  */ 
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository; 
    /** @parameter expression="${project.remoteArtifactRepositories}"  */ 
    private java.util.List<?> remoteRepositories; 
    /** @component */ 
    private ArtifactMetadataSource artifactMetadataSource;

    
    /**
     * The OS to generate a launcher for: Windows, MacOSX or Linux.
     * 
     * @required
     * @parameter expression="${xplp.os}"
     * 
     */
    private String os;
    
    /**
     * The directory into which the output files will be placed.
     * By default, this is the target directory.
     * 
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;
    
    /**
     * The fully-qualified name of the main class of your application,
     * i.e. contains a public static void main...
     * 
     * @required
     * @parameter expression="${xplp.mainclassname}"
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
     * @parameter expression="${xplp.librarydirectory}" default-value="lib"
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
     * The bundle OS type.
     * 
     * @parameter expression="${xplp.bundleostype}"
     */
    private String bundleOsType;
    
    /**
     * The bundle type name
     * 
     * @parameter expression="${xplp.bundletypename}"
     */
    private String bundleTypeName;
    
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
        final Set<Artifact> transitiveArtifacts = getTransitiveDependencies();
        final Set<File> resourceDirectories = getResourceDirectories();
        final Properties parameterProperties = getParameterProperties();
        
        LauncherCreator launcherCreator;
        if (os.equals("MacOSX")) {
            launcherCreator = new MacOSXLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts, resourceDirectories,
                fileType, iconsFileName, bundleSignature,
                bundleOsType, bundleTypeName);
        } else if (os.equals("Windows")) {
            launcherCreator = new WindowsLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts, resourceDirectories);
        } else if (os.equals("Linux")) {
            launcherCreator = new LinuxLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts, resourceDirectories);
        } else {
            throw new MojoExecutionException("No <os>Windows|MacOSX|Linux</os> specified in the <configuration>");
        }
        try {
            launcherCreator.createLauncher();
        } catch (final Exception e) {
            throw new MojoFailureException(("Could not create launcher: " + e.getMessage()));
        }
    }
    
    private Properties getParameterProperties() {
        final Properties properties = new Properties();
        this.getClass().getAnnotation(arg0)
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    private Set<File> getResourceDirectories() {
        final HashSet<File> resourceDirs = new HashSet<File>();
        final List<Resource> resources = mavenProject.getResources();
        for (Resource resource : resources) {
            final String directory = resource.getDirectory();
            final File directoryFile = new File(directory);
            if (directoryFile.exists() && directoryFile.isDirectory()) {
                resourceDirs.add(directoryFile);
            }
        }
        return resourceDirs;
    }

    @SuppressWarnings("unchecked")
    private Set<Artifact> getTransitiveDependencies() throws MojoFailureException {
        getLog().info("Resolving transitive dependencies");
        Set<?> artifacts;
        try {
            artifacts = mavenProject.createArtifacts(artifactFactory, null, null);
            
//          For unknown reasons, this fails to filter - nothing's returned                
//            final TypeArtifactFilter typeFilter = new TypeArtifactFilter("jar");
//            final ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter("compile");
//            final AndArtifactFilter filter = new AndArtifactFilter();
//            filter.add(typeFilter);
//            filter.add(scopeFilter);
            
            final Set<Artifact> result = artifactResolver.resolveTransitively(artifacts,
                mavenProject.getArtifact(), localRepository, remoteRepositories,
                artifactMetadataSource, null).getArtifacts(); 
            for (Artifact artifact : result) {
                getLog().debug("Transitive artifact: " + artifact.toString());
                getLog().debug("   File: " + artifact.getFile().getAbsolutePath());
            }
            getLog().info("Transitive dependencies resolved");
            return result;
        } catch (final InvalidDependencyVersionException e) {
            final String message = "Invalid dependency version: " + e.getMessage();
            getLog().warn(message);
            throw new MojoFailureException(message);
        } catch (final ArtifactResolutionException e) {
            final String message = "Artifact failed to resolve: " + e.getMessage();
            getLog().warn(message);
            throw new MojoFailureException(message);
        } catch (final ArtifactNotFoundException e) {
            final String message = "Artifact not found: " + e.getMessage();
            getLog().warn(message);
            throw new MojoFailureException(message);
        } 
    }
}
