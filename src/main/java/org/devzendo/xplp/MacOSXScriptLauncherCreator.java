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
 * Create a Mc OS X launcher directory structure.
 * @author matt
 *
 */
public class MacOSXScriptLauncherCreator extends UnixScriptLauncherCreator {
    private final String mLauncherType;
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
     * @param launcherType the launcher type, Console or GUI
     */
    public MacOSXScriptLauncherCreator(final AbstractMojo mojo,
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
            final String launcherType) {
        super(mojo, outputDirectory, "macosx", mainClassName,
            applicationName, libraryDirectory,
            transitiveArtifacts, resourceDirectories, parameterProperties,
            systemProperties, vmArguments, narClassifierTypes);
        mLauncherType = launcherType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createLauncher() throws IOException {
        getMojo().getLog().info("Launcher type:     " + mLauncherType);
        super.createLauncher();
    }
}
