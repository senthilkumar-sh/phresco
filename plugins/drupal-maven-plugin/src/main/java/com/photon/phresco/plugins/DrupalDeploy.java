package com.photon.phresco.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.framework.PhrescoFrameworkFactory;
import com.photon.phresco.framework.api.ProjectAdministrator;
import com.photon.phresco.model.SettingsInfo;
import com.photon.phresco.util.ArchiveUtil;
import com.photon.phresco.util.ArchiveUtil.ArchiveType;
import com.photon.phresco.util.Constants;
import com.photon.phresco.util.PluginConstants;
import com.photon.phresco.util.PluginUtils;

/**
 * Goal which deploys the Drupal project
 * 
 * @goal deploy
 * 
 */
public class DrupalDeploy extends AbstractMojo implements PluginConstants {

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * @parameter expression="${project.basedir}" required="true"
	 * @readonly
	 */
	protected File baseDir;

	/**
	 * Build file name to deploy
	 * 
	 * @parameter expression="${buildName}" required="true"
	 */
	protected String buildName;

	/**
	 * @parameter expression="${environmentName}" required="true"
	 */
	protected String environmentName;
	
	/**
	 * @parameter expression="${importSql}" required="true"
	 */
	protected boolean importSql;
	
	private File binariesDir;
	private File buildDir;
	private File buildFile;
	private File tempDir;
	private Map<String, String> dbDriverMap = new HashMap<String, String>(8);

	public void execute() throws MojoExecutionException {
		init();
		createDb();
		packDrupal();
		extractBuild();
		deploy();
		cleanUp();
	}

	private void init() throws MojoExecutionException  {
		try {
			if (StringUtils.isEmpty(buildName) || StringUtils.isEmpty(environmentName)) {
				callUsage();
			}
			buildDir = new File(baseDir.getPath() + BUILD_DIRECTORY);
			buildFile = new File(buildDir.getPath() + File.separator + buildName);
			binariesDir = new File(baseDir.getPath() + BINARIES_DIR);
			
			String context = "";
			List<SettingsInfo> settingsInfos = getSettingsInfo(Constants.SETTINGS_TEMPLATE_SERVER);
			for (SettingsInfo serverDetails : settingsInfos) {
				context = serverDetails.getPropertyInfo(Constants.SERVER_CONTEXT).getValue();
				break;
			}
			tempDir = new File(buildDir.getPath() + TEMP_DIR + File.separator + context);
			tempDir.mkdirs();
		} catch (Exception e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}


	private void callUsage() throws MojoExecutionException {
		getLog().error("Invalid usage.");
		getLog().info("Usage of Deploy Goal");
		getLog().info(
				"mvn drupal:deploy -DbuildName=\"Name of the build\""
				+ " -DenvironmentName=\"Multivalued evnironment names\""
				+ " -DimportSql=\"Does the deployment needs to import sql(TRUE/FALSE?)\"");
		throw new MojoExecutionException(
				"Invalid Usage. Please see the Usage of Deploy Goal");
	}
	
	private void createDb() throws MojoExecutionException {
		PluginUtils util = new PluginUtils();
		try {
			if (importSql) {
				List<SettingsInfo> settingsInfos = getSettingsInfo(Constants.SETTINGS_TEMPLATE_DB);
				for (SettingsInfo databaseDetails : settingsInfos) {
					util.executeSql(databaseDetails,baseDir, DRUPAL_SQL_DIR, DRUPAL_SQL_FILE);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	
	private void packDrupal() throws MojoExecutionException {
		try {
			//fetching drupal binary from repo
			StringBuilder sb = new StringBuilder();
			sb.append(MVN_CMD);
			sb.append(STR_SPACE);
			sb.append(MVN_PHASE_INITIALIZE);

			Commandline cl = new Commandline(sb.toString());
			Process process = cl.execute();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
			}
			
			//packing drupal binary to build
			File drupalBinary = null;
			File[] listFiles = binariesDir.listFiles();
			for (File file : listFiles) {
				if (file.isDirectory()) {
					drupalBinary = new File(binariesDir + "/drupal");
					file.renameTo(drupalBinary);
				}
			}
			if (!drupalBinary.exists()) {
				throw new MojoExecutionException("Drupal binary not found");
			}
			if (drupalBinary != null) {
				FileUtils.copyDirectoryStructure(drupalBinary, tempDir);
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	
	private void extractBuild() throws MojoExecutionException {
		try {
			ArchiveUtil.extractArchive(buildFile.getPath(), tempDir.getPath() + "/sites", ArchiveType.ZIP);
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getErrorMessage(), e);
		}
	}

	private void deploy() throws MojoExecutionException {
		String deployLocation = "";
		try {
			List<SettingsInfo> settingsInfos = getSettingsInfo(Constants.SETTINGS_TEMPLATE_SERVER);
			for (SettingsInfo serverDetails : settingsInfos) {
				deployLocation = serverDetails.getPropertyInfo(Constants.SERVER_DEPLOY_DIR).getValue();
				break;
			}
			File deployDir = new File(deployLocation);
			FileUtils.mkdir(deployDir.getPath().trim());
			getLog().info("Project is deploying.........");
			FileUtils.copyDirectoryStructure(tempDir.getParentFile(), deployDir);
			getLog().info("Project is deployed successfully");
		} catch (Exception e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	
	private List<SettingsInfo> getSettingsInfo(String configType) throws PhrescoException {
		ProjectAdministrator projAdmin = PhrescoFrameworkFactory.getProjectAdministrator();
		return projAdmin.getSettingsInfos(configType, baseDir.getName(), environmentName);
	}

	private void cleanUp() throws MojoExecutionException {
		try {
			FileUtils.deleteDirectory(tempDir.getParentFile());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}