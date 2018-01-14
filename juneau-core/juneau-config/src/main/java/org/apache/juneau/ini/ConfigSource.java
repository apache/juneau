// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.ini;

/**
 * Represents a storage location of a configuration file.
 */
public abstract class ConfigSource {

	/** The path of the config file. */
	private final ConfigSourceSettings settings;

	/**
	 * Constructor.
	 * 
	 * @param settings
	 * 	The settings for this config source.
	 */
	protected ConfigSource(ConfigSourceSettings settings) {
		this.settings = settings;
	}

	/**
	 * Returns the name of the config file.
	 * 
	 * @return The name of the config file.
	 */
	protected final ConfigSourceSettings getSettings() {
		return settings;
	}

	/**
	 * Returns the contents of the configuration file.
	 * 
	 * @param name The config file name.
	 * @return The contents of the configuration file.
	 * @throws Exception
	 */
	protected abstract String read(String name) throws Exception;

	/**
	 * Saves the contents of the configuration file if the underlying storage hasn't been modified.
	 * 
	 * @param name The config file name.
	 * @param contents The new contents of the configuration file.
	 * @return <jk>true</jk> if we successfully saved the new configuration file contents, or <jk>false</jk> if the
	 * 	underlying storage changed since the last time the {@link #read(String)} method was called.
	 * @throws Exception
	 */
	protected abstract boolean write(String name, String contents) throws Exception;

	/**
	 * Returns whether the underlying configuration contents have changed.
	 * 
	 * <p>
	 * For example, if the configuration source is a file, this method would return <jk>true</jk> if the
	 * file on the filesystem has been modified since the {@link #read(String)} method was called.
	 * 
	 * @param name The config file name.
	 * @return <jk>true</jk> if the persisted contents of the config file have changed.
	 * @throws Exception
	 */
	protected abstract boolean hasBeenModified(String name) throws Exception;
}
