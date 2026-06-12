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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;
import java.util.function.*;

/**
 * Represents an HTTP <c>Range</c> header.
 *
 * <p>
 * Request only part of an entity. Bytes are numbered from 0.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class Range extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "Range";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public Range(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public Range(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static Range of(String value) {
		return new Range(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static Range of(Supplier<String> valueSupplier) {
		return new Range(valueSupplier);
	}

	/**
	 * Creates a new {@link Builder} defaulting to the <c>bytes</c> range unit.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder create() {
		return new Builder();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Inner types
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for assembling an HTTP <c>Range</c> header value.
	 *
	 * <p>
	 * Composes the range unit (default <c>bytes</c>) and one or more range specs defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7233#section-3.1'>RFC 7233 §3.1</a> into the
	 * <c>unit=spec,spec,...</c> wire format.  Typed setters cover the three byte-range-spec forms — a closed range
	 * (<c>0-499</c>) via {@link #range(long, long)}, an open-ended range (<c>500-</c>) via {@link #from(long)}, and a
	 * suffix range (<c>-500</c>) via {@link #suffix(long)} — and {@link #spec(String)} is a generic escape hatch.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = Range.<jsm>create</jsm>()
	 * 		.range(0, 499)
	 * 		.from(9500)
	 * 		.build();
	 * 	<jc>// =&gt; "bytes=0-499,9500-"</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link Range}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7233'>RFC 7233 - HTTP/1.1 Range Requests</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private String unit = "bytes";
		private final List<String> specs = new ArrayList<>();

		/**
		 * Sets the range unit (default <c>bytes</c>).
		 *
		 * @param value The range unit. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
		 */
		public Builder unit(String value) {
			assertArgNotNull("value", value);
			var v = value.trim();
			if (v.isEmpty())
				throw illegalArg("range unit must not be blank");
			unit = v;
			return this;
		}

		/**
		 * Adds a closed byte-range spec of the form <c>start-end</c>.
		 *
		 * @param startValue The first byte position (inclusive). Must be non-negative.
		 * @param endValue The last byte position (inclusive). Must be greater than or equal to {@code startValue}.
		 * @return This object.
		 * @throws IllegalArgumentException If the positions are negative or out of order.
		 */
		public Builder range(long startValue, long endValue) {
			if (startValue < 0)
				throw illegalArg("range start must be non-negative: {0}", startValue);
			if (endValue < startValue)
				throw illegalArg("range end {0} must be >= start {1}", endValue, startValue);
			return spec(startValue + "-" + endValue);
		}

		/**
		 * Adds an open-ended byte-range spec of the form <c>start-</c> (from the position to the end).
		 *
		 * @param startValue The first byte position (inclusive). Must be non-negative.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code startValue} is negative.
		 */
		public Builder from(long startValue) {
			if (startValue < 0)
				throw illegalArg("range start must be non-negative: {0}", startValue);
			return spec(startValue + "-");
		}

		/**
		 * Adds a suffix byte-range spec of the form <c>-length</c> (the final {@code length} bytes).
		 *
		 * @param length The number of trailing bytes. Must be non-negative.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code length} is negative.
		 */
		public Builder suffix(long length) {
			if (length < 0)
				throw illegalArg("suffix length must be non-negative: {0}", length);
			return spec("-" + length);
		}

		/**
		 * Generic escape hatch for adding any range spec verbatim.
		 *
		 * @param value The range spec (e.g. <js>"0-499"</js>). Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
		 */
		public Builder spec(String value) {
			assertArgNotNull("value", value);
			var v = value.trim();
			if (v.isEmpty())
				throw illegalArg("range spec must not be blank");
			specs.add(v);
			return this;
		}

		/**
		 * Builds the rendered <c>Range</c> header value.
		 *
		 * <p>
		 * Returns an empty string when no range specs have been registered.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			if (specs.isEmpty())
				return "";
			return unit + "=" + String.join(",", specs);
		}

		/**
		 * Builds a {@link Range} header bean directly from this builder.
		 *
		 * @return A {@link Range} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public Range toHeader() {
			return Range.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
