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
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * A Maven plugin that creates launcher directory structures for Windows
 * (using Janel), Mac OS X (creating a .app structure) or Linux
 * (using a shell script).
 * 
 * @author Matt Gumbley, DevZendo.org
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
     * By default, this is the target directory. The launcher directory
     * structure will be created in a subdirectory of this. This subdirectory
     * will be named according to the platform specified in the os parameter,
     * so: windows, linux or macosx.
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
     * generated. 
     * 
     * On Mac OS X, this is used to name the application menu.
     * On Windows, this is used to name the Janel .exe/.lap files.
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
     * Mac OS X only: Any file type that is associated with this application.
     * This is registered in the Mac OS X Info.plist as CFBundleTypeExtensions.
     * 
     * @parameter expression="${xplp.filetype}"
     */
    private String fileType;
    
    /**
     * Mac OS X only: The name of the icons file.
     * 
     * @parameter expression="${xplp.iconsfilename}"
     */
    private String iconsFileName;
    
    /**
     * Mac OS X only: The bundle signature.
     * This is registered in the Mac OS X Info.plist as CFBundleSignature, and
     * in the PkgInfo as APPL${xplp.bundlesignature}.
     * 
     * @parameter expression="${xplp.bundlesignature}"
     */
    private String bundleSignature;
    
    /**
     * Mac OS X only: The bundle OS type.
     * This is registered in the Mac OS X Info.plist as CFBundleTypeOSTypes.
     * 
     * @parameter expression="${xplp.bundleostype}"
     */
    private String bundleOsType;
    
    /**
     * Mac OS X only: The bundle type name
     * This is registered in the Mac OS X Info.plist as CFBundleTypeName.
     * 
     * @parameter expression="${xplp.bundletypename}"
     */
    private String bundleTypeName;
    
    // Windows specific parameters
    /**
     * Windows only: Whether to use the Console or GUI Janel EXE.
     * Can be "Console" or "GUI"
     * 
     * @parameter expression="${xplp.janeltype}" default-value="GUI"
     */
    private String janelType;
    
    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Cross Platform Launcher Plugin");
        if (os == null || os.equals("none")) {
            throw new MojoExecutionException("No <os>Windows|MacOSX|Linux</os> specified in the <configuration>");
        }
        final Set<Artifact> transitiveArtifacts = getTransitiveDependencies();
        final Set<File> resourceDirectories = getResourceDirectories();
        final Properties parameterProperties = getParameterProperties();
        getLog().info("Operating System:  " + os);
        getLog().info("Output directory:  " + outputDirectory);
        getLog().info("Main class name:   " + mainClassName);
        getLog().info("Application name:  " + applicationName);
        getLog().info("Library directory: " + libraryDirectory);
        
        LauncherCreator launcherCreator;
        if (os.equals("MacOSX")) {
            launcherCreator = new MacOSXLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts,
                resourceDirectories,
                parameterProperties, fileType, iconsFileName,
                bundleSignature, bundleOsType, bundleTypeName);
        } else if (os.equals("Windows")) {
            launcherCreator = new WindowsLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts,
                resourceDirectories, parameterProperties, janelType);
        } else if (os.equals("Linux")) {
            launcherCreator = new LinuxLauncherCreator(this,
                outputDirectory, mainClassName, applicationName,
                libraryDirectory, transitiveArtifacts,
                resourceDirectories, parameterProperties);
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
