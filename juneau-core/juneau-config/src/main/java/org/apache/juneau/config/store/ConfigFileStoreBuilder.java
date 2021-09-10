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

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link ConfigFileStore} objects.
 * {@review}
 */
@FluentSetters
public class ConfigFileStoreBuilder extends ConfigStoreBuilder {

	String directory, extensions;
	Charset charset;
	boolean enableWatcher, updateOnWrite;
	WatcherSensitivity watcherSensitivity;

	/**
	 * Constructor, default settings.
	 */
	protected ConfigFileStoreBuilder() {
		super();
		directory = env("ConfigFileStore.directory", ".");
		charset = env("ConfigFileStore.charset", Charset.defaultCharset());
		enableWatcher = env("ConfigFileStore.enableWatcher", false);
		watcherSensitivity = env("ConfigFileStore.watcherSensitivity", WatcherSensitivity.MEDIUM);
		updateOnWrite = env("ConfigFileStore.updateOnWrite", false);
		extensions = env("ConfigFileStore.extensions", "cfg");
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected ConfigFileStoreBuilder(ConfigFileStore copyFrom) {
		super(copyFrom);
		directory = copyFrom.directory;
		charset = copyFrom.charset;
		enableWatcher = copyFrom.enableWatcher;
		watcherSensitivity = copyFrom.watcherSensitivity;
		updateOnWrite = copyFrom.updateOnWrite;
		extensions = copyFrom.extensions;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected ConfigFileStoreBuilder(ConfigFileStoreBuilder copyFrom) {
		super(copyFrom);
		directory = copyFrom.directory;
		charset = copyFrom.charset;
		enableWatcher = copyFrom.enableWatcher;
		watcherSensitivity = copyFrom.watcherSensitivity;
		updateOnWrite = copyFrom.updateOnWrite;
		extensions = copyFrom.extensions;
	}

	@Override /* ContextBuilder */
	public ConfigFileStoreBuilder copy() {
		return new ConfigFileStoreBuilder(this);
	}

	@Override /* ContextBuilder */
	public ConfigFileStore build() {
		return new ConfigFileStore(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Local file system directory.
	 *
	 * <p>
	 * Identifies the path of the directory containing the configuration files.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.directory"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_DIRECTORY"
	 * 		<li><js>"."</js>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder directory(String value) {
		directory = value;
		return this;
	}

	/**
	 * Local file system directory.
	 *
	 * <p>
	 * Identifies the path of the directory containing the configuration files.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.directory"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_DIRECTORY"
	 * 		<li><js>"."</js>.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder directory(File value) {
		directory = value.getAbsolutePath();
		return this;
	}

	/**
	 * Charset for external files.
	 *
	 * <p>
	 * Identifies the charset of external files.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.charset"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_CHARSET"
	 * 		<li>{@link Charset#defaultCharset()}
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder charset(Charset value) {
		charset = value;
		return this;
	}

	/**
	 * Use watcher.
	 *
	 * <p>
	 * Use a file system watcher for file system changes.
	 *
	 * <ul class='notes'>
	 * 	<li>Calling {@link ConfigFileStore#close()} closes the watcher.
	 * </ul>
	 *
	 *	<p>
	 * 	The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.enableWatcher"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_ENABLEWATCHER"
	 * 		<li><jk>false</jk>.
	 * 	</ul>
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder enableWatcher() {
		enableWatcher = true;
		return this;
	}

	/**
	 * Watcher sensitivity.
	 *
	 * <p>
	 * Determines how frequently the file system is polled for updates.
	 *
	 * <ul class='notes'>
	 * 	<li>This relies on internal Sun packages and may not work on all JVMs.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.watcherSensitivity"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_WATCHERSENSITIVITY"
	 * 		<li>{@link WatcherSensitivity#MEDIUM}
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder watcherSensitivity(WatcherSensitivity value) {
		watcherSensitivity = value;
		return this;
	}

	/**
	 * Update-on-write.
	 *
	 * <p>
	 * When enabled, the {@link ConfigFileStore#update(String, String)} method will be called immediately following
	 * calls to {@link ConfigFileStore#write(String, String, String)} when the contents are changing.
	 * <br>This allows for more immediate responses to configuration changes on file systems that use
	 * polling watchers.
	 * <br>This may cause double-triggering of {@link ConfigStoreListener ConfigStoreListeners}.
	 *
	 *	<p>
	 * 	The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.updateOnWrite"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_UPDATEONWRITE"
	 * 		<li><jk>false</jk>.
	 * 	</ul>
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder updateOnWrite() {
		updateOnWrite = true;
		return this;
	}

	/**
	 * File extensions.
	 *
	 * <p>
	 * Defines what file extensions to search for when the config name does not have an extension.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"ConfigFileStore.extensions"
	 * 		<li>Environment variable <js>"CONFIGFILESTORE_EXTENSIONS"
	 * 		<li><js>"cfg"</js>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public ConfigFileStoreBuilder extensions(String value) {
		extensions = value;
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
	public ConfigFileStoreBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigFileStoreBuilder debug() {
		super.debug();
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
	public ConfigFileStoreBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	// </FluentSetters>
}
