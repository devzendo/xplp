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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

/**
 * Create a Linux launcher directory structure.
 * @author matt
 *
 */
public class LinuxLauncherCreator extends LauncherCreator {
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
     */
    public LinuxLauncherCreator(final AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories,
            final Properties parameterProperties, 
            final String[] systemProperties, 
            final String[] vmArguments,
            final String[] narClassifierTypes) {
        super(mojo, outputDirectory, mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories, parameterProperties,
            systemProperties, vmArguments, narClassifierTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        final File osOutputDir = new File(getOutputDirectory(), "linux");
        final File binDir = new File(osOutputDir, "bin");
        final File libDir = new File(osOutputDir, "lib");
        osOutputDir.mkdirs();
        binDir.mkdirs();
        libDir.mkdirs();
        final boolean allDirsOK = osOutputDir.exists() 
            && binDir.exists() && libDir.exists();
        if (!allDirsOK) {
            throw new IOException("Could not create required directories under " + getOutputDirectory().getAbsolutePath());
        }
        
        final List<String> jvmArgs = new ArrayList<String>();
        jvmArgs.addAll(systemPropertiesAsJVMArgs(getSystemProperties()));
        jvmArgs.addAll(vmArgumentsAsJVMArgs(getVmArguments()));
        final StringBuilder jvmArgsString = new StringBuilder();
        for (final String jvmArg : jvmArgs) {
            jvmArgsString.append(jvmArg);
            jvmArgsString.append(' '); // a space at the end is needed
        }
        getParameterProperties().put("xplp.linuxjvmargs", jvmArgsString.toString());

        final File outputRunScript = new File(binDir, getApplicationName());
        copyInterpolatedPluginResource("linux/launcher.sh", outputRunScript);
        makeExecutable(outputRunScript);
        
        copyTransitiveArtifacts(libDir);
    }

    private List<String> vmArgumentsAsJVMArgs(final String[] vmArguments) {
        return Arrays.asList(vmArguments);
    }

    private List<String> systemPropertiesAsJVMArgs(final String[] systemProperties) {
        final List<String> addDList = new ArrayList<String>();
        for (final String sysProp : systemProperties) {
            addDList.add("-D" + sysProp);
        }
        return addDList;
    }
}
