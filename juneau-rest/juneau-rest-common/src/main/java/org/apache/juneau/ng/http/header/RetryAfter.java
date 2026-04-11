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
package org.apache.juneau.ng.http.header;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.rex;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.Optional;
import java.util.function.*;

/**
 * Represents an HTTP <c>Retry-After</c> header.
 *
 * <p>
 * Delay before retrying, as a delta-seconds integer or an HTTP-date.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160" // equals() on HttpHeaderBean uses name + getValue()
})
public class RetryAfter extends HttpHeaderBean {

	/** The header name */
	public static final String NAME = "Retry-After";

	private final Integer delaySeconds;
	private final ZonedDateTime retryAt;
	private final Supplier<?> supplier;

	/**
	 * Constructor with delay seconds.
	 *
	 * @param value Seconds to wait. May be <jk>null</jk>.
	 */
	public RetryAfter(Integer value) {
		super(NAME, (String)null);
		this.delaySeconds = value;
		this.retryAt = null;
		this.supplier = null;
	}

	/**
	 * Constructor with a wire string (integer or HTTP-date).
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public RetryAfter(String value) {
		super(NAME, (String)null);
		if (isNumeric(value)) {
			this.delaySeconds = Integer.parseInt(value);
			this.retryAt = null;
		} else {
			this.delaySeconds = null;
			this.retryAt = e(value) ? null : parseHttpDate(value);
		}
		this.supplier = null;
	}

	/**
	 * Constructor with a lazy supplier of {@link Integer} or {@link ZonedDateTime}.
	 *
	 * @param valueSupplier Supplier for the value. Must not be <jk>null</jk>.
	 */
	public RetryAfter(Supplier<?> valueSupplier) {
		super(NAME, (String)null);
		this.delaySeconds = null;
		this.retryAt = null;
		this.supplier = valueSupplier;
	}

	/**
	 * Constructor with an HTTP-date.
	 *
	 * @param value The retry time. May be <jk>null</jk>.
	 */
	public RetryAfter(ZonedDateTime value) {
		super(NAME, (String)null);
		this.delaySeconds = null;
		this.retryAt = value;
		this.supplier = null;
	}

	/**
	 * @return The value as seconds when encoded as a delta-seconds integer.
	 */
	public Optional<Integer> asInteger() {
		if (nn(supplier)) {
			var o = supplier.get();
			return opt(o instanceof Integer o2 ? o2 : null);
		}
		return opt(delaySeconds);
	}

	/**
	 * @return The value as {@link ZonedDateTime} when encoded as an HTTP-date.
	 */
	public Optional<ZonedDateTime> asZonedDateTime() {
		if (nn(supplier)) {
			var o = supplier.get();
			return opt(o instanceof ZonedDateTime o2 ? o2 : null);
		}
		return opt(retryAt);
	}

	@Override /* HttpHeader */
	public String getValue() {
		if (nn(supplier)) {
			var o = supplier.get();
			if (o == null)
				return null;
			if (o instanceof Integer o2)
				return o2.toString();
			if (o instanceof ZonedDateTime o2)
				return RFC_1123_DATE_TIME.format(o2);
			throw rex("Invalid object type returned by supplier: {0}", cn(o));
		}
		if (nn(delaySeconds))
			return delaySeconds.toString();
		if (nn(retryAt))
			return RFC_1123_DATE_TIME.format(retryAt);
		return null;
	}

	/**
	 * @param value Delay seconds. May be <jk>null</jk>.
	 * @return A new instance or <jk>null</jk>.
	 */
	public static RetryAfter of(Integer value) {
		return value == null ? null : new RetryAfter(value);
	}

	/**
	 * @param value Wire string. May be <jk>null</jk>.
	 * @return A new instance or <jk>null</jk>.
	 */
	public static RetryAfter of(String value) {
		return value == null ? null : new RetryAfter(value);
	}

	/**
	 * @param valueSupplier Supplier of {@link Integer} or {@link ZonedDateTime}. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static RetryAfter of(Supplier<?> valueSupplier) {
		return new RetryAfter(valueSupplier);
	}

	/**
	 * @param value HTTP-date. May be <jk>null</jk>.
	 * @return A new instance or <jk>null</jk>.
	 */
	public static RetryAfter of(ZonedDateTime value) {
		return value == null ? null : new RetryAfter(value);
	}

	private static ZonedDateTime parseHttpDate(String value) {
		return ZonedDateTime.from(RFC_1123_DATE_TIME.parse(value)).truncatedTo(SECONDS);
	}
}
