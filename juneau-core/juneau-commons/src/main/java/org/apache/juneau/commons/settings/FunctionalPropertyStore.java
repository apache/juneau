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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.function.*;

import org.apache.juneau.commons.function.*;

/**
 * A writable {@link PropertyStore} implementation created from functional interfaces.
 */
@SuppressWarnings({
	"java:S115" // Constants use ARG_lowerCamel convention to match the corresponding constructor parameter name (e.g., ARG_reader → reader).
})
public class FunctionalPropertyStore implements PropertyStore {

	private static final String ARG_reader = "reader";
	private static final String ARG_writer = "writer";
	private static final String ARG_unsetter = "unsetter";
	private static final String ARG_clearer = "clearer";

	private final UnaryOperator<String> reader;
	private final BiConsumer<String, String> writer;
	private final Consumer<String> unsetter;
	private final Snippet clearer;

	/**
	 * Constructor.
	 *
	 * @param reader The function used to read property values by name.  Must not be <jk>null</jk>.
	 * @param writer The function used to write property values.  Must not be <jk>null</jk>.
	 * @param unsetter The function used to remove a property by name.  Must not be <jk>null</jk>.
	 * @param clearer The function used to clear all properties.  Must not be <jk>null</jk>.
	 */
	public FunctionalPropertyStore(
		UnaryOperator<String> reader,
		BiConsumer<String, String> writer,
		Consumer<String> unsetter,
		Snippet clearer
	) {
		assertArgNotNull(ARG_reader, reader);
		assertArgNotNull(ARG_writer, writer);
		assertArgNotNull(ARG_unsetter, unsetter);
		assertArgNotNull(ARG_clearer, clearer);
		this.reader = reader;
		this.writer = writer;
		this.unsetter = unsetter;
		this.clearer = clearer;
	}

	@Override
	public PropertyLookupResult get(String name) {
		var v = reader.apply(name);
		return v == null ? PropertyLookupResult.missing() : PropertyLookupResult.present(o(v));
	}

	@Override
	public void set(String name, String value) {
		writer.accept(name, value);
	}

	@Override
	public void unset(String name) {
		unsetter.accept(name);
	}

	@Override
	public void clear() {
		safe(clearer::run);
	}

	/**
	 * Creates a new store from the specified functions.
	 *
	 * @param reader The function used to read property values by name.  Must not be <jk>null</jk>.
	 * @param writer The function used to write property values.  Must not be <jk>null</jk>.
	 * @param unsetter The function used to remove a property by name.  Must not be <jk>null</jk>.
	 * @param clearer The function used to clear all properties.  Must not be <jk>null</jk>.
	 * @return A new store.
	 */
	public static FunctionalPropertyStore of(
		UnaryOperator<String> reader,
		BiConsumer<String, String> writer,
		Consumer<String> unsetter,
		Snippet clearer
	) {
		return new FunctionalPropertyStore(reader, writer, unsetter, clearer);
	}
}
