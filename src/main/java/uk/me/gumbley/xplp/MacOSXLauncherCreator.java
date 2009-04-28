/**
 * 
 */
package uk.me.gumbley.xplp;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;


/**
 * Creates a Mac OS X launcher directory structure.
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
        if (mIconsFileName == null ||
                mIconsFileName.length() == 0) {
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
        
        final File appDir = new File(getOutputDirectory(), getApplicationName() + ".app");
        final File contentsDir = new File(appDir, "Contents");
        final File macOSDir = new File(contentsDir, "MacOS");
        final File resourcesDir = new File(contentsDir, "Resources");
        final File libDir = new File(resourcesDir, "Java/lib");
        appDir.mkdirs();
        contentsDir.mkdirs();
        macOSDir.mkdirs();
        resourcesDir.mkdirs();
        libDir.mkdirs();
        final boolean allDirsOK = appDir.exists() && contentsDir.exists() &&
            macOSDir.exists() && resourcesDir.exists() && libDir.exists();
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

    private void makeExecutable(final File nonExecutableFile) {
        getMojo().getLog().info("Making " + nonExecutableFile + " executable");
        final Commandline cl = new  Commandline( "chmod" ); 
        cl.addArguments( new  String [] { "a+x" , nonExecutableFile.getAbsolutePath()  } ); 
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

    private void setTransitiveArtifactsAsParameterProperty() {
        final String lineSeparator = System.getProperty("line.separator");
        final StringBuilder libsAsArtifacts = new StringBuilder();
        for (Artifact transitiveArtifact: getTransitiveArtifacts()) {
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
