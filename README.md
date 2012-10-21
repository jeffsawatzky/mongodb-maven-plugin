# mongodb-maven-plugin

Run create/populate/update scripts on multiple mongo databases as part of your maven build. This is useful for continuous integration servers. It can also generate a combined file of all updates applied to a database, which is useful for providing operations with a single update script for the production database.

## Usage

By default the plugin will look for the javascript files in the following folders:
	${basedir}/src/main/mongodb/<db>/create/
	${basedir}/src/main/mongodb/<db>/populate/
	${basedir}/src/main/mongodb/<db>/update/

The best way I have found to use it is to configure the plugin properties in the build/pluginManagement section. That way you can execute the plugin easily from the command line or through the pom.

	<build>  
	  <pluginManagement>  
	    <plugins>
	      <!--
	      Configures the mongodb plugin for our various mongodb databases
	      To run this plugin on it's own to create a default database run "mvn mongodb:create"
	      To run this plugin on it's own to populate a default database run "mvn mongodb:populate"
	      To run this plugin on it's own to update a current database run "mvn mongodb:update"
	      Note that in order to use the shorthand "mongodb" for the plugin, you should read:
	      http://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html
	      -->
	      <plugin>
	        <artifactId>mongodb-maven-plugin</artifactId>
	        <groupId>com.github.niltz.maven2</groupId>
	        <version>1.0</version>
	        <configuration>
	          <!-- scriptsDirectory>set this if you don't want to use the defaults described above</scriptsDirectory -->
	          <!--
	          This tells the plugin whether or not it should acutally execute the scripts against the database, or do a dry run.
	          In both cases the plugin produces a single script file that contains all of the operations that would have been
	          performed. You can set this to false to generate an update script for a production database
	          -->
	          <executeScripts>true</executeScripts>
	          <databaseSettings>
	            <param>
	              <-- updatesCollection>Set this to the collection name to store applied updates. Default is "appliedUpdates"</updatesCollection -->
	              <-- updatesCollectionScriptNameField>Set this to the field name in the collection where the script name is stored. Default is "name"</updatesCollectionScriptNameField -->
	              <connectionSettings>
	                <!-- There are a lot more connection settings available. Look at the ConnectionSettings.java for them -->
	                <hostname>db1.host</hostname>
	                <database>db1</database>
	              </connectionSettings>
	            </param>
	            <param>
	              <connectionSettings>
	                <hostname>db2.host</hostname>
	                <database>db2</database>
	              </connectionSettings>
	            </param>
	          </databaseSettings>
	        </configuration>
	      </plugin>
	    </plugins>
	  </pluginManagement>
	</build>
  
Then to execute the updates automatically, create a profile like (setting up the profile activation how you like):
  
	<profile>
	  <id>mongodb-update</id>
	  <activation>...</activation>
	  <build>
	    <plugins>
	      <plugin>
	        <artifactId>mongodb-maven-plugin</artifactId>
	        <groupId>com.github.niltz.maven2</groupId>
	        <executions>
	          <execution>
	            <id>mongodb-update</id>
	            <phase>pre-integration-test</phase>
	            <goals><goal>update</goal></goals>
	          </execution>
	        </executions>
	      </plugin>
	    </plugins>
	  </build>
	</profile>

## Updates

Updates assume there is a collection in the database which stores the list of applied updates. Each update script is responsible for storing this information at some point during the update. By default the collection which the plugin looks into is call "appliedUpdates" and it looks for a field called "name" and compares it to the name of the update script. If the update script already exists in the collection then it is skipped and not re-applied. This means that the update scripts should have unique names. The scripts are also sorted by name and applied in the sorted order, that way you know the order in which they are applied.