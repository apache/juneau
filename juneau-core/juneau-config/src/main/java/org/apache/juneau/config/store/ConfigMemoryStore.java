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

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;

/**
 * Filesystem-based storage location for configuration files.
 *
 * <p>
 * Points to a file system directory containing configuration files.
 */
@ConfigurableContext
public class ConfigMemoryStore extends ConfigStore {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "ConfigMemoryStore";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default memory store, all default values.*/
	public static final ConfigMemoryStore DEFAULT = ConfigMemoryStore.create().build();


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static ConfigMemoryStoreBuilder create() {
		return new ConfigMemoryStoreBuilder();
	}

	@Override /* Context */
	public ConfigMemoryStoreBuilder copy() {
		return new ConfigMemoryStoreBuilder(this);
	}

	private final ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param cp The settings for this content store.
	 */
	protected ConfigMemoryStore(ContextProperties cp) {
		super(cp);
	}

	@Override /* ConfigStore */
	public synchronized String read(String name) {
		return emptyIfNull(cache.get(name));
	}

	@Override /* ConfigStore */
	public synchronized String write(String name, String expectedContents, String newContents) {

		// This is a no-op.
		if (eq(expectedContents, newContents))
			return null;

		String currentContents = read(name);

		if (expectedContents != null && ! eq(currentContents, expectedContents))
			return currentContents;

		update(name, newContents);

		return null;
	}

	@Override /* ConfigStore */
	public synchronized boolean exists(String name) {
		return cache.containsKey(name);
	}

	@Override /* ConfigStore */
	public synchronized ConfigMemoryStore update(String name, String newContents) {
		if (newContents == null)
			cache.remove(name);
		else
			cache.put(name, newContents);
		super.update(name, newContents);  // Trigger any listeners.
		return this;
	}

	/**
	 * No-op.
	 */
	@Override /* Closeable */
	public void close() throws IOException {
		// No-op
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"ConfigMemoryStore",
				OMap
					.create()
					.filtered()
			);
	}
}
