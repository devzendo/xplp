/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

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

    public LauncherCreator(final AbstractMojo mojo,
            final File outputDirectory,
            final String mainClassName,
            final String applicationName,
            final String libraryDirectory,
            final Set<Artifact> transitiveArtifacts,
            final Set<File> resourceDirectories) {
                mMojo = mojo;
                mOutputDirectory = outputDirectory;
                mMainClassName = mainClassName;
                mApplicationName = applicationName;
                mLibraryDirectory = libraryDirectory;
                mTransitiveArtifacts = transitiveArtifacts;
                mResourceDirectories = resourceDirectories;
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

    public abstract void createLauncher() throws IOException;

    protected void copyPluginResource(final String resourceName, final File destinationFile) throws IOException {
        final InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
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
        for (File resourceDir : mResourceDirectories) {
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
    /**
     * Copy a file from its source to a destination directory.
     * @param sourceFile the source file
     * @param destinationDirectory the destination directory, which
     * must exist
     * @throws IOException on copy failure
     */
    protected void copyFile(final File sourceFile, final File destinationDirectory) throws IOException {
        final InputStream inputStream = new FileInputStream(sourceFile);
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
        mMojo.getLog().info("Created " +
            destinationDirectory.getAbsoluteFile() +
            File.separatorChar + sourceFile.getName() +
            " [" + bytesCopied + " byte(s) copied]");
    }

    private long copyStream(final String inName, final String outName,
            final InputStream inputStream, OutputStream outputStream) 
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
     */
    protected void copyTransitiveArtifacts(final File destinationDirectory) throws IOException {
        final Set<Artifact> transitiveArtifacts = getTransitiveArtifacts();
        for (Artifact artifact : transitiveArtifacts) {
            if (artifact.getScope().equals("compile") && artifact.getType().equals("jar")) {
                copyFile(artifact.getFile(), destinationDirectory);
            } else {
                getMojo().getLog().info("Not copying transitive artifact " + artifact + " since it is not a compile-scoped jar");
            }
        }
    }
}
