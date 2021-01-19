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
package org.apache.juneau.utils;

import static java.lang.Character.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.utils.ReflectionMap.*;

/**
 * A builder for {@link ReflectionMap} objects.
 *
 * @param <V> The type of object in this map.
 */
public class ReflectionMapBuilder<V> {
	List<ClassEntry<V>> classEntries = new ArrayList<>();
	List<MethodEntry<V>> methodEntries = new ArrayList<>();
	List<FieldEntry<V>> fieldEntries = new ArrayList<>();
	List<ConstructorEntry<V>> constructorEntries = new ArrayList<>();

	/**
	 * Adds a mapping to this builder.
	 *
	 * @param key
	 * 	The mapping key.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>Full class name (e.g. <js>"com.foo.MyClass"</js>).
	 * 		<li>Simple class name (e.g. <js>"MyClass"</js>).
	 * 		<li>All classes (e.g. <js>"*"</js>).
	 * 		<li>Full method name (e.g. <js>"com.foo.MyClass.myMethod"</js>).
	 * 		<li>Simple method name (e.g. <js>"MyClass.myMethod"</js>).
	 * 		<li>A comma-delimited list of anything on this list.
	 * 	</ul>
	 * @param value The value for this mapping.
	 * @return This object (for method chaining).
	 */
	public ReflectionMapBuilder<V> append(String key, V value) {
		if (isEmpty(key))
			throw new RuntimeException("Invalid reflection signature: [" + key + "]");
		try {
			for (String k : ReflectionMap.splitNames(key)) {
				if (k.endsWith(")")) {
					int i = k.substring(0, k.indexOf('(')).lastIndexOf('.');
					if (i == -1 || isUpperCase(k.charAt(i+1))) {
						constructorEntries.add(new ConstructorEntry<>(k, value));
					} else {
						methodEntries.add(new MethodEntry<>(k, value));
					}
				} else {
					int i = k.lastIndexOf('.');
					if (i == -1) {
						classEntries.add(new ClassEntry<>(k, value));
					} else if (isUpperCase(k.charAt(i+1))) {
						classEntries.add(new ClassEntry<>(k, value));
						fieldEntries.add(new FieldEntry<>(k, value));
					} else {
						methodEntries.add(new MethodEntry<>(k, value));
						fieldEntries.add(new FieldEntry<>(k, value));
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new RuntimeException("Invalid reflection signature: [" + key + "]");
		}

		return this;
	}

	/**
	 * Create new instance of {@link ReflectionMap} based on the contents of this builder.
	 *
	 * @return A new {@link ReflectionMap} object.
	 */
	public ReflectionMap<V> build() {
		return new ReflectionMap<>(this);
	}
}