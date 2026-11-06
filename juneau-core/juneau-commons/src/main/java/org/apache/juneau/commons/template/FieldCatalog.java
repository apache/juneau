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
package org.apache.juneau.commons.template;

import java.util.*;

/**
 * The fields of one list's rows.
 */
public final class FieldCatalog {

	private final Set<String> names;

	private FieldCatalog(Set<String> names) {
		this.names = names;
	}

	/**
	 * Creates a catalog from field names.
	 *
	 * @param names The names.  Must not be <jk>null</jk>.
	 * @return A new catalog.
	 */
	public static FieldCatalog of(String...names) {
		var s = new LinkedHashSet<String>();
		for (var n : names)
			s.add(new FieldDef(n).name());
		return new FieldCatalog(Collections.unmodifiableSet(s));
	}

	/**
	 * Creates a catalog from field definitions.
	 *
	 * @param fields The fields.  Must not be <jk>null</jk>.
	 * @return A new catalog.
	 */
	public static FieldCatalog of(FieldDef...fields) {
		return of(Arrays.stream(fields).map(FieldDef::name).toArray(String[]::new));
	}

	/**
	 * Returns whether the catalog has this field.
	 *
	 * @param name The field name.
	 * @return <jk>true</jk> if declared.
	 */
	public boolean has(String name) {
		return names.contains(name);
	}
}
