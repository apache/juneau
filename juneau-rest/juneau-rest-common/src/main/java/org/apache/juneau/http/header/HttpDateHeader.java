/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.header;


import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Base for HTTP-date headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpDateHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_ZONED_DATE_TIME = 1;

	private final ZonedDateTime cachedZdt;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Creates an {@link HttpDateHeader} by parsing the given RFC-1123 wire-format value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param wireValue Wire value (e.g. {@code "Sun, 06 Nov 1994 08:49:37 GMT"}). May be {@code null} or empty.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpDateHeader of(String name, String wireValue) {
		return new HttpDateHeader(name, wireValue);
	}

	/**
	 * Creates an {@link HttpDateHeader} with the given typed value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param typedValue The date value. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpDateHeader of(String name, ZonedDateTime typedValue) {
		return new HttpDateHeader(name, typedValue);
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Wire value. Can be <jk>null</jk> or empty, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpDateHeader(String name, String value) {
		super(name, (String)null);
		this.cachedZdt = ie(value) ? null : parseHttpDate(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value The date value. Can be <jk>null</jk>, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpDateHeader(String name, ZonedDateTime value) {
		super(name, (String)null);
		this.cachedZdt = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param supplier The lazy value supplier. Must not be <jk>null</jk>.
	 * @param lazyMode Either {@link #LAZY_WIRE_STRING} or {@link #LAZY_ZONED_DATE_TIME}.
	 */
	protected HttpDateHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> {
				var z = ((Supplier<ZonedDateTime>) supplier).get();
				if (z == null)
					return null;
				return RFC_1123_DATE_TIME.format(z);
			});
		this.cachedZdt = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<ZonedDateTime> asZonedDateTime() {
		return o(toZonedDateTime());
	}

	/**
	 * Returns the RFC-1123 wire-format value of this header.
	 *
	 * @return The wire value, or <jk>null</jk> if the value is unset.
	 */
	@Override
	public String getValue() {
		if (lazyMode == LAZY_ZONED_DATE_TIME) {
			var x = ((Supplier<ZonedDateTime>) lazySupplier).get();
			return x == null ? null : RFC_1123_DATE_TIME.format(x);
		}
		if (cachedZdt != null)
			return RFC_1123_DATE_TIME.format(cachedZdt);
		return super.getValue();
	}

	/**
	 * Returns the parsed value of this header, or the specified default if unset.
	 *
	 * @param other The default value. Can be <jk>null</jk> to allow a <jk>null</jk> result when the header is unset.
	 * @return The parsed value, or <c>other</c> if the value is unset. Can be <jk>null</jk> if <c>other</c> is <jk>null</jk>.
	 */
	public ZonedDateTime orElse(ZonedDateTime other) {
		var x = toZonedDateTime();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the parsed value of this header.
	 *
	 * @return The parsed value, or <jk>null</jk> if the value is unset.
	 */
	public ZonedDateTime toZonedDateTime() {
		if (lazyMode == LAZY_ZONED_DATE_TIME)
			return ((Supplier<ZonedDateTime>) lazySupplier).get();
		if (cachedZdt != null)
			return cachedZdt;
		var v = super.getValue();
		return ie(v) ? null : parseHttpDate(v);
	}

	private static ZonedDateTime parseHttpDate(String value) {
		return ZonedDateTime.from(RFC_1123_DATE_TIME.parse(value)).truncatedTo(SECONDS);
	}
}
