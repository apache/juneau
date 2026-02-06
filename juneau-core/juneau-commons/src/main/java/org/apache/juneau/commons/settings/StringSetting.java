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

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * A specialized {@link Setting} for string values that provides convenience methods for type conversion.
 *
 * <p>
 * This class extends {@link Setting} with type-specific conversion methods such as {@link #asInteger()},
 * {@link #asBoolean()}, {@link #asCharset()}, etc.
 */
@SuppressWarnings("java:S115")
public class StringSetting extends Setting<String> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_mapper = "mapper";
	private static final String ARG_predicate = "predicate";
	private static final String ARG_c = "c";

	/**
	 * Creates a new StringSetting from a Settings instance and a Supplier.
	 *
	 * @param settings The Settings instance that created this setting. Must not be <jk>null</jk>.
	 * @param supplier The supplier that provides the string value. Must not be <jk>null</jk>.
	 */
	public StringSetting(Settings settings, Supplier<String> supplier) {
		super(settings, supplier);
	}

	/**
	 * If a value is present, applies the provided mapping function to it and returns a StringSetting describing the result.
	 *
	 * <p>
	 * This method is specifically for String-to-String mappings. For mappings to other types, use the inherited {@link #map(Function)} method.
	 *
	 * <p>
	 * The returned StringSetting maintains its own cache, independent of this supplier.
	 * Resetting the mapped supplier does not affect this supplier, and vice versa.
	 *
	 * @param mapper A mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return A StringSetting describing the result of applying a mapping function to the value of this StringSetting, if a value is present, otherwise an empty StringSetting.
	 */
	public StringSetting mapString(Function<String, String> mapper) {
		assertArgNotNull(ARG_mapper, mapper);
		return new StringSetting(getSettings(), () -> {
			String value = get();
			return nn(value) ? mapper.apply(value) : null;
		});
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns a StringSetting describing the value, otherwise returns an empty StringSetting.
	 *
	 * <p>
	 * The returned StringSetting maintains its own cache, independent of this supplier.
	 * Resetting the filtered supplier does not affect this supplier, and vice versa.
	 *
	 * @param predicate A predicate to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return A StringSetting describing the value of this StringSetting if a value is present and the value matches the given predicate, otherwise an empty StringSetting.
	 */
	@Override
	public StringSetting filter(Predicate<? super String> predicate) {
		assertArgNotNull(ARG_predicate, predicate);
		return new StringSetting(getSettings(), () -> {
			String value = get();
			return (nn(value) && predicate.test(value)) ? value : null;
		});
	}

	/**
	 * Converts the string value to an Integer.
	 *
	 * <p>
	 * The property value is parsed using {@link Integer#valueOf(String)}. If the property is not found
	 * or cannot be parsed as an integer, returns {@link Optional#empty()}.
	 *
	 * @return The property value as an Integer, or {@link Optional#empty()} if not found or not a valid integer.
	 */
	public Setting<Integer> asInteger() {
		return map(v -> safeOrNull(() -> Integer.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to a Long.
	 *
	 * <p>
	 * The property value is parsed using {@link Long#valueOf(String)}. If the property is not found
	 * or cannot be parsed as a long, returns {@link Optional#empty()}.
	 *
	 * @return The property value as a Long, or {@link Optional#empty()} if not found or not a valid long.
	 */
	public Setting<Long> asLong() {
		return map(v -> safeOrNull(() -> Long.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to a Boolean.
	 *
	 * <p>
	 * The property value is parsed using {@link Boolean#parseBoolean(String)}, which returns <c>true</c>
	 * if the value is (case-insensitive) "true", otherwise <c>false</c>. Note that this method will
	 * return <c>Optional.of(false)</c> for any non-empty value that is not "true", and
	 * {@link Optional#empty()} only if the property is not set.
	 *
	 * @return The property value as a Boolean, or {@link Optional#empty()} if not found.
	 */
	public Setting<Boolean> asBoolean() {
		return map(Boolean::parseBoolean);
	}

	/**
	 * Converts the string value to a Double.
	 *
	 * <p>
	 * The property value is parsed using {@link Double#valueOf(String)}. If the property is not found
	 * or cannot be parsed as a double, returns {@link Optional#empty()}.
	 *
	 * @return The property value as a Double, or {@link Optional#empty()} if not found or not a valid double.
	 */
	public Setting<Double> asDouble() {
		return map(v -> safeOrNull(() -> Double.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to a Float.
	 *
	 * <p>
	 * The property value is parsed using {@link Float#valueOf(String)}. If the property is not found
	 * or cannot be parsed as a float, returns {@link Optional#empty()}.
	 *
	 * @return The property value as a Float, or {@link Optional#empty()} if not found or not a valid float.
	 */
	public Setting<Float> asFloat() {
		return map(v -> safeOrNull(() -> Float.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to a File.
	 *
	 * <p>
	 * The property value is converted to a {@link File} using the {@link File#File(String)} constructor.
	 * If the property is not found, returns {@link Optional#empty()}. Note that this method does not
	 * validate that the file path is valid or that the file exists.
	 *
	 * @return The property value as a File, or {@link Optional#empty()} if not found.
	 */
	public Setting<File> asFile() {
		return map(File::new);
	}

	/**
	 * Converts the string value to a Path.
	 *
	 * <p>
	 * The property value is converted to a {@link Path} using {@link Paths#get(String, String...)}.
	 * If the property is not found or the path string is invalid, returns {@link Optional#empty()}.
	 *
	 * @return The property value as a Path, or {@link Optional#empty()} if not found or not a valid path.
	 */
	public Setting<Path> asPath() {
		return map(v -> safeOrNull(() -> Paths.get(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to a URI.
	 *
	 * <p>
	 * The property value is converted to a {@link URI} using {@link URI#create(String)}.
	 * If the property is not found or the URI string is invalid, returns {@link Optional#empty()}.
	 *
	 * @return The property value as a URI, or {@link Optional#empty()} if not found or not a valid URI.
	 */
	public Setting<URI> asURI() {
		return map(v -> safeOrNull(() -> URI.create(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to a Charset.
	 *
	 * <p>
	 * The property value is converted to a {@link Charset} using {@link Charset#forName(String)}.
	 * If the property is not found or the charset name is not supported, returns {@link Optional#empty()}.
	 *
	 * @return The property value as a Charset, or {@link Optional#empty()} if not found or not a valid charset.
	 */
	public Setting<Charset> asCharset() {
		return map(v -> safeOrNull(() -> Charset.forName(v))).filter(Objects::nonNull);
	}

	/**
	 * Converts the string value to the specified type using the Settings type conversion functions.
	 *
	 * <p>
	 * The property value is converted using {@link Settings#toType(String, Class)}. If the property is not found
	 * or cannot be converted to the specified type, returns {@link Optional#empty()}.
	 *
	 * @param <T> The target type.
	 * @param c The target class. Must not be <jk>null</jk>.
	 * @return The property value as the specified type, or {@link Optional#empty()} if not found or not a valid conversion.
	 */
	public <T> Setting<T> asType(Class<T> c) {
		assertArgNotNull(ARG_c, c);
		return map(v -> getSettings().toType(v, c)).filter(Objects::nonNull);
	}
}
