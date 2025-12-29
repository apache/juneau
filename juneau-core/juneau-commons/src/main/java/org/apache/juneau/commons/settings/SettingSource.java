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
 * A setting source provides a way to retrieve property values.
 * Sources are checked in reverse order (last added is checked first) when looking up properties.
 *
 * <p>
 * For writable sources that support modifying property values, see {@link SettingStore}.
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
 * 	<jc>// Create a read-only functional source directly from a lambda</jc>
 * 	FunctionalSource <jv>readOnly</jv> = name -&gt; opt(System.getProperty(name));
 *
 * 	<jc>// Or use the factory method</jc>
 * 	FunctionalSource <jv>readOnly2</jv> = FunctionalSource.<jsf>of</jsf>(System::getProperty);
 *
 * 	<jc>// Stores can be used as sources (they extend SettingSource)</jc>
 * 	MapStore <jv>store</jv> = <jk>new</jk> MapStore();
 * 	<jv>store</jv>.set(<js>"my.property"</js>, <js>"value"</js>);
 * 	Settings.<jsf>get</jsf>().addSource(<jv>store</jv>);  <jc>// Stores can be added as sources</jc>
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
}
