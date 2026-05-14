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
 * A writable extension of {@link PropertySource} that supports modifying property values.
 */
public interface PropertyStore extends PropertySource {

	/**
	 * Sets a property in this store.
	 *
	 * @param name The property name.
	 * @param value The property value, or <c>null</c> to set an empty override.
	 */
	void set(String name, String value);

	/**
	 * Removes a property from this store.
	 *
	 * @param name The property name to remove.
	 */
	void unset(String name);

	/**
	 * Clears all properties from this store.
	 */
	void clear();
}
