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

/**
 * 
 */
package org.devzendo.xplp;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;


/**
 * Creates a Mac OS X launcher directory structure.
 * See
 * http://developer.apple.com/documentation/Java/Conceptual/Java14Development/03-JavaDeployment/JavaDeployment.html
 * for details.
 * 
 * @author matt
 *
 */
public final class MacOSXLauncherCreator extends LauncherCreator {
    private final String mFileType;
    private final String mIconsFileName;
    private final String mBundleSignature;
    private final String mBundleOsType;
    private final String mBundleTypeName;

    /**
     * @param mojo the parent mojo class
     * @param outputDirectory where to create the .app structure 
     * @param mainClassName the main class
     * @param applicationName the name of the application
     * @param libraryDirectory where the libraries are stored
     * @param transitiveArtifacts the set of transitive artifact dependencies
     * @param resourceDirectories the project's resource directories
     * @param parameterProperties the plugin configuration parameters, as properties
     * @param fileType the file type (currently unused)
     * @param iconsFileName the name of the icons file
     * @param bundleSignature the bundle signature
     * @param bundleOsType the bundle OS type
     * @param bundleTypeName the bundle type name
     */
    public MacOSXLauncherCreator(final AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories,
            final Properties parameterProperties,
            final String fileType,
            final String iconsFileName,
            final String bundleSignature,
            final String bundleOsType,
            final String bundleTypeName) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories,
            parameterProperties);
        mFileType = fileType;
        mIconsFileName = iconsFileName;
        mBundleSignature = bundleSignature;
        mBundleOsType = bundleOsType;
        mBundleTypeName = bundleTypeName;
    }

    private void validate() {
        if (mIconsFileName == null || mIconsFileName.length() == 0) {
            final String message = "No iconsFileName specified - this is mandatory for Mac OS X";
            getMojo().getLog().warn(message);
            throw new IllegalStateException(message);
        }
        if (mBundleSignature == null || mBundleSignature.length() == 0) {
            final String message = "No bundleSignature specified - this is mandatory for Mac OS X";
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
        setTransitiveArtifactsAsParameterProperty();
        getMojo().getLog().info("Icons file name:   " + mIconsFileName);
        getMojo().getLog().info("File type:         " + mFileType);
        getMojo().getLog().info("Bundle signature:  " + mBundleSignature);
        getMojo().getLog().info("Bundle OS type:    " + mBundleOsType);
        getMojo().getLog().info("Bundle type name:  " + mBundleTypeName);
        
        final File osOutputDir = new File(getOutputDirectory(), "macosx");
        final File appDir = new File(osOutputDir, getApplicationName() + ".app");
        final File contentsDir = new File(appDir, "Contents");
        final File macOSDir = new File(contentsDir, "MacOS");
        final File resourcesDir = new File(contentsDir, "Resources");
        final File libDir = new File(resourcesDir, "Java/lib");
        osOutputDir.mkdirs();
        appDir.mkdirs();
        contentsDir.mkdirs();
        macOSDir.mkdirs();
        resourcesDir.mkdirs();
        libDir.mkdirs();
        final boolean allDirsOK = osOutputDir.exists()
            && appDir.exists() && contentsDir.exists()
            && macOSDir.exists() && resourcesDir.exists() 
            && libDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + appDir.getAbsolutePath());
        }
        
        final File javaApplicationStub = new File(macOSDir, "JavaApplicationStub");
        copyPluginResource("macosx/JavaApplicationStub", javaApplicationStub);
        makeExecutable(javaApplicationStub);
        
        copyProjectResource(mIconsFileName, new File(resourcesDir, mIconsFileName));
        
        copyInterpolatedPluginResource("macosx/Info.plist", new File(contentsDir, "Info.plist"));
        copyInterpolatedPluginResource("macosx/PkgInfo", new File(contentsDir, "PkgInfo"));
        
        copyTransitiveArtifacts(libDir);
    }

    private void setTransitiveArtifactsAsParameterProperty() {
        final String lineSeparator = System.getProperty("line.separator");
        final StringBuilder libsAsArtifacts = new StringBuilder();
        for (final Artifact transitiveArtifact : getTransitiveArtifacts()) {
            if (transitiveArtifact.getScope().equals("compile")
                    && transitiveArtifact.getType().equals("jar")) {
                libsAsArtifacts.append("            <string>$JAVAROOT/lib/");
                libsAsArtifacts.append(transitiveArtifact.getFile().getName());
                libsAsArtifacts.append("</string>");
                libsAsArtifacts.append(lineSeparator);
            }
        }
        getParameterProperties().put("xplp.macosxclasspatharray", libsAsArtifacts.toString());
    }
}