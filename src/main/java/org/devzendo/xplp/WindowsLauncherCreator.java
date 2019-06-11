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
 * Creates a Windows launcher directory structure.
 * @author matt
 *
 */
public class WindowsLauncherCreator extends LauncherCreator {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String MSVCR71_DLL = "msvcr71.dll";
    private final String mLauncherType;
    private final String[] mJanelCustomLines;
    private final String mJanelVersion;
    private final String mJanelBits;
    private final String mJanelDirectory;

    /**
     * @param mojo the parent mojo class
     * @param outputDirectory where to create the .app structure 
     * @param mainClassName the main class
     * @param applicationName the name of the application
     * @param libraryDirectory where the libraries are stored
     * @param transitiveArtifacts the set of transitive artifact dependencies
     * @param resourceDirectories the project's resource directories
     * @param parameterProperties the plugin configuration parameters, as properties
     * @param systemProperties an array of name=value system properties
     * @param vmArguments an array of arguments to the VM
     * @param narClassifierTypes an array of NAR classifier:types
     * @param launcherType the launcher type, Console or GUI.
     * @param janelVersion the version of Janel, 3.0 or 4.2
     * @param janelBits 32 or 64 bit Janel 4.2
     * @param janelCustomLines an array of extra lines to be added to the launcher file
     * @param janelDirectory root or bin - where to put the binaries, and do we need to relativise the library directory
     */
    public WindowsLauncherCreator(final AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories,
            final Properties parameterProperties,
            final String[] systemProperties, 
            final String[] vmArguments, 
            final String[] narClassifierTypes,
            final String launcherType,
            final String janelVersion,
            final String janelBits,
            final String[] janelCustomLines,
            final String janelDirectory) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories,
            parameterProperties, systemProperties, vmArguments,
            narClassifierTypes);
        mLauncherType = launcherType;
        mJanelCustomLines = janelCustomLines;
        mJanelVersion = janelVersion;
        mJanelBits = janelBits;
        mJanelDirectory = janelDirectory;
    }
    
    private void validate() {
        if (mLauncherType == null || mLauncherType.length() == 0) {
            final String message = "No launcherType specified - this is mandatory for Windows";
            getMojo().getLog().warn(message);
            throw new IllegalStateException(message);
        }
        if (!(mLauncherType.equals("Console") || mLauncherType.equals("GUI"))) {
            final String message = "launcherType must be either 'Console' or 'GUI' (GUI is the default if not specified)";
            getMojo().getLog().warn(message);
            throw new IllegalStateException(message);
        }
        if (mJanelVersion.equals("3.0") || mJanelVersion.equals("4.2")) {
            getMojo().getLog().info("Janel version:               " + mJanelVersion);
        } else {
            throw new IllegalStateException("Janel version must be 3.0 or 4.2");
        }
        if (mJanelBits.equals("32") || mJanelBits.equals("64")) {
            getMojo().getLog().info("Janel bits:                  " + mJanelBits);
        } else {
            throw new IllegalStateException("Janel bits must be 32 or 64");
        }
        if (mJanelDirectory.equals("root") || mJanelDirectory.equals("bin")) {
            getMojo().getLog().info("Janel bin/dll/lap directory: " + mJanelDirectory);
        } else {
            throw new IllegalStateException("Janel directory must be root or bin");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        validate();
        getParameterProperties().put("xplp.windowssystemproperties", systemPropertiesAsJanelLines(getSystemProperties()));
        getParameterProperties().put("xplp.windowsvmarguments", stringsToSeparatedJanelLines(getVmArguments()));
        getParameterProperties().put("xplp.janelcustomlines", stringsToSeparatedJanelLines(mJanelCustomLines));

        getMojo().getLog().info("Janel .EXE type:             " + mLauncherType);

        final boolean usingBinForBinaries = mJanelDirectory.equals("bin"); // could be 'root' instead
        if (usingBinForBinaries) {
            // Relativise the xplp.librarydirectory to the bin directory
            getParameterProperties().put("xplp.librarydirectory", "..\\" + getLibraryDirectory());
        }
        // .. else just use the xplp.librarydirectory as-is, it's relative to the root

        final File osOutputDir = new File(getOutputDirectory(), "windows");
        final File libDir = new File(osOutputDir, "lib");
        final File binDir = usingBinForBinaries ? new File(osOutputDir, "bin") : osOutputDir;
        osOutputDir.mkdirs();
        libDir.mkdirs();
        binDir.mkdirs(); // Will return false if !usingBinForBinaries, since it == osOutputDir which already exists. This is fine.
        final boolean allDirsOK = osOutputDir.exists() && libDir.exists() && binDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + getOutputDirectory().getAbsolutePath());
        }
        
        final File outputJanelEXE = new File(binDir, getApplicationName() + ".exe");
        final String janelEXEResource = "windows/" + janelExecutableName();
        copyPluginResource(janelEXEResource, outputJanelEXE);
        // TODO icon munging in the launcher .EXE
        copyPluginResource("windows/" + MSVCR71_DLL, new File(binDir, MSVCR71_DLL));
        
        copyInterpolatedPluginResource("windows/launcher.lap", new File(binDir, getApplicationName() + ".lap"));

        copyTransitiveArtifacts(libDir);
    }

    private String janelExecutableName() {
        final String consoleOrWindows = mLauncherType.equals("Console") ? "Console" : "Windows";
        if (mJanelVersion.equals("3.0")) {
            // It's the original 3.0 version
            final String originalDir = "3.0.2";
            return originalDir + "/Janel" + consoleOrWindows + ".exe";
        } else {
            // It's the enhanced 4.2 version
            final String enhancedDir = "4.2.0-98";
            return enhancedDir + "/Janel" + consoleOrWindows + mJanelBits + ".exe";
        }
    }

    private String stringsToSeparatedJanelLines(final String[] strings) {
        final StringBuilder stringLines = new StringBuilder();
        if (strings.length > 0) {
            for (final String string : strings) {
                stringLines.append(string);
                stringLines.append(LINE_SEPARATOR);
            }
        }
        return stringLines.toString();
    }

    private String systemPropertiesAsJanelLines(final String[] systemProperties) {
        final StringBuilder sysPropLines = new StringBuilder();
        if (systemProperties.length > 0) {
            for (final String sysProp : systemProperties) {
                sysPropLines.append("-D");
                sysPropLines.append(sysProp);
                sysPropLines.append(LINE_SEPARATOR);
            }
        }
        return sysPropLines.toString();
    }
}
