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
package org.apache.juneau.commons.settings;

import java.util.*;

/**
 * Interface for pluggable property sources used by {@link Settings}.
 *
 * <p>
 * A setting source provides a way to retrieve and optionally modify property values.
 * Sources are checked in reverse order (last added is checked first) when looking up properties.
 *
 * <h5 class='section'>Return Value Semantics:</h5>
 * <ul class='spaced-list'>
 * 	<li><c>null</c> - The setting does not exist in this source. The lookup will continue to the next source.
 * 	<li><c>Optional.empty()</c> - The setting exists but has an explicitly null value. This will be returned
 * 		immediately, overriding any values from lower-priority sources.
 * 	<li><c>Optional.of(value)</c> - The setting exists and has a non-null value. This will be returned immediately.
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a writable source</jc>
 * 	MapSource <jv>source</jv> = <jk>new</jk> MapSource();
 * 	<jv>source</jv>.set(<js>"my.property"</js>, <js>"value"</js>);
 *
 * 	<jc>// Create a read-only source from a function</jc>
 * 	ReadOnlySource <jv>readOnly</jv> = <jk>new</jk> ReadOnlySource(x -&gt; System.getProperty(x));
 * </p>
 */
public interface SettingSource {

	/**
	 * Returns a setting in this setting source.
	 *
	 * <p>
	 * Return value semantics:
	 * <ul>
	 * 	<li><c>null</c> - The setting does not exist in this source. The lookup will continue to the next source.
	 * 	<li><c>Optional.empty()</c> - The setting exists but has an explicitly null value. This will be returned
	 * 		immediately, overriding any values from lower-priority sources.
	 * 	<li><c>Optional.of(value)</c> - The setting exists and has a non-null value. This will be returned immediately.
	 * </ul>
	 *
	 * @param name The property name.
	 * @return The property value, <c>null</c> if the property doesn't exist in this source, or <c>Optional.empty()</c>
	 * 	if the property exists but has a null value.
	 */
	Optional<String> get(String name);

	/**
	 * Returns whether this source is writable.
	 *
	 * <p>
	 * If <c>false</c>, all write operations ({@link #set(String, String)}, {@link #unset(String)}, {@link #clear()})
	 * should be no-ops.
	 *
	 * @return <c>true</c> if this source is writable, <c>false</c> otherwise.
	 */
	boolean canWrite();

	/**
	 * Sets a setting in this setting source.
	 *
	 * <p>
	 * Should be a no-op if the source is not writable (i.e., {@link #canWrite()} returns <c>false</c>).
	 *
	 * <p>
	 * Setting a value to <c>null</c> means that {@link #get(String)} will return <c>Optional.empty()</c> for that key,
	 * effectively overriding any values from lower-priority sources. Use {@link #unset(String)} if you want
	 * {@link #get(String)} to return <c>null</c> (indicating the key doesn't exist in this source).
	 *
	 * @param name The property name.
	 * @param value The property value, or <c>null</c> to set an empty override.
	 */
	void set(String name, String value);

	/**
	 * Removes a setting from this setting source.
	 *
	 * <p>
	 * Should be a no-op if the source is not writable (i.e., {@link #canWrite()} returns <c>false</c>).
	 *
	 * <p>
	 * After calling this method, {@link #get(String)} will return <c>null</c> for the specified key,
	 * indicating that the key doesn't exist in this source (as opposed to returning <c>Optional.empty()</c>,
	 * which would indicate the key exists but has a null value).
	 *
	 * @param name The property name to remove.
	 */
	void unset(String name);

	/**
	 * Clears all settings from this setting source.
	 *
	 * <p>
	 * Should be a no-op if the source is not writable (i.e., {@link #canWrite()} returns <c>false</c>).
	 *
	 * <p>
	 * After calling this method, all keys will be removed from this source, and {@link #get(String)} will
	 * return <c>null</c> for all keys.
	 */
	void clear();
}
