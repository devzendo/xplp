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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;

/**
 * A LauncherCreator is given all the parameters, extracted from
 * the plugin configuration, and creates an appropriate
 * launcher filesystem structure under the output directory.
 * 
 * @author matt
 *
 */
public abstract class LauncherCreator {
    private final AbstractMojo mMojo;
    private final File mOutputDirectory;
    private final String mMainClassName;
    private final String mApplicationName;
    private final String mLibraryDirectory;
    private final Set<Artifact> mTransitiveArtifacts;
    private final Set<File> mResourceDirectories;
    private final Properties mParameterProperties;
    private final String[] mSystemProperties;
    private final String[] mVmArguments;

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
     */
    public LauncherCreator(final AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories,
            final Properties parameterProperties, 
            final String[] systemProperties, 
            final String[] vmArguments) {
                mMojo = mojo;
                mOutputDirectory = outputDirectory;
                mMainClassName = mainClassName;
                mApplicationName = applicationName;
                mLibraryDirectory = libraryDirectory;
                mTransitiveArtifacts = transitiveArtifacts;
                mResourceDirectories = resourceDirectories;
                mParameterProperties = parameterProperties;
                mSystemProperties = systemProperties;
                mVmArguments = vmArguments;
    }

    /**
     * @return the mojo that Austin Powers stole
     */
    protected final AbstractMojo getMojo() {
        return mMojo;
    }

    /**
     * @return the transitiveArtifacts
     */
    protected final Set<Artifact> getTransitiveArtifacts() {
        return mTransitiveArtifacts;
    }

    /**
     * @return the outputDirectory
     */
    protected final File getOutputDirectory() {
        return mOutputDirectory;
    }

    /**
     * @return the mainClassName
     */
    protected final String getMainClassName() {
        return mMainClassName;
    }

    /**
     * @return the applicationName
     */
    protected final String getApplicationName() {
        return mApplicationName;
    }

    /**
     * @return the libraryDirectory
     */
    protected final String getLibraryDirectory() {
        return mLibraryDirectory;
    }

    /**
     * @return the resourceDirectories
     */
    protected final Set<File> getResourceDirectories() {
        return mResourceDirectories;
    }

    /**
     * @return the parameterProperties
     */
    protected final Properties getParameterProperties() {
        return mParameterProperties;
    }

    /**
     * @return the array of system properties name=value pair strings
     */
    protected final String[] getSystemProperties() {
        return mSystemProperties;
    }

    /**
     * @return the array of VM arguments
     */
    protected final String[] getVmArguments() {
        return mVmArguments;
    }

    /**
     * Create a launcher specific to a given platform.
     * @throws IOException on creation failure
     */
    public abstract void createLauncher() throws IOException;

    /**
     * Copy a resource from the plugin's resources to a file.
     * @param resourceName the name of the resource
     * @param destinationFile the file to write it to
     * @throws IOException on write failure
     */
    protected void copyPluginResource(final String resourceName, final File destinationFile) throws IOException {
        final InputStream resourceAsStream = getPluginResourceAsStream(resourceName);
        if (resourceAsStream == null) {
            final String message = "Could not open resource " + resourceName;
            mMojo.getLog().warn(message);
            throw new IOException(message);
        }
        
        final OutputStream outputStream = createFileOutputStream(destinationFile);
        final long bytesCopied = copyStream(resourceName, destinationFile.getAbsolutePath(),
            resourceAsStream, outputStream);
        mMojo.getLog().info("Created " + destinationFile.getAbsolutePath() + " [" + bytesCopied + " byte(s)]");
    }

    /**
     * Copy a resource from the user project to a file
     * @param resourceName the name of the resource in the user project
     * @param destinationFile the file to write it to
     * @throws IOException on write failure
     */
    protected void copyProjectResource(final String resourceName, final File destinationFile) throws IOException {
        final InputStream resourceAsStream = getProjectResourceAsStream(resourceName);
        final OutputStream outputStream = createFileOutputStream(destinationFile);
        final long bytesCopied = copyStream(resourceName, destinationFile.getAbsolutePath(),
            resourceAsStream, outputStream);
        mMojo.getLog().info("Created " + destinationFile.getAbsolutePath() + " [" + bytesCopied + " byte(s)]");
    }

    private OutputStream createFileOutputStream(final File destinationFile) throws IOException {
        try {
            return new FileOutputStream(destinationFile);
        } catch (final FileNotFoundException e) {
            final String message = "Could not create destination file " + destinationFile.getAbsolutePath() + ": " + e.getMessage();
            mMojo.getLog().warn(message);
            throw new IOException(message);
        }
    }

    private InputStream getProjectResourceAsStream(final String resourceName) throws IOException {
        for (final File resourceDir : mResourceDirectories) {
            try {
                final InputStream resourceAsStream = new FileInputStream(new File(resourceDir, resourceName));
                mMojo.getLog().debug("Located resource " + resourceName + " in directory " + resourceDir.getAbsolutePath());
                return resourceAsStream;
            } catch (final FileNotFoundException e) {
                mMojo.getLog().debug("Resource " + resourceName + " not found in " + resourceDir.getAbsolutePath());
            }
        }
        final String message = "Could not open resource " + resourceName;
        mMojo.getLog().warn(message);
        throw new IOException(message);
    }
    
    private InputStream getPluginResourceAsStream(final String resourceName) {
        final InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        return resourceAsStream;
    }
    
    /**
     * Copy a file from its source to a destination directory.
     * @param sourceFile the source file
     * @param destinationDirectory the destination directory, which
     * must exist
     * @throws IOException on copy failure
     */
    protected void copyFile(final File sourceFile, final File destinationDirectory) throws IOException {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(sourceFile);
        } catch (final IOException ioe) {
            final String warning = "Could not open source file '" + sourceFile + "': " + ioe.getMessage();
            mMojo.getLog().warn(warning);
            throw new IOException(warning);
        }
        final File destinationFile = new File(destinationDirectory, sourceFile.getName());
        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(destinationFile);
        } catch (final FileNotFoundException e) {
            final String message = "Could not create destination file " + destinationFile.getAbsolutePath() + ": " + e.getMessage();
            mMojo.getLog().warn(message);
            throw new IOException(message);
        }
        final long bytesCopied = copyStream(sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath(),
            inputStream, outputStream);
        mMojo.getLog().info("Created "
            + destinationDirectory.getAbsoluteFile()
            + File.separatorChar + sourceFile.getName()
            + " [" + bytesCopied + " byte(s) copied]");
    }

    private long copyStream(final String inName, final String outName,
            final InputStream inputStream, final OutputStream outputStream) 
    throws IOException {
        final int bufsize = 16384;
        final byte[] buf = new byte[bufsize];
        long totalRead = 0;
        int nRead;
        try {
            while ((nRead = inputStream.read(buf, 0, bufsize)) != -1) {
                outputStream.write(buf, 0, nRead);
                totalRead += nRead;
            }
            return totalRead;
        } catch (final IOException e) {
            final String message = "Could not copy " + inName + " to "
            + outName + ": " + e.getMessage();
            mMojo.getLog().warn(message);
            throw new IOException(message);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException ioe) {
            }
            try {
                outputStream.close();
            } catch (final IOException ioe) {
            }
        }
    }

    /**
     * Copy all compile-scoped jar-typed transitive artifacts into
     * a destination directory.
     * @param destinationDirectory the destination directory, which
     * must exist
     * @throws IOException on copy failure
     */
    protected void copyTransitiveArtifacts(final File destinationDirectory) throws IOException {
        getMojo().getLog().info("Copying transitive artifacts");
        final Set<Artifact> transitiveArtifacts = getTransitiveArtifacts();
        getMojo().getLog().info("There are " + transitiveArtifacts.size() + " transitive artifacts");
        for (final Artifact artifact : transitiveArtifacts) {
            if (artifact.getScope().equals("compile") && artifact.getType().equals("jar")) {
                getMojo().getLog().info("Copying transitive artifact " + artifact);
                final File artifactFile = artifact.getFile();
                if (artifactFile.exists() && artifactFile.isFile()) {
                    copyFile(artifactFile, destinationDirectory);
                } else {
                    getMojo().getLog().warn("Not copying transitive artifact " + artifact + " since it either doesn't exist or is not a file");
                    getMojo().getLog().info("(Perhaps you're running this from an IDE, and this artifact is resolved as a project in your");
                    getMojo().getLog().info(" workspace and therefore on your classpath by the IDE?)");
                }
            } else {
                getMojo().getLog().info("Not copying transitive artifact " + artifact + " since it is not a compile-scoped jar");
            }
        }
    }

    /**
     * Copy a user project resource to a file, interpolating with the configuration properties
     * @param resourceName the name of the resource in the user project
     * @param outputFile the file to write it to
     * @throws IOException on write failure
     */
    protected void copyInterpolatedProjectResource(final String resourceName, final File outputFile) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(getProjectResourceAsStream(resourceName)));
        copyInterpolatedResource(resourceName, outputFile, br);
    }
    
    /**
     * Copy a plugin resource to a file, interpolating with the configuration properties
     * @param resourceName the name of the plugin resource
     * @param outputFile the file to write it to
     * @throws IOException on write failure
     */
    protected void copyInterpolatedPluginResource(final String resourceName, final File outputFile) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(getPluginResourceAsStream(resourceName)));
        copyInterpolatedResource(resourceName, outputFile, br);
    }

    private void copyInterpolatedResource(
            final String resourceName,
            final File outputFile,
            final BufferedReader br) throws IOException {
        long bytesCopied = 0;
        final PropertiesInterpolator interpolator = new PropertiesInterpolator(getParameterProperties());
        try {
            final String lineSeparator = System.getProperty("line.separator");
            final FileWriter fw = createFileWriter(outputFile);
            try {
                while (true) {
                    final String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    try {
                        final String outLine = interpolator.interpolate(line);
                        fw.write(outLine);
                        bytesCopied += outLine.length();
                        if (!outLine.endsWith(lineSeparator)) {
                            fw.write(lineSeparator);
                            bytesCopied += lineSeparator.length();
                        }
                    } catch (final IllegalStateException e) {
                        final String message = "Cannot interpolate whilst processing " + resourceName + ": " + e.getMessage();
                        getMojo().getLog().warn(message);
                        throw new IOException(message);
                    }
                }
            } finally {
                try {
                    fw.close();
                } catch (final IOException e) {
                    // nothing
                }
            }
        } finally {
            try {
                br.close();
            } catch (final IOException e) {
                // nothing
            }
        }
        getMojo().getLog().info("Created "
            + outputFile.getAbsolutePath() 
            + " [" + bytesCopied + " byte(s) copied]");
        
    }

    private FileWriter createFileWriter(final File outputFile) throws IOException {
        try {
            return new FileWriter(outputFile);
        } catch (final IOException e) {
            final String message = "Could not create destination file "
                + outputFile.getAbsolutePath() + ": " + e.getMessage();
            mMojo.getLog().warn(message);
            throw new IOException(message);
        }
    }

    /**
     * Make a file executable, in the terms of the specific platform.
     * @param nonExecutableFile some file that may not currently be executable
     */
    protected void makeExecutable(final File nonExecutableFile) {
        getMojo().getLog().info("Making " + nonExecutableFile + " executable");
        final Commandline cl = new  Commandline("chmod");
        cl.addArguments(new String [] {"a+x" , nonExecutableFile.getAbsolutePath()}); 
        try {
            final StringStreamConsumer output = new StringStreamConsumer();
            final StringStreamConsumer error = new StringStreamConsumer(); 
            final int returnValue = CommandLineUtils.executeCommandLine(cl, output, error);
            if (returnValue != 0) {
                getMojo().getLog().warn("chmod exit code is " + returnValue);
                getMojo().getLog().warn("chmod output: " + output.getOutput());
                getMojo().getLog().warn("chmod error output: " + error.getOutput());
            }
        } catch (final CommandLineException e) {
            getMojo().getLog().warn("Couldn't run chmod: " + e.getMessage());
        }
    }
}
