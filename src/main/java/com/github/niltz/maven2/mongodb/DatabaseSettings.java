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

/**
 * Class for storing database connection settings.
 */
public class DatabaseSettings {

	/**
	 * The database connection settings.
	 * 
	 * @parameter
	 * @required
	 */
	private ConnectionSettings connectionSettings;

	/**
	 * The collection in the database in which to look for applied updates
	 * 
	 * @parameter default-value="appliedUpdates"
	 */
	private String updatesCollection = "appliedUpdates";

	/**
	 * The the name of the field in the collection that stores the update script
	 * that was applied
	 * 
	 * @parameter default-value="name"
	 */
	private String updatesCollectionScriptNameField = "name";

	/**
	 * @return the connectionSettings
	 */
	ConnectionSettings getConnectionSettings() {
		return connectionSettings;
	}

	/**
	 * @param connectionSettings
	 *            the connectionSettings to set
	 */
	void setConnectionSettings(ConnectionSettings connectionSettings) {
		this.connectionSettings = connectionSettings;
	}

	/**
	 * @return the updatesCollection
	 */
	String getUpdatesCollection() {
		return updatesCollection;
	}

	/**
	 * @param updatesCollection
	 *            the updatesCollection to set
	 */
	void setUpdatesCollection(String updatesCollection) {
		this.updatesCollection = updatesCollection;
	}

	/**
	 * @return the updatesCollectionScriptNameField
	 */
	String getUpdatesCollectionScriptNameField() {
		return updatesCollectionScriptNameField;
	}

	/**
	 * @param updatesCollectionScriptNameField
	 *            the updatesCollectionScriptNameField to set
	 */
	void setUpdatesCollectionScriptNameField(String updatesCollectionScriptNameField) {
		this.updatesCollectionScriptNameField = updatesCollectionScriptNameField;
	}
}