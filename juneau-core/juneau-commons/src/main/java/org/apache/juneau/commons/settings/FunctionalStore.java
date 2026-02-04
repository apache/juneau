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
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.function.Snippet;

/**
 * A writable {@link SettingStore} implementation created from functional interfaces.
 *
 * <p>
 * This class allows you to create writable setting stores from lambda expressions or method references,
 * making it easy to wrap existing property systems (e.g., custom configuration systems) as
 * {@link SettingStore} instances.
 *
 * <h5 class='section'>Return Value Semantics:</h5>
 * <ul class='spaced-list'>
 * 	<li>If the reader function returns <c>null</c>, this store returns <c>null</c> (key doesn't exist).
 * 	<li>If the reader function returns a non-null value, this store returns <c>Optional.of(value)</c>.
 * </ul>
 *
 * <p>
 * Note: This store cannot distinguish between a key that doesn't exist and a key that exists with a null value,
 * since the reader function only returns a <c>String</c>. If you need to distinguish these cases, use {@link MapStore} instead.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a writable functional store</jc>
 * 	FunctionalStore <jv>store</jv> = FunctionalStore.<jsf>of</jsf>(
 * 		System::getProperty,  <jc>// reader</jc>
 * 		(k, v) -&gt; System.setProperty(k, v),  <jc>// writer</jc>
 * 		k -&gt; System.clearProperty(k),  <jc>// unset</jc>
 * 		() -&gt; { <jc>// clear</jc>
 * 			<jc>// Clear all properties logic</jc>
 * 		}
 * 	);
 *
 * 	<jc>// Use it</jc>
 * 	<jv>store</jv>.set(<js>"my.property"</js>, <js>"value"</js>);
 * 	Optional&lt;String&gt; <jv>value</jv> = <jv>store</jv>.get(<js>"my.property"</js>);
 * 	<jv>store</jv>.unset(<js>"my.property"</js>);
 * </p>
 */
@SuppressWarnings("java:S115")
public class FunctionalStore implements SettingStore {

	// Argument name constants for assertArgNotNull
	private static final String ARG_reader = "reader";
	private static final String ARG_writer = "writer";
	private static final String ARG_unsetter = "unsetter";
	private static final String ARG_clearer = "clearer";

	private final Function<String, String> reader;
	private final BiConsumer<String, String> writer;
	private final Consumer<String> unsetter;
	private final Snippet clearer;

	/**
	 * Creates a new writable functional store.
	 *
	 * @param reader The function to read property values. Must not be <c>null</c>.
	 * @param writer The function to write property values. Must not be <c>null</c>.
	 * @param unsetter The function to remove property values. Must not be <c>null</c>.
	 * @param clearer The snippet to clear all property values. Must not be <c>null</c>.
	 */
	public FunctionalStore(
		Function<String, String> reader,
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

	/**
	 * Returns a setting by applying the reader function.
	 *
	 * <p>
	 * If the reader function returns <c>null</c>, this method returns <c>null</c> (indicating the key doesn't exist).
	 * If the reader function returns a non-null value, this method returns <c>Optional.of(value)</c>.
	 *
	 * @param name The property name.
	 * @return The property value, or <c>null</c> if the reader function returns <c>null</c>.
	 */
	@Override
	public Optional<String> get(String name) {
		var v = reader.apply(name);
		return v == null ? null : opt(v);
	}

	/**
	 * Sets a setting by applying the writer function.
	 *
	 * @param name The property name.
	 * @param value The property value, or <c>null</c> to set an empty override.
	 */
	@Override
	public void set(String name, String value) {
		writer.accept(name, value);
	}

	/**
	 * Removes a setting by applying the unsetter function.
	 *
	 * @param name The property name to remove.
	 */
	@Override
	public void unset(String name) {
		unsetter.accept(name);
	}

	/**
	 * Clears all settings by invoking the clearer snippet.
	 *
	 * <p>
	 * If the clearer snippet throws an exception, it will be wrapped in a {@link RuntimeException}.
	 */
	@Override
	public void clear() {
		safe(clearer::run);
	}

	/**
	 * Creates a writable functional store from four functions.
	 *
	 * <p>
	 * This is a convenience factory method for creating writable functional stores.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create from lambdas</jc>
	 * 	FunctionalStore <jv>store</jv> = FunctionalStore.<jsf>of</jsf>(
	 * 		System::getProperty,
	 * 		(k, v) -&gt; System.setProperty(k, v),
	 * 		k -&gt; System.clearProperty(k),
	 * 		() -&gt; { <jc>// Clear all properties</jc> }
	 * 	);
	 * </p>
	 *
	 * @param reader The function to read property values. Must not be <c>null</c>.
	 * @param writer The function to write property values. Must not be <c>null</c>.
	 * @param unsetter The function to remove property values. Must not be <c>null</c>.
	 * @param clearer The snippet to clear all property values. Must not be <c>null</c>.
	 * @return A new writable functional store instance.
	 */
	public static FunctionalStore of(
		Function<String, String> reader,
		BiConsumer<String, String> writer,
		Consumer<String> unsetter,
		Snippet clearer
	) {
		return new FunctionalStore(reader, writer, unsetter, clearer);
	}
}

