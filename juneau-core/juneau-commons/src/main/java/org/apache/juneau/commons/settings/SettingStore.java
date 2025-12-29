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

/**
 * A writable extension of {@link SettingSource} that supports modifying property values.
 *
 * <p>
 * This interface extends {@link SettingSource} with methods for setting, unsetting, and clearing properties.
 * All stores that implement this interface provide read/write access and can be modified at runtime.
 *
 * <p>
 * <b>Sources vs Stores:</b>
 * <ul>
 * 	<li><b>Sources</b> ({@link SettingSource}) - Provide read-only access to property values
 * 	<li><b>Stores</b> ({@link SettingStore}) - Provide read/write access to property values
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a writable store</jc>
 * 	MapStore <jv>store</jv> = <jk>new</jk> MapStore();
 * 	<jv>store</jv>.set(<js>"my.property"</js>, <js>"value"</js>);
 * 	<jv>store</jv>.unset(<js>"my.property"</js>);
 * 	<jv>store</jv>.clear();
 * </p>
 */
public interface SettingStore extends SettingSource {

	/**
	 * Sets a setting in this store.
	 *
	 * <p>
	 * Setting a value to <c>null</c> means that {@link #get(String)} will return <c>Optional.empty()</c> for that key,
	 * effectively overriding any values from lower-priority sources. Use {@link #unset(String)} if you want
	 * {@link #get(String)} to return <c>null</c> (indicating the key doesn't exist in this store).
	 *
	 * @param name The property name.
	 * @param value The property value, or <c>null</c> to set an empty override.
	 */
	void set(String name, String value);

	/**
	 * Removes a setting from this store.
	 *
	 * <p>
	 * After calling this method, {@link #get(String)} will return <c>null</c> for the specified key,
	 * indicating that the key doesn't exist in this store (as opposed to returning <c>Optional.empty()</c>,
	 * which would indicate the key exists but has a null value).
	 *
	 * @param name The property name to remove.
	 */
	void unset(String name);

	/**
	 * Clears all settings from this store.
	 *
	 * <p>
	 * After calling this method, all keys will be removed from this store, and {@link #get(String)} will
	 * return <c>null</c> for all keys.
	 */
	void clear();
}

