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
 * A property source provides a way to retrieve property values.
 * Sources are checked in reverse order (last added is checked first) when looking up properties.
 *
 * <p>
 * For writable sources that support modifying property values, see {@link PropertyStore}.
 */
public interface PropertySource {

	/**
	 * Returns a property in this property source.
	 *
	 * @param name The property name.
	 * @return The property lookup result:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link PropertyLookupResult#missing()} if this source does not define the key.
	 * 		<li>{@link PropertyLookupResult#present(Optional)} with {@link Optional#empty()} if the key exists with a null value.
	 * 		<li>{@link PropertyLookupResult#present(Optional)} with a value if the key exists with a non-null value.
	 * 	</ul>
	 */
	PropertyLookupResult get(String name);
}
