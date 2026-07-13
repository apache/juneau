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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.function.*;

/**
 * Represents an HTTP <c>Content-Range</c> header.
 *
 * <p>
 * Where in a full body message this partial message belongs.
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
public class ContentRange extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "Content-Range";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public ContentRange(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public ContentRange(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ContentRange of(String value) {
		return new ContentRange(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ContentRange of(Supplier<String> valueSupplier) {
		return new ContentRange(valueSupplier);
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
	 * Fluent builder for assembling an HTTP <c>Content-Range</c> header value.
	 *
	 * <p>
	 * Composes the three fields defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7233#section-4.2'>RFC 7233 §4.2</a> — the range unit
	 * (default <c>bytes</c>), the resolved <c>first-last</c> byte range (or <c>*</c> for an unsatisfied range), and the
	 * <c>complete-length</c> (or <c>*</c> when unknown) — into forms such as <c>bytes 0-1023/2048</c>,
	 * <c>bytes 0-1023/&#42;</c>, and <c>bytes &#42;/2048</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = ContentRange.<jsm>create</jsm>()
	 * 		.range(0, 1023)
	 * 		.length(2048)
	 * 		.build();
	 * 	<jc>// =&gt; "bytes 0-1023/2048"</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link ContentRange}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7233'>RFC 7233 - HTTP/1.1 Range Requests</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private String unit = "bytes";
		private Long start;
		private Long end;
		private boolean rangeUnsatisfied;
		private Long completeLength;
		private boolean lengthUnknown;

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
				throw iaex("range unit must not be blank");
			unit = v;
			return this;
		}

		/**
		 * Sets the resolved <c>first-last</c> byte range.
		 *
		 * <p>
		 * Clears any previously-set {@link #unsatisfiedRange() unsatisfied} marker.
		 *
		 * @param startValue The first byte position (inclusive). Must be non-negative.
		 * @param endValue The last byte position (inclusive). Must be greater than or equal to {@code startValue}.
		 * @return This object.
		 * @throws IllegalArgumentException If the positions are negative or out of order.
		 */
		public Builder range(long startValue, long endValue) {
			if (startValue < 0)
				throw iaex("range start must be non-negative: {0}", startValue);
			if (endValue < startValue)
				throw iaex("range end {0} must be >= start {1}", endValue, startValue);
			start = startValue;
			end = endValue;
			rangeUnsatisfied = false;
			return this;
		}

		/**
		 * Marks the range portion as unsatisfied, rendering it as <c>*</c> (used with a known complete length).
		 *
		 * @return This object.
		 */
		public Builder unsatisfiedRange() {
			rangeUnsatisfied = true;
			start = null;
			end = null;
			return this;
		}

		/**
		 * Sets the <c>complete-length</c> (the total size of the representation).
		 *
		 * <p>
		 * Clears any previously-set {@link #unknownLength() unknown-length} marker.
		 *
		 * @param value The complete length in units. Must be non-negative.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is negative.
		 */
		public Builder length(long value) {
			if (value < 0)
				throw iaex("complete-length must be non-negative: {0}", value);
			completeLength = value;
			lengthUnknown = false;
			return this;
		}

		/**
		 * Marks the complete length as unknown, rendering it as <c>*</c>.
		 *
		 * @return This object.
		 */
		public Builder unknownLength() {
			lengthUnknown = true;
			completeLength = null;
			return this;
		}

		/**
		 * Builds the rendered <c>Content-Range</c> header value.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			var rangePart = rangeUnsatisfied || start == null ? "*" : start + "-" + end;
			var lengthPart = lengthUnknown || completeLength == null ? "*" : completeLength.toString();
			return unit + " " + rangePart + "/" + lengthPart;
		}

		/**
		 * Builds a {@link ContentRange} header bean directly from this builder.
		 *
		 * @return A {@link ContentRange} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public ContentRange toHeader() {
			return ContentRange.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
