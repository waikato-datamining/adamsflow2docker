/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Main.java
 * Copyright (C) 2020 University of Waikato, Hamilton, NZ
 */

package adams.flow.docker;

import com.github.fracpete.resourceextractor4j.IOUtils;
import com.github.fracpete.simpleargparse4j.ArgumentParser;
import com.github.fracpete.simpleargparse4j.ArgumentParserException;
import com.github.fracpete.simpleargparse4j.Namespace;
import com.github.fracpete.simpleargparse4j.Option.Type;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line application for turning ADAMS workflows into Docker images.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class Main {

  /** the alternative maven installation. */
  protected File m_MavenHome;

  /** the maven user settings to use. */
  protected File m_MavenUserSettings;

  /** the alternative java installation. */
  protected File m_JavaHome;

  /** the modules. */
  protected String m_Modules;

  /** the version to use. */
  protected String m_Version;

  /** the dependencies. */
  protected List<String> m_Dependencies;

  /** the dependency files. */
  protected List<File> m_DependencyFiles;

  /** the external jar files/dirs. */
  protected List<File> m_ExternalJars;

  /** the JVM options. */
  protected List<String> m_JVM;

  /** the flow to use. */
  protected File m_Input;

  /** the docker base image to use. */
  protected String m_DockerBaseImage;

  /** any Dockerfile instructions to add beforehand. */
  protected File m_DockerInstructions;

  /** the output directory. */
  protected File m_OutputDir;

  /** for logging. */
  protected Logger m_Logger;

  /** whether help got requested. */
  protected boolean m_HelpRequested;

  /** the generated Dockerfile. */
  protected transient File m_DockerFile;

  /**
   * Initializes the object.
   */
  public Main() {
    initialize();
  }

  /**
   * Initializes the members.
   */
  protected void initialize() {
    m_MavenHome          = null;
    m_MavenUserSettings  = null;
    m_JavaHome           = null;
    m_Modules            = null;
    m_Version            = null;
    m_Dependencies       = null;
    m_DependencyFiles    = null;
    m_ExternalJars       = null;
    m_Input              = null;
    m_DockerBaseImage    = null;
    m_DockerInstructions = null;
    m_OutputDir          = null;
    m_JVM                = null;
    m_HelpRequested      = false;
    m_DockerFile         = null;
  }

  /**
   * Returns the logger instance to use.
   *
   * @return		the logger
   */
  protected Logger getLogger() {
    if (m_Logger == null)
      m_Logger = Logger.getLogger(getClass().getName());
    return m_Logger;
  }

  /**
   * Sets the alternative maven installation to use.
   *
   * @param dir		the top-level directory (above "bin")
   * @return		itself
   */
  public Main mavenHome(File dir) {
    m_MavenHome = dir;
    return this;
  }

  /**
   * Returns the alternative maven installation to use.
   *
   * @return		the directory, null to use bundled one
   */
  public File getMavenHome() {
    return m_MavenHome;
  }

  /**
   * Sets the alternative maven user settings to use.
   *
   * @param dir		the XML file, null to use default ($HOME/.m2/settings.xml)
   * @return		itself
   */
  public Main mavenUserSettings(File dir) {
    m_MavenUserSettings = dir;
    return this;
  }

  /**
   * Returns the alternative maven user settings to use.
   *
   * @return		the file, null to use default ($HOME/.m2/settings.xml)
   */
  public File getMavenUserSettings() {
    return m_MavenUserSettings;
  }

  /**
   * Sets the alternative java installation to use.
   *
   * @param dir		the top-level directory (above "bin")
   * @return		itself
   */
  public Main javaHome(File dir) {
    m_JavaHome = dir;
    return this;
  }

  /**
   * Returns the alternative java installation to use.
   *
   * @return		the directory, null if using one that class was started with
   */
  public File getJavaHome() {
    return m_JavaHome;
  }

  /**
   * Sets the modules to use for bootstrapping.
   *
   * @param modules	the modules (comma-separated list)
   * @return		itself
   */
  public Main modules(String modules) {
    m_Modules = modules;
    return this;
  }

  /**
   * Sets the modules to use for bootstrapping.
   *
   * @param modules	the modules
   * @return		itself
   */
  public Main modules(String... modules) {
    StringBuilder	all;

    if (modules != null) {
      all = new StringBuilder();
      for (String module : modules) {
	if (all.length() > 0)
	  all.append(",");
	all.append(module);
      }
      m_Modules = all.toString();
    }
    else {
      m_Modules = null;
    }
    return this;
  }

  /**
   * Sets the modules to use for bootstrapping.
   *
   * @param modules	the modules
   * @return		itself
   */
  public Main modules(List<String> modules) {
    if (modules != null)
      modules(modules.toArray(new String[0]));
    else
      m_Modules = null;
    return this;
  }

  /**
   * Returns the modules.
   *
   * @return		the modules (comma-separated list), if not yet set
   */
  public String getModules() {
    return m_Modules;
  }

  /**
   * Sets the version of ADAMS to use.
   *
   * @param version	the version
   * @return		itself
   */
  public Main version(String version) {
    m_Version = version;
    return this;
  }

  /**
   * Returns the version of ADAMS to use.
   *
   * @return		the version
   */
  public String getVersion() {
    return m_Version;
  }

  /**
   * Sets the dependencies to use for bootstrapping.
   *
   * @param dependencies	the dependencies, can be null
   * @return		itself
   */
  public Main dependencies(List<String> dependencies) {
    m_Dependencies = dependencies;
    return this;
  }

  /**
   * Sets the dependencies to use for bootstrapping.
   *
   * @param dependencies	the dependencies, can be null
   * @return		itself
   */
  public Main dependencies(String... dependencies) {
    if (dependencies != null)
      m_Dependencies = new ArrayList<>(Arrays.asList(dependencies));
    else
      m_Dependencies = null;
    return this;
  }

  /**
   * Returns the dependencies.
   *
   * @return		the dependencies, can be null
   */
  public List<String> getDependencies() {
    return m_Dependencies;
  }

  /**
   * Sets the dependency files to use for bootstrapping (one dependency per line).
   *
   * @param files	the dependencies, can be null
   * @return		itself
   */
  public Main dependencyFiles(List<File> files) {
    m_DependencyFiles = files;
    return this;
  }

  /**
   * Sets the dependency files to use for bootstrapping (one dependency per line).
   *
   * @param files	the dependency files, can be null
   * @return		itself
   */
  public Main dependencyFiles(File... files) {
    if (files != null)
      m_DependencyFiles = new ArrayList<>(Arrays.asList(files));
    else
      m_DependencyFiles = null;
    return this;
  }

  /**
   * Returns the dependency files.
   *
   * @return		the files, can be null
   */
  public List<File> getDependencyFiles() {
    return m_DependencyFiles;
  }

  /**
   * Sets the external jar files/dirs to use.
   *
   * @param external	the files/dirs, null to unset
   * @return		itself
   */
  public Main externalJars(List<File> external) {
    m_ExternalJars = external;
    return this;
  }

  /**
   * Sets the external jar files/dirs to use.
   *
   * @param external	the files/dirs, null to unset
   * @return		itself
   */
  public Main externalJars(File... external) {
    if (external == null)
      m_ExternalJars = null;
    else
      externalJars(Arrays.asList(external));
    return this;
  }

  /**
   * Returns the currently set external jar files/dirs.
   *
   * @return		the files/dirs, null if none set
   */
  public List<File> getExternalJars() {
    return m_ExternalJars;
  }

  /**
   * Sets the workflow to convert.
   *
   * @param input	the notebook
   * @return		itself
   */
  public Main input(File input) {
    m_Input = input;
    return this;
  }

  /**
   * Returns the workflow to convert.
   *
   * @return		the notebook, null if none set
   */
  public File getInput() {
    return m_Input;
  }

  /**
   * Sets the docker base image to use.
   *
   * @param image	the base image
   * @return		itself
   */
  public Main dockerBaseImage(String image) {
    m_DockerBaseImage = image;
    return this;
  }

  /**
   * Returns the docker base image to use ("FROM ...").
   *
   * @return		the base image, null if none set
   */
  public String getDockerBaseImage() {
    return m_DockerBaseImage;
  }

  /**
   * Sets the file with instructions for the Dockerfile to generate.
   *
   * @param dir		the file
   * @return		itself
   */
  public Main dockerInstructions(File dir) {
    m_DockerInstructions = dir;
    return this;
  }

  /**
   * Returns the file with instructions for the Dockerfile to generate.
   *
   * @return		the file, null if not used
   */
  public File getDockerInstructions() {
    return m_DockerInstructions;
  }

  /**
   * Sets the output directory for the bootstrapped application.
   *
   * @param dir		the directory
   * @return		itself
   */
  public Main outputDir(File dir) {
    m_OutputDir = dir;
    return this;
  }

  /**
   * Returns the output directory for the bootstrapped application.
   *
   * @return		the directory, null if none set
   */
  public File getOutputDir() {
    return m_OutputDir;
  }

  /**
   * Sets the JVM options to use for launching the main class.
   *
   * @param options	the options, can be null
   * @return		itself
   */
  public Main jvm(List<String> options) {
    m_JVM = options;
    return this;
  }

  /**
   * Sets the JVM options to use for launching the main class.
   *
   * @param options	the options, can be null
   * @return		itself
   */
  public Main jvm(String... options) {
    if (options != null)
      m_JVM = new ArrayList<>(Arrays.asList(options));
    else
      m_JVM = null;
    return this;
  }

  /**
   * Returns the JVM options.
   *
   * @return		the options, can be null
   */
  public List<String> getJvm() {
    return m_JVM;
  }

  /**
   * Configures and returns the commandline parser.
   *
   * @return		the parser
   */
  protected ArgumentParser getParser() {
    ArgumentParser 		parser;

    parser = new ArgumentParser("Converts ADAMS workflows into Docker images.");
    parser.addOption("-m", "--maven_home")
      .required(false)
      .type(Type.EXISTING_DIR)
      .dest("maven_home")
      .help("The directory with a local Maven installation to use instead of the bundled one.");
    parser.addOption("-u", "--maven_user_settings")
      .required(false)
      .type(Type.EXISTING_FILE)
      .dest("maven_user_settings")
      .help("The file with the maven user settings to use other than $HOME/.m2/settings.xml.");
    parser.addOption("-j", "--java_home")
      .required(false)
      .type(Type.EXISTING_DIR)
      .dest("java_home")
      .help("The Java home to use for the Maven execution.");
    parser.addOption("-M", "--module")
      .required(true)
      .dest("modules")
      .help("The comma-separated list of ADAMS modules to use for the application, e.g.: adams-weka,adams-groovy,adams-excel");
    parser.addOption("-V", "--version")
      .required(true)
      .dest("version")
      .help("The version of ADAMS to use, e.g., '20.1.1' or '20.2.0-SNAPSHOT'.");
    parser.addOption("-d", "--dependency")
      .required(false)
      .multiple(true)
      .dest("dependencies")
      .metaVar("DEPENDENCY")
      .help("The additional maven dependencies to use for bootstrapping ADAMS (group:artifact:version), e.g.: nz.ac.waikato.cms.weka:kfGroovy:1.0.12");
    parser.addOption("-D", "--dependency-file")
      .required(false)
      .multiple(true)
      .type(Type.EXISTING_FILE)
      .dest("dependency_files")
      .metaVar("FILE")
      .help("The file(s) with additional maven dependencies to use for bootstrapping ADAMS (group:artifact:version), one dependency per line.");
    parser.addOption("-J", "--external-jar")
      .required(false)
      .multiple(true)
      .type(Type.EXISTING_FILE_OR_DIRECTORY)
      .dest("external_jars")
      .metaVar("JAR_OR_DIR")
      .help("The external jar or directory with jar files to also include in the application.");
    parser.addOption("-v", "--jvm")
      .required(false)
      .multiple(true)
      .dest("jvm")
      .help("The parameters to pass to the JVM to launch the workflow with.");
    parser.addOption("-i", "--input")
      .required(true)
      .type(Type.EXISTING_FILE)
      .dest("input")
      .help("The ADAMS workflow to use.");
    parser.addOption("-b", "--docker_base_image")
      .required(true)
      .dest("docker_base_image")
      .help("The docker base image to use, e.g. 'openjdk:11-jdk-slim-buster'.");
    parser.addOption("-I", "--docker_instructions")
      .required(false)
      .type(Type.EXISTING_FILE)
      .dest("docker_instructions")
      .help("File with additional docker instructions to use for generating the Dockerfile.");
    parser.addOption("-o", "--output_dir")
      .required(true)
      .type(Type.DIRECTORY)
      .dest("output_dir")
      .help("The directory to output the bootstrapped application, workflow and Dockerfile in.");

    return parser;
  }

  /**
   * Sets the parsed options.
   *
   * @param ns		the parsed options
   * @return		if successfully set
   */
  protected boolean setOptions(Namespace ns) {
    mavenHome(ns.getFile("maven_home"));
    mavenUserSettings(ns.getFile("maven_user_settings"));
    javaHome(ns.getFile("java_home"));
    modules(ns.getString("modules"));
    version(ns.getString("version"));
    dependencies(ns.getList("dependencies"));
    dependencyFiles(ns.getList("dependency_files"));
    externalJars(ns.getList("external_jars"));
    input(ns.getFile("input"));
    dockerBaseImage(ns.getString("docker_base_image"));
    dockerInstructions(ns.getFile("docker_instructions"));
    outputDir(ns.getFile("output_dir"));
    jvm(ns.getList("jvm"));
    return true;
  }

  /**
   * Returns whether help got requested when setting the options.
   *
   * @return		true if help got requested
   */
  public boolean getHelpRequested() {
    return m_HelpRequested;
  }

  /**
   * Parses the options and configures the object.
   *
   * @param options	the command-line options
   * @return		true if successfully set (or help requested)
   */
  public boolean setOptions(String[] options) {
    ArgumentParser parser;
    Namespace 		ns;

    m_HelpRequested = false;
    parser          = getParser();
    try {
      ns = parser.parseArgs(options);
    }
    catch (ArgumentParserException e) {
      parser.handleError(e);
      m_HelpRequested = parser.getHelpRequested();
      return m_HelpRequested;
    }

    return setOptions(ns);
  }

  /**
   * Generates the lib directory based on the dependencies.
   *
   * @return		null if successful, otherwise error message
   */
  protected String initLibraries() {
    adams.bootstrap.Main		main;

    main = new adams.bootstrap.Main()
      .clean(true)
      .modules(m_Modules)
      .version(m_Version)
      .dependencies(m_Dependencies)
      .externalJars(m_ExternalJars)
      .javaHome(m_JavaHome)
      .mavenHome(m_MavenHome)
      .mavenUserSettings(m_MavenUserSettings)
      .outputDir(m_OutputDir);
    return main.execute();
  }

  /**
   * Copies the flow into the output directory for docker.
   *
   * @return		null if successful, otherwise error message
   */
  protected String initFlow() {
    File 	flowFile;

    flowFile = new File(m_OutputDir + "/worker.flow");
    try {
      Files.copy(m_Input.toPath(), flowFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed to copy flow '" + m_Input + "' to: " + m_OutputDir, e);
      return "Failed to copy flow '" + m_Input + "' to: " + m_OutputDir;
    }

    return null;
  }

  /**
   * Copies the flow into the output directory for docker.
   *
   * @return		null if successful, otherwise error message
   */
  protected String initPlaceholders() {
    Properties		props;
    File		propsFile;
    FileWriter		fwriter;
    BufferedWriter	bwriter;

    props = new Properties();
    props.setProperty("CWD", "/adamsflow2docker");
    props.setProperty("TMP", "/tmp");

    propsFile = new File(m_OutputDir + "/Placeholders.props");
    fwriter   = null;
    bwriter   = null;
    try {
      fwriter = new FileWriter(propsFile);
      bwriter = new BufferedWriter(fwriter);
      props.store(bwriter, null);
    }
    catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed to store placeholders in: " + propsFile, e);
      return "Failed to store placeholders in: " + propsFile;
    }
    finally {
      IOUtils.closeQuietly(bwriter);
      IOUtils.closeQuietly(fwriter);
    }

    return null;
  }

  /**
   * Creates the Dockerfile.
   *
   * @return		null if successful, otherwise error message
   */
  protected String createDockerfile() {
    List<String>	content;
    List<String>	cmd;
    StringBuilder	cmdLine;
    int			i;

    content      = new ArrayList<>();
    m_DockerFile = new File(m_OutputDir.getAbsolutePath() + "/Dockerfile");

    content.add("FROM " + m_DockerBaseImage);
    if ((m_DockerInstructions != null) && (m_DockerInstructions.exists()) && !m_DockerInstructions.isDirectory()) {
      try {
        content.addAll(Files.readAllLines(m_DockerInstructions.toPath()));
      }
      catch (Exception e) {
        getLogger().log(Level.SEVERE, "Failed to read docker instructions from: " + m_DockerInstructions, e);
        return "Failed to read docker instructions from: " + m_DockerInstructions;
      }
    }

    content.add("COPY \"target/lib/*\" /adamsflow2docker/lib/");
    content.add("COPY Placeholders.props /adamsflow2docker/Placeholders.props");
    content.add("COPY worker.flow /adamsflow2docker/worker.flow");

    cmd = new ArrayList<>();
    cmd.add("java");
    cmd.add("-cp");
    cmd.add("/adamsflow2docker/lib/*");
    if (m_JVM != null)
      cmd.addAll(m_JVM);
    cmd.add("adams.flow.FlowRunner");
    cmd.add("-headless");
    cmd.add("true");
    cmd.add("-non-interactive");
    cmd.add("true");
    cmd.add("-clean-up");
    cmd.add("true");
    cmd.add("-home");
    cmd.add("/adamsflow2docker");
    cmd.add("-input");
    cmd.add("/adamsflow2docker/worker.flow");

    cmdLine = new StringBuilder();
    cmdLine.append("CMD [");
    for (i = 0; i < cmd.size(); i++) {
      if (i > 0)
        cmdLine.append(", ");
      cmdLine.append("\"");
      cmdLine.append(cmd.get(i));
      cmdLine.append("\"");
    }
    cmdLine.append("]");
    content.add(cmdLine.toString());

    try {
      Files.write(m_DockerFile.toPath(), content, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed to write " + m_DockerFile, e);
      return "Failed to write " + m_DockerFile;
    }

    return null;
  }

  /**
   * Performs the Docker image generation.
   *
   * @return		null if successful, otherwise error message
   */
  protected String doExecute() {
    String 	result;

    // generate lib directory with bootstrapp
    if ((result = initLibraries()) != null)
      return result;

    // copies the flow
    if ((result = initFlow()) != null)
      return result;

    // creates the placeholders to use in the docker image
    if ((result = initPlaceholders()) != null)
      return result;

    // generate Dockerfile
    if ((result = createDockerfile()) != null)
      return result;

    // output instructions for compiling docker image
    System.out.println();
    System.out.println("You can compile the Docker image now as follows:");
    System.out.println("cd " + m_OutputDir);
    System.out.println("[sudo] docker build -t <imagename> .");
    System.out.println();

    return null;
  }

  /**
   * Performs the bootstrapping.
   *
   * @return		null if successful, otherwise error message
   */
  public String execute() {
    String		result;

    result = doExecute();
    if (result != null)
      getLogger().severe(result);

    return result;
  }

  /**
   * Executes the bootstrapping with the specified command-line arguments.
   *
   * @param args	the options to use
   */
  public static void main(String[] args) {
    Main main = new Main();

    if (!main.setOptions(args)) {
      System.err.println("Failed to parse options!");
      System.exit(1);
    }
    else if (main.getHelpRequested()) {
      System.exit(0);
    }

    String result = main.execute();
    if (result != null) {
      System.err.println("Failed to perform Dockerfile generation:\n" + result);
      System.exit(2);
    }
  }
}
