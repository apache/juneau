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

import java.util.Optional;
import java.util.function.*;

import org.apache.juneau.commons.function.ResettableSupplier;

/**
 * A resettable supplier that provides convenience methods for type conversion.
 *
 * <p>
 * This class extends {@link ResettableSupplier} to provide methods to convert the string value
 * to various types, similar to the {@link StringSetting#asInteger()}, {@link StringSetting#asBoolean()}, etc. methods.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	StringSetting <jv>setting</jv> = Settings.<jsf>get</jsf>().setting(<js>"my.property"</js>);
 * 	Setting&lt;Integer&gt; <jv>intValue</jv> = <jv>setting</jv>.asInteger();
 * 	Setting&lt;Boolean&gt; <jv>boolValue</jv> = <jv>setting</jv>.asBoolean();
 * 	Setting&lt;Charset&gt; <jv>charset</jv> = <jv>setting</jv>.asCharset();
 *
 * 	<jc>// Reset the cache to force recomputation</jc>
 * 	<jv>setting</jv>.reset();
 * </p>
 *
 * @param <T> The type of value supplied.
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class Setting<T> extends ResettableSupplier<T> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_supplier = "supplier";
	private static final String ARG_settings = "settings";
	private static final String ARG_mapper = "mapper";
	private static final String ARG_predicate = "predicate";
	private final Settings settings;

	/**
	 * Creates a new Setting from a Settings instance and a Supplier.
	 *
	 * @param settings The Settings instance that created this setting. Must not be <jk>null</jk>.
	 * @param supplier The supplier that provides the value. Must not be <jk>null</jk>.
	 */
	public Setting(Settings settings, Supplier<T> supplier) {
		super(assertArgNotNull(ARG_supplier, supplier));
		this.settings = assertArgNotNull(ARG_settings, settings);
	}

	/**
	 * Returns the Settings instance that created this setting.
	 *
	 * @return The Settings instance.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Returns the underlying Optional&lt;T&gt;.
	 *
	 * <p>
	 * <b>Note:</b> The returned {@link Optional} is a snapshot-in-time of the current value.
	 * Resetting this {@link Setting} will not affect the returned {@link Optional} instance.
	 * To get an updated value after resetting, call this method again.
	 *
	 * @return The optional value.
	 */
	public Optional<T> asOptional() {
		return opt(get());
	}

	/**
	 * If a value is present, applies the provided mapping function to it and returns a Setting describing the result.
	 *
	 * <p>
	 * The returned Setting maintains its own cache, independent of this supplier.
	 * Resetting the mapped supplier does not affect this supplier, and vice versa.
	 *
	 * @param <U> The type of the result of the mapping function.
	 * @param mapper A mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return A Setting describing the result of applying a mapping function to the value of this Setting, if a value is present, otherwise an empty Setting.
	 */
	@Override
	public <U> Setting<U> map(Function<? super T, ? extends U> mapper) {
		assertArgNotNull(ARG_mapper, mapper);
		return new Setting<>(settings, () -> {
			T value = get();
			return nn(value) ? mapper.apply(value) : null;
		});
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns a Setting describing the value, otherwise returns an empty Setting.
	 *
	 * <p>
	 * The returned Setting maintains its own cache, independent of this supplier.
	 * Resetting the filtered supplier does not affect this supplier, and vice versa.
	 *
	 * @param predicate A predicate to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return A Setting describing the value of this Setting if a value is present and the value matches the given predicate, otherwise an empty Setting.
	 */
	@Override
	public Setting<T> filter(Predicate<? super T> predicate) {
		assertArgNotNull(ARG_predicate, predicate);
		return new Setting<>(settings, () -> {
			T value = get();
			return (nn(value) && predicate.test(value)) ? value : null;
		});
	}
}

