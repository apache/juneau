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

import java.nio.charset.*;
import java.util.*;

/**
 * Configuration settings for the {@link ConfigSource} class.
 */
public class ConfigSourceSettings {

	private final List<String> searchPaths;
	private final Charset charset;
	private final boolean readonly, createIfNotExists;

	static class Builder {
		private List<String> searchPaths = Arrays.asList(new String[]{"."});
		private Charset charset = Charset.defaultCharset();
		private boolean readonly = false, createIfNotExists = true;

		Builder searchPaths(String[] searchPaths) {
			this.searchPaths = Arrays.asList(searchPaths);
			return this;
		}

		Builder charset(Charset charset) {
			this.charset = charset;
			return this;
		}

		Builder readonly(boolean readonly) {
			this.readonly = readonly;
			return this;
		}

		Builder createIfNotExists(boolean createIfNotExists) {
			this.createIfNotExists = createIfNotExists;
			return this;
		}

		ConfigSourceSettings build() {
			return new ConfigSourceSettings(this);
		}
	}

	ConfigSourceSettings(Builder b) {
		this.searchPaths = b.searchPaths;
		this.charset = b.charset;
		this.readonly = b.readonly;
		this.createIfNotExists = b.createIfNotExists;
	}

	/**
	 * Returns the paths to search to find config files.
	 *
	 * @return The paths to search to find config files.
	 */
	public List<String> getSearchPaths() {
		return searchPaths;
	}

	/**
	 * Returns the charset of the config file.
	 *
	 * @return The charset of the config file.
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Specifies whether the config file should be opened in read-only mode.
	 *
	 * @return <jk>true</jk> if the config file should be opened in read-only mode.
	 */
	public boolean isReadonly() {
		return readonly;
	}

	/**
	 * Specifies whether config files should be created if they're not found in the search paths.
	 *
	 * <p>
	 * Note that the first writable path will be used for the location of the file.
	 *
	 * @return <jk>true</jk> if the config file should be created if not found.
	 */
	public boolean isCreateIfNotExists() {
		return createIfNotExists;
	}
}

