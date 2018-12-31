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
package org.apache.juneau.config.store;

import static org.apache.juneau.config.store.ConfigFileStore.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;

/**
 * Builder for {@link ConfigFileStore} objects.
 */
public class ConfigFileStoreBuilder extends ConfigStoreBuilder {

	/**
	 * Constructor, default settings.
	 */
	public ConfigFileStoreBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public ConfigFileStoreBuilder(PropertyStore ps) {
		super(ps);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Local file system directory.
	 *
	 * <p>
	 * Identifies the path of the directory containing the configuration files.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_directory}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder directory(String value) {
		super.set(FILESTORE_directory, value);
		return this;
	}

	/**
	 * Configuration property:  Local file system directory.
	 *
	 * <p>
	 * Identifies the path of the directory containing the configuration files.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_directory}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder directory(File value) {
		super.set(FILESTORE_directory, value);
		return this;
	}

	/**
	 * Configuration property:  Charset.
	 *
	 * <p>
	 * Identifies the charset of external files.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_charset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder charset(String value) {
		super.set(FILESTORE_charset, value);
		return this;
	}

	/**
	 * Configuration property:  Charset.
	 *
	 * <p>
	 * Identifies the charset of external files.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_charset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder charset(Charset value) {
		super.set(FILESTORE_charset, value);
		return this;
	}

	/**
	 * Configuration property:  Use watcher.
	 *
	 * <p>
	 * Shortcut for calling <code>useWatcher(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_useWatcher}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder useWatcher() {
		super.set(FILESTORE_useWatcher, true);
		return this;
	}

	/**
	 * Configuration property:  Watcher sensitivity.
	 *
	 * <p>
	 * Determines how frequently the file system is polled for updates.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_watcherSensitivity}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link WatcherSensitivity#MEDIUM}
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder watcherSensitivity(WatcherSensitivity value) {
		super.set(FILESTORE_watcherSensitivity, value);
		return this;
	}

	/**
	 * Configuration property:  Update-on-write.
	 *
	 * <p>
	 * Shortcut for calling <code>useWatcher(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_updateOnWrite}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder updateOnWrite() {
		super.set(FILESTORE_updateOnWrite, true);
		return this;
	}

	/**
	 * Configuration property:  Watcher sensitivity.
	 *
	 * <p>
	 * Determines how frequently the file system is polled for updates.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_watcherSensitivity}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link WatcherSensitivity#MEDIUM}
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder watcherSensitivity(String value) {
		super.set(FILESTORE_watcherSensitivity, value);
		return this;
	}

	/**
	 * Configuration property:  File extensions.
	 *
	 * <p>
	 * Defines what file extensions to search for when the config name does not have an extension.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"cfg"</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder extensions(String...value) {
		super.set(FILESTORE_extensions, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigFileStore build() {
		return new ConfigFileStore(getPropertyStore());
	}
}
