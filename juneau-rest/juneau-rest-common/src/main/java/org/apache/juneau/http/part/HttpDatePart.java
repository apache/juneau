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
package org.apache.juneau.http.part;

import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * An {@link org.apache.juneau.http.HttpPart} whose value is an ISO-8601 date-time.
 *
 * <p>
 * Mirrors the semantics of {@code BasicDatePart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency. Values are parsed/formatted via
 * {@link java.time.format.DateTimeFormatter#ISO_DATE_TIME}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S2160" // Equality is fully determined by name + getValue() in HttpPartBean; the typed fields are derived from the wire value and add no state to compare
})
public class HttpDatePart extends HttpPartBean {

	/**
	 * Creates an {@link HttpDatePart} with the given name and date value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The date value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpDatePart of(String name, ZonedDateTime value) {
		return new HttpDatePart(name, value);
	}

	/**
	 * Creates an {@link HttpDatePart} by parsing the given ISO-8601 wire value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpDatePart ofString(String name, String value) {
		return new HttpDatePart(name, value);
	}

	/**
	 * Creates an {@link HttpDatePart} with a lazy date supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the date value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpDatePart ofLazy(String name, Supplier<ZonedDateTime> supplier) {
		return new HttpDatePart(name, supplier);
	}

	private final ZonedDateTime typedValue;
	private final Supplier<ZonedDateTime> typedSupplier;

	/**
	 * Constructor accepting an ISO-8601 wire-format string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 */
	public HttpDatePart(String name, String value) {
		super(name, value);
		this.typedValue = e(value) ? null : ZonedDateTime.from(ISO_DATE_TIME.parse(value)).truncatedTo(SECONDS);
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a {@link ZonedDateTime} value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The date value. May be <jk>null</jk>.
	 */
	public HttpDatePart(String name, ZonedDateTime value) {
		super(name, value == null ? null : ISO_DATE_TIME.format(value));
		this.typedValue = value;
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a lazy {@link ZonedDateTime} supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the date value. Must not be <jk>null</jk>.
	 */
	public HttpDatePart(String name, Supplier<ZonedDateTime> supplier) {
		super(name, () -> {
			var v = supplier.get();
			return v == null ? null : ISO_DATE_TIME.format(v);
		});
		this.typedValue = null;
		this.typedSupplier = supplier;
	}

	/**
	 * Returns the value as an {@link Optional}{@code <ZonedDateTime>}.
	 *
	 * @return The date value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<ZonedDateTime> asZonedDateTime() {
		return opt(toZonedDateTime());
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public ZonedDateTime orElse(ZonedDateTime other) {
		var x = toZonedDateTime();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the value as a {@link ZonedDateTime}, or {@code null} if not set.
	 *
	 * @return The date value, possibly {@code null}.
	 */
	public ZonedDateTime toZonedDateTime() {
		if (typedSupplier != null)
			return typedSupplier.get();
		return typedValue;
	}
}
