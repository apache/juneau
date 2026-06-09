/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.config.store;

import static java.util.Collections.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.config.format.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.marshall.*;

/**
 * Represents a storage location for configuration files.
 *
 * <p>
 * Content stores require two methods to be implemented:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #read(String)} - Retrieve a config file.
 * 	<li class='jm'>{@link #write(String,String,String)} - ConfigStore a config file.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
*/
@SuppressWarnings({
	"rawtypes",
	"resource" // ConfigStore manages Closeable resources
})
public abstract class ConfigStore extends Context implements Closeable {

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends Context.Builder<SELF> {

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(ConfigStore copyFrom) {
			super(copyFrom);
		}

	}

	private final ConcurrentHashMap<String,ConfigMap> configMaps = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String,Set<ConfigStoreListener>> listeners = new ConcurrentHashMap<>();

	// Tracks the chain of configs currently being constructed on this thread so that a circular import
	// (a config that directly or indirectly imports itself) can be detected and reported cleanly instead
	// of recursing infinitely through getMap()->new ConfigMap()->load()->getMap() until the stack overflows.
	private static final ThreadLocal<Set<String>> LOADING = ThreadLocal.withInitial(LinkedHashSet::new);

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ConfigStore(Builder builder) {
		super(builder);
	}

	/**
	 * Checks whether the configuration with the specified name exists in this store.
	 *
	 * @param name The config name.
	 * @return <jk>true</jk> if the configuration with the specified name exists in this store.
	 */
	public abstract boolean exists(String name);

	/**
	 * Returns a map file containing the parsed contents of a configuration.
	 *
	 * @param name The configuration name.
	 * @return
	 * 	The parsed configuration.
	 * 	<br>Never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public synchronized ConfigMap getMap(String name) throws IOException {
		return getMap(name, IniConfigFormat.INSTANCE);
	}

	/**
	 * Returns a map file containing the parsed contents of a configuration.
	 *
	 * @param name The configuration name.
	 * @param format The configuration format.
	 * @return The parsed configuration.
	 * @throws IOException Thrown by underlying stream.
	 */
	public synchronized ConfigMap getMap(String name, ConfigFormat format) throws IOException {
		name = resolveName(name);
		var format2 = format == null ? IniConfigFormat.INSTANCE : format;
		var key = format2.id() + ":" + name;
		var cm = configMaps.get(key);
		if (nn(cm))
			return cm;

		// Detect circular imports before recursing into ConfigMap construction.
		var loading = LOADING.get();
		if (! loading.add(key))
			throw new ConfigException("Import loop detected in configuration:  {0}", importChain(loading, name));
		try {
			cm = new ConfigMap(this, name, format2);
		} finally {
			loading.remove(key);
		}

		var cm2 = configMaps.putIfAbsent(key, cm);
		if (nn(cm2))
			return cm2;
		register(name, cm);
		return cm;
	}

	// Renders the chain of configs currently being loaded (e.g. "B -> A2 -> A1 -> A2") for a circular-import error message.
	// The entries in the loading set are cache keys of the form "<formatId>:<name>", so the format prefix is stripped.
	private static String importChain(Set<String> loading, String reentry) {
		var sb = new StringBuilder();
		for (var key : loading)
			sb.append(key.substring(key.indexOf(':') + 1)).append(" -> ");
		return sb.append(reentry).toString();
	}

	/**
	 * Returns the contents of the configuration file.
	 *
	 * @param name The config file name.
	 * @return
	 * 	The contents of the configuration file.
	 * 	<br>A blank string if the config does not exist.
	 * 	<br>Never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public abstract String read(String name) throws IOException;

	/**
	 * Registers a new listener on this store.
	 *
	 * @param name The configuration name to listen for.
	 * @param l The new listener.
	 * @return This object.
	 */
	public synchronized ConfigStore register(String name, ConfigStoreListener l) {
		name = resolveName(name);
		var s = listeners.computeIfAbsent(name, k -> synced(newSetFromMap(new IdentityHashMap<>())));
		s.add(l);
		return this;
	}

	/**
	 * Unregisters a listener from this store.
	 *
	 * @param name The configuration name to listen for.
	 * @param l The listener to unregister.
	 * @return This object.
	 */
	public synchronized ConfigStore unregister(String name, ConfigStoreListener l) {
		name = resolveName(name);
		var s = listeners.get(name);
		if (nn(s))
			s.remove(l);
		return this;
	}

	/**
	 * Called when the physical contents of a config file have changed.
	 *
	 * <p>
	 * Triggers calls to {@link ConfigStoreListener#onChange(String)} on all registered listeners.
	 *
	 * @param name The config name (e.g. the filename without the extension).
	 * @param contents The new contents.
	 * @return This object.
	 */
	public synchronized ConfigStore update(String name, String contents) {
		name = resolveName(name);
		var s = listeners.get(name);
		if (nn(s))
			listeners.get(name).forEach(x -> x.onChange(contents));
		return this;
	}

	/**
	 * Convenience method for updating the contents of a file with lines.
	 *
	 * @param name The config name (e.g. the filename without the extension).
	 * @param contentLines The new contents.
	 * @return This object.
	 */
	public synchronized ConfigStore update(String name, String...contentLines) {
		name = resolveName(name);
		var sb = new StringBuilder();
		for (var l : contentLines)
			sb.append(l).append('\n');
		return update(name, sb.toString());
	}

	/**
	 * Saves the contents of the configuration file if the underlying storage hasn't been modified.
	 *
	 * @param name The config file name.
	 * @param expectedContents The expected contents of the file.
	 * @param newContents The new contents.
	 * @return
	 * 	If <jk>null</jk>, then we successfully stored the contents of the file.
	 * 	<br>Otherwise the contents of the file have changed and we return the new contents of the file.
	 * @throws IOException Thrown by underlying stream.
	 */
	public abstract String write(String name, String expectedContents, String newContents) throws IOException;

	/**
	 * Subclasses can override this method to convert config names to internal forms.
	 *
	 * <p>
	 * For example, the {@link FileStore} class can take in both <js>"MyConfig"</js> and <js>"MyConfig.cfg"</js>
	 * as names that both resolve to <js>"MyConfig.cfg"</js>.
	 *
	 * @param name The name to resolve.
	 * @return The resolved name.
	 */
	protected String resolveName(String name) {
		return name;
	}
}