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
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Builder for {@link ConfigFileStore} objects.
 */
@FluentSetters
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
	 * @param cp The initial configuration settings for this builder.
	 */
	public ConfigFileStoreBuilder(ContextProperties cp) {
		super(cp);
	}

	@Override /* ContextBuilder */
	public ConfigFileStore build() {
		return new ConfigFileStore(getContextProperties());
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
	 * <ul class='seealso'>
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
	 * <ul class='seealso'>
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
	 * <ul class='seealso'>
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
	 * <ul class='seealso'>
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_enableWatcher}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder enableWatcher() {
		super.set(FILESTORE_enableWatcher);
		return this;
	}

	/**
	 * Configuration property:  Watcher sensitivity.
	 *
	 * <p>
	 * Determines how frequently the file system is polled for updates.
	 *
	 * <ul class='seealso'>
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link ConfigFileStore#FILESTORE_enableUpdateOnWrite}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder enableUpdateOnWrite() {
		super.set(FILESTORE_enableUpdateOnWrite);
		return this;
	}

	/**
	 * Configuration property:  Watcher sensitivity.
	 *
	 * <p>
	 * Determines how frequently the file system is polled for updates.
	 *
	 * <ul class='seealso'>
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
	public ConfigFileStoreBuilder extensions(String value) {
		super.set(FILESTORE_extensions, value);
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	// </FluentSetters>
}
