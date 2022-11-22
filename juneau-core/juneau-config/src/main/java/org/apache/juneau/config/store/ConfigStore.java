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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

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
public abstract class ConfigStore extends Context implements Closeable {

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public abstract static class Builder extends Context.Builder {

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(ConfigStore copyFrom) {
			super(copyFrom);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
		}

		@Override /* Context.Builder */
		public abstract Builder copy();

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final ConcurrentHashMap<String,Set<ConfigStoreListener>> listeners = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String,ConfigMap> configMaps = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ConfigStore(Builder builder) {
		super(builder);
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
	 * Checks whether the configuration with the specified name exists in this store.
	 *
	 * @param name The config name.
	 * @return <jk>true</jk> if the configuration with the specified name exists in this store.
	 */
	public abstract boolean exists(String name);

	/**
	 * Registers a new listener on this store.
	 *
	 * @param name The configuration name to listen for.
	 * @param l The new listener.
	 * @return This object.
	 */
	public synchronized ConfigStore register(String name, ConfigStoreListener l) {
		name = resolveName(name);
		Set<ConfigStoreListener> s = listeners.get(name);
		if (s == null) {
			s = synced(Collections.newSetFromMap(new IdentityHashMap<ConfigStoreListener,Boolean>()));
			listeners.put(name, s);
		}
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
		Set<ConfigStoreListener> s = listeners.get(name);
		if (s != null)
			s.remove(l);
		return this;
	}

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
		name = resolveName(name);
		ConfigMap cm = configMaps.get(name);
		if (cm != null)
			return cm;
		cm = new ConfigMap(this, name);
		ConfigMap cm2 = configMaps.putIfAbsent(name, cm);
		if (cm2 != null)
			return cm2;
		register(name, cm);
		return cm;
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
		Set<ConfigStoreListener> s = listeners.get(name);
		if (s != null)
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
		StringBuilder sb = new StringBuilder();
		for (String l : contentLines)
			sb.append(l).append('\n');
		return update(name, sb.toString());
	}

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
