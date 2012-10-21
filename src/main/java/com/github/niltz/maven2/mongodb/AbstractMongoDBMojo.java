/**
 * Copyright (c) 2012 Jeff Sawatzky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.github.niltz.maven2.mongodb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

/**
 * Abstract mojo that all mongodb related mojos inherit from.
 */
public abstract class AbstractMongoDBMojo extends AbstractMojo {

	private static final String SCRIPT_EXTENSION = ".js";

	/**
	 * The directory that contains the scripts, looks for a subfolder named
	 * after the database, and either a create/populate/update subfolder based
	 * on the operation
	 * 
	 * @parameter default-value="${basedir}/src/main/mongodb"
	 */
	private File scriptsDirectory;

	/**
	 * Whether or not to execute the scripts against the database.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean executeScripts;

	/**
	 * The final name of the file, which will contain a suffix based on the
	 * operation
	 * 
	 * @parameter default-value="${project.build.finalName}"
	 * @readonly
	 */
	private String outputName;

	/**
	 * The output folder
	 * 
	 * @parameter default-value="${project.build.directory}/mongodb"
	 * @readonly
	 */
	private String outputDirectory;

	/**
	 * The database settings
	 * 
	 * @parameter
	 * @reqiured
	 */
	private DatabaseSettings[] databaseSettings;

	/**
	 * @return the databaseSettings
	 */
	DatabaseSettings[] getDatabaseSettings() {
		return databaseSettings;
	}

	/**
	 * The {@link Settings} object.
	 * 
	 * @parameter default-value="${settings}"
	 * @required
	 * @readonly
	 */
	private Settings settings;

	/**
	 * Child mojos need to implement this.
	 * 
	 * @throws MojoExecutionException
	 *             on error
	 * @throws MojoFailureException
	 *             on error
	 */
	public abstract void executeInternal() throws MojoExecutionException, MojoFailureException;

	/**
	 * {@inheritDoc}
	 */
	public final void execute() throws MojoExecutionException, MojoFailureException {
		checkDbSettings(databaseSettings);
		executeInternal();
	}

	/**
	 * Checks the given database connection settings.
	 * 
	 * @param dbSettings
	 *            the settings to check
	 * @param name
	 *            the name of the settings
	 * @throws MojoExecutionException
	 *             on error
	 * @throws MojoFailureException
	 *             on error
	 */
	private void checkDbSettings(DatabaseSettings[] databaseSettings) throws MojoExecutionException,
			MojoFailureException {

		for (int i = 0; i < this.databaseSettings.length; i++) {
			DatabaseSettings dbSettings = this.databaseSettings[i];
			ConnectionSettings connectionSettings = dbSettings.getConnectionSettings();

			if (!StringUtils.isEmpty(connectionSettings.getServerId())) {
				Server server = settings.getServer(connectionSettings.getServerId());
				if (server == null) {
					throw new MojoFailureException("[connectionSettings] Server ID: "
							+ connectionSettings.getServerId() + " not found!");
				}

			} else if (StringUtils.isEmpty(connectionSettings.getHostname())) {
				throw new MojoFailureException("[connectionSettings] No hostname defined!");
			}
		}
	}

	/**
	 * Executes all of the scripts in a given directory using the given Mongo
	 * object.
	 * 
	 * @param connectionSettings
	 *            the connection settings for the current database
	 * @param operation
	 *            the operation to perform (create,update,etc) which is used to
	 *            look for the scripts and create the combined file
	 * @param db
	 *            the db
	 * @param ignore
	 *            a list of scripts to ignore
	 * @throws MojoFailureException
	 *             on error
	 * @throws MojoExecutionException
	 *             on error
	 * @throws IOException
	 *             on error
	 */
	protected void executeScriptsInDirectory(ConnectionSettings connectionSettings, String operation, DB db,
			String[] ignore) throws MojoFailureException, MojoExecutionException, IOException {

		BufferedWriter outputFileWriter = null;

		try {
			File scriptsDirectory = new File(this.scriptsDirectory, connectionSettings.getDatabase());
			scriptsDirectory = new File(scriptsDirectory, operation);

			if (!scriptsDirectory.exists()) {
				getLog().info("  no scripts found at: " + scriptsDirectory.getAbsolutePath());
				return;
			}

			File outputFile = new File(this.outputDirectory, this.outputName + "." + connectionSettings.getDatabase()
					+ "." + operation + SCRIPT_EXTENSION);
			if (outputFile.exists()) {
				getLog().info("  deleting: " + outputFile.getAbsolutePath());
				outputFile.delete();
			}

			getLog().info("  creating dir: " + outputFile.getParentFile().getAbsolutePath());
			outputFile.getParentFile().mkdirs();

			getLog().info("  creating: " + outputFile.getAbsolutePath());
			outputFile.createNewFile();

			outputFileWriter = new BufferedWriter(new FileWriter(outputFile));

			getLog().info("  executing scripts in: " + scriptsDirectory.getAbsolutePath());

			// make sure we can read it, and that it's
			// a directory and not a file
			if (!scriptsDirectory.isDirectory()) {
				throw new MojoFailureException(scriptsDirectory.getAbsolutePath() + " is not a directory");
			}

			// get all files in directory
			File[] files = scriptsDirectory.listFiles();

			// sort the scripts so that they execute in a reproducible order
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File arg0, File arg1) {
					return arg0.getName().compareTo(arg1.getName());
				}
			});

			// loop through all the files and execute them unless they are to be
			// ignored
			List<String> ignoreList = Arrays.asList(ignore);
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory() && files[i].isFile()) {
					File file = files[i];
					String fileName = file.getName();

					//
					// Skip files that don't end with SCRIPT_EXTENSION
					//
					if (!fileName.endsWith(SCRIPT_EXTENSION)) {
						getLog().info(
								"    file '" + fileName + "' ignored. Doesn't end with '" + SCRIPT_EXTENSION + "'");
						continue;
					}

					//
					// Skip files that are to be ignored
					//
					if (ignoreList.contains(fileName)) {
						getLog().info("    script '" + fileName + "' ignored. Already applied.");
						continue;
					}

					//
					// Execute the script
					//
					double startTime = System.currentTimeMillis();

					executeScript(files[i], db, outputFileWriter);

					double endTime = System.currentTimeMillis();
					double elapsed = ((endTime - startTime) / 1000.0);
					getLog().info("    script '" + fileName + "' completed execution in " + elapsed + " second(s)");
				}
			}

		} finally {
			if (null != outputFileWriter) {
				outputFileWriter.close();
				outputFileWriter = null;
			}
		}
	}

	/**
	 * Executes the given script, using the given Mongo.
	 * 
	 * @param file
	 *            the file to execute
	 * @param mongo
	 *            the db
	 * @throws MojoFailureException
	 *             on error
	 * @throws MojoExecutionException
	 *             on error
	 * @throws IOException
	 *             on error
	 */
	protected void executeScript(File file, DB db, BufferedWriter outputFileWriter) throws MojoFailureException,
			MojoExecutionException, IOException {

		// talk a bit :)
		getLog().info("    executing script: " + file.getName());

		// make sure we can read it, and that it's
		// a file and not a directory
		if (!file.exists() || !file.canRead() || file.isDirectory() || !file.isFile()) {
			throw new MojoFailureException(file.getName() + " is not a file");
		}

		// our file reader
		StringBuffer data = new StringBuffer();
		BufferedReader inputFileReader = null;
		try {
			inputFileReader = new BufferedReader(new FileReader(file));

			String line;

			// loop through the statements
			while ((line = inputFileReader.readLine()) != null) {
				// append the line
				line.trim();
				data.append("\n").append(line);
			}
		} finally {
			inputFileReader.close();
		}

		// execute last statement
		try {
			if (this.executeScripts) {
				CommandResult result = db.doEval(data.toString(), new Object[0]);

				if (!result.ok()) {
					getLog().warn("Error executing " + file.getName() + ": " + result.getErrorMessage(),
							result.getException());
				} else {
					this.appendScriptToFileWriter(outputFileWriter, file, data);
					getLog().info("    " + file.getName() + " executed successfully");
				}
			} else {
				this.appendScriptToFileWriter(outputFileWriter, file, data);
			}
		} catch (Exception e) {
			getLog().error("    error executing " + file.getName(), e);
		}
	}

	/**
	 * 
	 * @param outputFileWriter
	 * @param scriptFile
	 * @param scriptData
	 * @throws IOException
	 */
	private void appendScriptToFileWriter(BufferedWriter outputFileWriter, File scriptFile, StringBuffer scriptData)
			throws IOException {
		outputFileWriter.append("//").append("\n");
		outputFileWriter.append("// -------------------- " + scriptFile.getName()).append("\n");
		outputFileWriter.append("//").append("\n");
		outputFileWriter.append(scriptData.toString());
		outputFileWriter.flush();
	}

	/**
	 * Opens a connection using the given settings.
	 * 
	 * @param dbSettings
	 *            the connection settings
	 * @return the Connection
	 * @throws MojoFailureException
	 *             on error
	 * @throws UnknownHostException
	 */
	protected Mongo openConnection(ConnectionSettings connectionSettings) throws MojoFailureException,
			UnknownHostException {

		// get server address
		ServerAddress serverAddr = (connectionSettings.getPort() != null) ? new ServerAddress(
				connectionSettings.getHostname(), connectionSettings.getPort().intValue()) : new ServerAddress(
				connectionSettings.getHostname());

		// get Mongo
		Mongo mongo = (connectionSettings.getOptions() != null) ? new Mongo(serverAddr, connectionSettings.getOptions())
				: new Mongo(serverAddr);

		// we're good :)
		return mongo;
	}

	/**
	 * Returns a DB from the given settings and mongo.
	 * 
	 * @param mongo
	 *            the mongo
	 * @param dbSettings
	 *            the settings
	 * @return the DB
	 */
	protected DB getDatabase(ConnectionSettings connectionSettings, Mongo mongo) {

		String username, password = null;

		// use settings to get authentication info for the given server
		if (!StringUtils.isEmpty(connectionSettings.getServerId())) {
			Server server = settings.getServer(connectionSettings.getServerId());
			username = server.getUsername();
			password = server.getPassword();

			// use settings in pom.xml
		} else {
			username = connectionSettings.getUserName();
			password = connectionSettings.getPassword();
		}

		// get the DB and optionaly authenticate
		DB db = mongo.getDB(connectionSettings.getDatabase());
		if (username != null && password != null) {
			db.authenticate(username, password.toCharArray());
		}
		return db;
	}

	/**
	 * Drops the configured mongo database.
	 * 
	 * @param mongo
	 *            the mogno
	 */
	protected void dropDatabase(ConnectionSettings connectionSettings, Mongo mongo) {
		mongo.dropDatabase(connectionSettings.getDatabase());
	}
}