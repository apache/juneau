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
 * Describes the names a template may read, for {@link TemplateResolver#check(String, TemplateCatalog)}.
 *
 * <p>
 * A <em>field</em> is a top-level name.  A <em>list</em> is a top-level name holding rows; it may carry a
 * {@link FieldCatalog} naming the row fields, and without one any row field is accepted.
 */
public final class TemplateCatalog {

	private final Set<String> fields;
	private final Map<String,FieldCatalog> lists;
	private final boolean allowUnknownFunctions;

	private TemplateCatalog(Builder b) {
		fields = Collections.unmodifiableSet(new LinkedHashSet<>(b.fields));
		lists = Collections.unmodifiableMap(new LinkedHashMap<>(b.lists));
		allowUnknownFunctions = b.allowUnknownFunctions;
	}

	/**
	 * Creates a builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() { return new Builder(); }

	/** Builder for {@link TemplateCatalog}. */
	public static final class Builder {
		private final Set<String> fields = new LinkedHashSet<>();
		private final Map<String,FieldCatalog> lists = new LinkedHashMap<>();
		private boolean allowUnknownFunctions;

		private Builder() {}

		/**
		 * Declares top-level fields.
		 *
		 * @param names The names.
		 * @return This object.
		 */
		public Builder fields(String...names) {
			for (var n : names)
				fields.add(new FieldDef(n).name());
			return this;
		}

		/**
		 * Declares a list whose row fields are not checked.
		 *
		 * @param name The list name.
		 * @return This object.
		 */
		public Builder list(String name) {
			lists.put(new FieldDef(name).name(), null);
			return this;
		}

		/**
		 * Declares a list with its row fields.
		 *
		 * @param name The list name.
		 * @param rowFields The row fields.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder list(String name, FieldCatalog rowFields) {
			lists.put(new FieldDef(name).name(), Objects.requireNonNull(rowFields, "rowFields must not be null"));
			return this;
		}

		/**
		 * Accepts function names the resolver does not know or permit.
		 *
		 * @param value Whether to accept them.  Default <jk>false</jk>.
		 * @return This object.
		 */
		public Builder allowUnknownFunctions(boolean value) {
			allowUnknownFunctions = value;
			return this;
		}

		/**
		 * Builds the catalog.
		 *
		 * @return A new catalog.
		 */
		public TemplateCatalog build() { return new TemplateCatalog(this); }
	}

	boolean hasName(String name) { return fields.contains(name) || lists.containsKey(name); }

	boolean isList(String name) { return lists.containsKey(name); }

	/** @return The row catalog, or <jk>null</jk> if the list is undeclared or has none. */
	FieldCatalog rowFields(String list) { return lists.get(list); }

	boolean allowUnknownFunctions() { return allowUnknownFunctions; }
}
