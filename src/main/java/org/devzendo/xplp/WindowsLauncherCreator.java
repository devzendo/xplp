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
    private final String mJanelType;
    private final String[] mJanelCustomLines;

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
     * @param janelType the launcher type, Console or GUI.
     * @param janelCustomLines an array of extra lines to be added to the launcher file
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
            final String janelType,
            final String[] janelCustomLines) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories,
            parameterProperties, systemProperties, vmArguments);
        mJanelType = janelType;
        mJanelCustomLines = janelCustomLines;
    }
    
    private void validate() {
        if (mJanelType == null || mJanelType.length() == 0) {
            final String message = "No janelType specified - this is mandatory for Windows";
            getMojo().getLog().warn(message);
            throw new IllegalStateException(message);
        }
        if (!(mJanelType.equals("Console") || mJanelType.equals("GUI"))) {
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
        getParameterProperties().put("xplp.windowssystemproperties", systemPropertiesAsJanelLines(getSystemProperties()));
        getParameterProperties().put("xplp.windowsvmarguments", stringsToSeparatedJanelLines(getVmArguments()));
        getParameterProperties().put("xplp.janelcustomlines", stringsToSeparatedJanelLines(mJanelCustomLines));

        getMojo().getLog().info("Janel .EXE type:   " + mJanelType);
        
        final File osOutputDir = new File(getOutputDirectory(), "windows");
        final File libDir = new File(osOutputDir, "lib");
        osOutputDir.mkdirs();
        libDir.mkdirs();
        final boolean allDirsOK = osOutputDir.exists() && libDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + getOutputDirectory().getAbsolutePath());
        }
        
        final File outputJanelEXE = new File(osOutputDir, getApplicationName() + ".exe");
        final String janelEXEResource = "windows/" + (mJanelType.equals("Console") ? "JanelConsole.exe" : "JanelWindows.exe");
        copyPluginResource(janelEXEResource, outputJanelEXE);
        // TODO icon munging in the launcher .EXE
        copyPluginResource("windows/" + MSVCR71_DLL, new File(osOutputDir, MSVCR71_DLL));
        
        copyInterpolatedPluginResource("windows/launcher.lap", new File(osOutputDir, getApplicationName() + ".lap"));

        copyTransitiveArtifacts(libDir);
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
