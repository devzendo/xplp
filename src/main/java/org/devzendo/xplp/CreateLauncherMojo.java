/**
 * Copyright (C) 2008-2010 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.devzendo.xplp;

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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * A Maven plugin that creates launcher directory structures for Windows
 * (using Janel), Mac OS X (creating a .app or script structure) or Linux
 * (using a shell script).
 * 
 * @author Matt Gumbley, DevZendo.org
 *
 */
@Mojo( name = "createlauncher",
       defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public final class CreateLauncherMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true)
    private org.apache.maven.project.MavenProject mavenProject;

    @Component()
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    @Component()
    private org.apache.maven.artifact.resolver.ArtifactResolver artifactResolver;

    @Parameter( defaultValue = "${localRepository}", readonly = true)
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    private java.util.List<?> remoteRepositories;

    @Component()
    private ArtifactMetadataSource artifactMetadataSource;

    
    /**
     * The OS to generate a launcher for: Windows, MacOSX or Linux.
     * 
     * @required
     *
     */
    @Parameter( defaultValue = "${xplp.os}", required = true )
    private String os;
    
    /**
     * The directory into which the output files will be placed.
     * By default, this is the target directory. The launcher directory
     * structure will be created in a subdirectory of this. This subdirectory
     * will be named according to the platform specified in the os parameter,
     * so: windows, linux or macosx.
     */
    @Parameter( defaultValue = "${project.build.directory}")
    private File outputDirectory;
    
    /**
     * The fully-qualified name of the main class of your application,
     * i.e. contains a public static void main...
     */
    @Parameter( defaultValue = "${xplp.mainclassname}", required = true )
    private String mainClassName;

    /**
     * The name of the application for whom this launcher is to be
     * generated. 
     * 
     * On Mac OS X, this is used to name the application menu.
     * On Windows, this is used to name the Janel .exe/.lap files.
     * If not specified the client project's artifact id will be used.
     */
    @Parameter( defaultValue = "${xplp.applicationname}", required = true )
    private String applicationName;
    
    /**
     * The directory where the application's jars are.
     * By default, assume lib/
     */
    @Parameter( defaultValue = "${xplp.librarydirectory}")
    private String libraryDirectory = "lib";
    
    /**
     * A list of system properties, to be passed to the JVM via multiple
     * -Dxxx=yyy parameters. When specifying system properties, omit the -D, and
     * just give them as xxx=yyy. The platform-specific launcher will add in the
     * -D if necessary.
     */
    @Parameter( defaultValue = "${xplp.systemproperty}")
    private String[] systemProperties;
    
    /**
     * A list of VM arguments, to be passed straight to the JVM, e.g. -Xmx2048.
     * Note that on Mac OS X, your application should set -Xdock:name=MyApplication
     * to have the correct name in the application menu and on the dock.
     */
    @Parameter( defaultValue = "${xplp.vmargument}")
    private String[] vmArguments;

    /**
     * A list of NAR (Native ARchive, from the Maven NAR Plugin) classifiers and
     * types. These refer to native library files that have been unpacked using
     * the nar-download, nar-unpack and nar-assembly goals of the Maven NAR
     * Plugin, and reside in the target/nar/lib/classifier/type directories. 
     * </p>
     * Any files in these directories will be copied to the launcher's library
     * directory.
     * </p>
     * The params you specify here must be in the form classifier:type, e.g.
     * x86_64-MacOSX-g++:jni and you may specify as many as you like; only those
     * directories that have anything in them will have their contents copied.
     */
    @Parameter( defaultValue = "${xplp.narClassifierType}")
    private String[] narClassifierTypes;

    /**
     * The launcher type, can be "Console" or "GUI". 
     * For Windows, whether to use the Console or GUI Janel EXE.
     * For Mac OS X, whether to create a script or .app structure.
     */
    @Parameter( defaultValue = "${xplp.launchertype}")
    private String launcherType = "GUI";
    
    // Mac OS X Specific parameters -------------------------------
    
    /**
     * Mac OS X only: Any file type that is associated with this application.
     * This is registered in the Mac OS X Info.plist as CFBundleTypeExtensions.
     */
    @Parameter( defaultValue = "${xplp.filetype}")
    private String fileType;
    
    /**
     * Mac OS X only: The name of the icons file.
     */
    @Parameter( defaultValue = "${xplp.iconsfilename}")
    private String iconsFileName;
    
    /**
     * Mac OS X only: The bundle signature. Only use this if you have a registered
     * creator code.
     * This is registered in the Mac OS X Info.plist as CFBundleSignature, and
     * in the PkgInfo prefixed with APPL
     * </p>
     * As stated at <a href="http://developer.apple.com/mac/library/documentation/Java/Conceptual/Java14Development/03-JavaDeployment/JavaDeployment.html">
     * the Apple developer website</a> 
     * "This is a simple text file that contains the string APPL optionally
     * concatenated with a four letter creator code. If an application does not
     * have a registered creator code, the string APPL???? should be used."
     */
    @Parameter( defaultValue = "${xplp.bundlesignature}")
    private String bundleSignature = "????";
    
    /**
     * Mac OS X only: The bundle OS type.
     * This is registered in the Mac OS X Info.plist as CFBundleTypeOSTypes.
     */
    @Parameter( defaultValue = "${xplp.bundleostype}")
    private String bundleOsType;
    
    /**
     * Mac OS X only: The bundle type name
     * This is registered in the Mac OS X Info.plist as CFBundleTypeName.
     */
    @Parameter( defaultValue = "${xplp.bundletypename}")
    private String bundleTypeName;

    /**
     * Mac OS X GUI launchers only: whether to use Apple's JavaApplicationStub, or Tobias Fischer's universal
     * application stub. (Currently this Maven plugin supplies v2.1.0 of Tobias' stub)
     * Please see https://github.com/tofi86/universalJavaApplicationStub for details of this.
     * If you want to launch with something other than Apple Java 6, you need the universal stub.
     *
     * Can be "Apple" or "Universal". Default is "Apple" for backwards compatibility.
     */
    @Parameter( defaultValue = "${xplp.stubtype}")
    private String stubType = "Apple";

    // Windows specific parameters
    /**
     * Windows only: Whether to use the Console or GUI Janel EXE.
     * Can be "Console" or "GUI"
     * 
     * @deprecated Use launcherType instead.
     */
    @Deprecated
    @Parameter( defaultValue = "${xplp.janeltype}")
    private String janelType;
    
    /**
     * Windows only: A list of lines of text that will be added to the Janel
     * launcher file.
     */
    @Parameter( defaultValue = "${xplp.janelcustomline}")
    private String[] janelCustomLines;

    /**
     * Windows only: The (major/minor) version of Janel you wish to use to launch your program. Choices are:
     * The Original Janel 3.0.2 by Timothy D. Kil that's been discontinued.
     * The Enhanced Janel 4.2.0-98 continued by Michael Knigge.
     * Both versions of Janel are embedded in the plugin; if you are using the Enhanced version, also consider setting
     * the xplp.janelbits property.
     *
     * Can be "3.0" (currently 3.0.2) or "4.2" (currently 4.2.0-98).
     * The default is "3.0" for backwards compatibility.
     */
    @Parameter( defaultValue = "${xplp.janelversion}")
    private String janelVersion = "3.0";

    /**
     * Windows only: If using the Enhanced Janel, are you using 32- or 64-bit? The original Janel makes no such
     * distinction.
     *
     * Can be "32" or "64".
     * The default is "64" as 32-bit systems are becoming rarer.
     */
    @Parameter( defaultValue = "${xplp.janelbits}")
    private String janelBits = "64";

    /**
     * Windows only: Should the Janel .EXE, .DLL and .LAP file be in the root directory (the backwards-compatible
     * default), or in a 'bin' directory? This affects the path to the library directory in the .LAP file -
     * if "root", we can refer to the library directory as "lib", but if the .LAP file is in a "bin" directory, then
     * the path to the library directory would be "../lib".
     *
     * Can be "root" or "bin".
     * The default is "root" for backwards compatibility.
     */
    @Parameter( defaultValue = "${xplp.janeldirectory}")
    private String janelDirectory = "root";

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Cross Platform Launcher Plugin");
        if (os == null || os.equals("none")) {
            throw new MojoExecutionException("No <os>Windows|MacOSX|Linux</os> specified in the <configuration>");
        }
        if (janelType != null && !janelType.equals("")) {
            throw new MojoExecutionException("The janelType attribute has been changed to launcherType in v0.2.1 of the plugin");
        }
        validateNarClassifierTypes();
        final Set<Artifact> transitiveArtifacts = getTransitiveDependencies();
        final Set<File> resourceDirectories = getResourceDirectories();
        final Properties parameterProperties = getParameterProperties();
        getLog().info("Operating System:            " + os);
        getLog().info("Output directory:            " + outputDirectory);
        getLog().info("Main class name:             " + mainClassName);
        getLog().info("Application name:            " + applicationName);
        getLog().info("Library directory:           " + libraryDirectory);
        getLog().info("System properties:           " + dumpArray(systemProperties));
        getLog().info("VM Arguments:                " + dumpArray(vmArguments));
        getLog().info("NAR Classifier:Types:        " + dumpArray(narClassifierTypes));

        LauncherCreator launcherCreator;
        if (os.equals("MacOSX")) {
            if (launcherType.equals("GUI")) { 
                launcherCreator = new MacOSXAppLauncherCreator(this,
                    outputDirectory, mainClassName, applicationName,
                    libraryDirectory, transitiveArtifacts,
                    resourceDirectories,
                    parameterProperties, systemProperties, vmArguments,
                    narClassifierTypes, launcherType,
                    fileType, iconsFileName, bundleSignature, bundleOsType,
                    bundleTypeName, stubType);
            } else {
                launcherCreator = new MacOSXScriptLauncherCreator(this,
                    outputDirectory, mainClassName, applicationName,
                    libraryDirectory, transitiveArtifacts,
                    resourceDirectories, parameterProperties, systemProperties,
                    vmArguments, narClassifierTypes, launcherType);
            }
        } else if (os.equals("Windows")) {
            getLog().info("Janel custom lines:          " + dumpArray(janelCustomLines));

            launcherCreator = new WindowsLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts,
                resourceDirectories, parameterProperties, systemProperties,
                vmArguments, narClassifierTypes, launcherType, janelVersion, janelBits, janelCustomLines, janelDirectory);
        } else if (os.equals("Linux")) {
            launcherCreator = new LinuxLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts,
                resourceDirectories, parameterProperties, systemProperties,
                vmArguments, narClassifierTypes);
        } else {
            throw new MojoExecutionException("No <os>Windows|MacOSX|Linux</os> specified in the <configuration>");
        }
        try {
            launcherCreator.createLauncher();
        } catch (final Exception e) {
            final StackTraceElement[] stackTrace = e.getStackTrace();
            for (final StackTraceElement stackTraceElement : stackTrace) {
                getLog().info(stackTraceElement.toString());
            }
            throw new MojoFailureException("Could not create launcher: " + e.getMessage());
        }
    }
    
    private void validateNarClassifierTypes() throws MojoFailureException {
        if (narClassifierTypes == null) {
            narClassifierTypes = new String[0];
            return;
        }
        boolean allOK = true;
        for (final String narClassifierType : narClassifierTypes) {
            if (!narClassifierType.matches("^\\S+:\\S+$")) {
                getLog().error("NAR Classifier:Type '" + narClassifierType + "' is not of the form Classifier:Type");
                allOK = false;
            }
        }
        if (!allOK) {
            throw new MojoFailureException("One or more NAR Classifier:Type parameters are incorrectly specified");
        }
    }

    private String dumpArray(final Object[] objects) {
        final StringBuilder sb = new StringBuilder();
        if (objects != null) {
            sb.append('[');
            if (objects.length != 0) {
                for (int i = 0; i < objects.length - 1; i++) {
                    sb.append(objects[i]);
                    sb.append(',');
                }
                sb.append(objects[objects.length - 1]);
            }
            sb.append(']');
        }
        return sb.toString();
    }

    private Properties getParameterProperties() {
        final Properties properties = new Properties();
        // TODO there has to be an automated way of doing this!
        // mavenProject.getProperties() ?
        properties.put("xplp.os", nullToEmptyString(os));
        properties.put("xplp.outputdirectory", nullToEmptyString(outputDirectory.getPath()));
        properties.put("xplp.mainclassname", nullToEmptyString(mainClassName));
        properties.put("xplp.applicationname", nullToEmptyString(applicationName));
        properties.put("xplp.librarydirectory", nullToEmptyString(libraryDirectory));
        properties.put("xplp.filetype", nullToEmptyString(fileType));
        properties.put("xplp.iconsfilename", nullToEmptyString(iconsFileName));
        properties.put("xplp.bundlesignature", nullToEmptyString(bundleSignature));
        properties.put("xplp.bundleostype", nullToEmptyString(bundleOsType));
        properties.put("xplp.bundletypename", nullToEmptyString(bundleTypeName));
        properties.put("project.version", nullToEmptyString(mavenProject.getVersion()));
        properties.put("project.description", nullToEmptyString(mavenProject.getDescription()));
        return properties;
    }

    private String nullToEmptyString(final String in) {
        return in == null ? "" : in;
    }

    @SuppressWarnings("unchecked")
    private Set<File> getResourceDirectories() {
        final HashSet<File> resourceDirs = new HashSet<File>();
        final List<Resource> resources = mavenProject.getResources();
        for (final Resource resource : resources) {
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
            for (final Artifact artifact : result) {
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
