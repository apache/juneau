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
package org.apache.juneau.config.source;

import static org.apache.juneau.config.source.FileStore.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;

/**
 * Builder for {@link FileStore} objects.
 */
public class FileStoreBuilder extends StoreBuilder {

	/**
	 * Constructor, default settings.
	 */
	public FileStoreBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param ps The initial configuration settings for this builder.
	 */
	public FileStoreBuilder(PropertyStore ps) {
		super(ps);
	}

	
	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Local file system directory.
	 * 
	 * <p>
	 * Identifies the path of the directory containing the configuration files.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link FileStore#FILESTORE_directory}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder directory(String value) {
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
	 * 	<li class='jf'>{@link FileStore#FILESTORE_directory}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder directory(File value) {
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
	 * 	<li class='jf'>{@link FileStore#FILESTORE_charset}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder charset(String value) {
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
	 * 	<li class='jf'>{@link FileStore#FILESTORE_charset}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <js>"."</js>.
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder charset(Charset value) {
		super.set(FILESTORE_charset, value);
		return this;
	}
	
	/**
	 * Configuration property:  Use watcher.
	 * 
	 * <p>
	 * Use a file system watcher for file system changes.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link FileStore#FILESTORE_useWatcher}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder useWatcher(boolean value) {
		super.set(FILESTORE_useWatcher, value);
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
	 * 	<li class='jf'>{@link FileStore#FILESTORE_useWatcher}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder useWatcher() {
		super.set(FILESTORE_useWatcher, true);
		return this;
	}
	
	/**
	 * Configuration property:  Config file extension.
	 * 
	 * <p>
	 * File extension identifier for config files.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <js>"cfg"</js>.
	 * @return This object (for method chaining).
	 */
	public FileStoreBuilder ext(String value) {
		super.set(FILESTORE_ext, value);
		return this;
	}

	@Override /* ContextBuilder */
	public FileStore build() {
		return new FileStore(getPropertyStore());
	}
}
