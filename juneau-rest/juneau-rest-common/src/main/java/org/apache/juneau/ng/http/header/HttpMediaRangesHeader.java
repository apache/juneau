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

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Base type for headers whose wire format is an <c>Accept</c>-style list of media ranges.
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
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpMediaRangesHeader extends HttpHeaderBean {

	/** Lazy supplier provides the wire string. */
	public static final int LAZY_WIRE_STRING = 0;

	/** Lazy supplier provides {@link MediaRanges} (re-evaluated on each access). */
	public static final int LAZY_MEDIA_RANGES = 1;

	private final MediaRanges cachedForStringOrDirect;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param value Wire value. May be {@code null}.
	 */
	protected HttpMediaRangesHeader(String name, String value) {
		super(name, value);
		this.cachedForStringOrDirect = value == null ? null : MediaRanges.of(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param value Parsed media ranges. May be {@code null}.
	 */
	protected HttpMediaRangesHeader(String name, MediaRanges value) {
		super(name, value == null ? null : value.toString());
		this.cachedForStringOrDirect = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param supplier Lazy supplier; mode {@link #LAZY_WIRE_STRING} or {@link #LAZY_MEDIA_RANGES}. Must not be {@code null}.
	 * @param lazyMode Discriminator for erasure-safe lazy construction.
	 */
	protected HttpMediaRangesHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> {
				var m = ((Supplier<MediaRanges>) supplier).get();
				return m == null ? null : m.toString();
			});
		this.cachedForStringOrDirect = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	/**
	 * @return Parsed {@link MediaRanges}, or {@code null} if the wire value is absent.
	 */
	public Optional<MediaRanges> asMediaRanges() {
		return opt(toMediaRanges());
	}

	/**
	 * @param index Range index.
	 * @return The {@link MediaRange} at {@code index}, or {@code null} if out of range.
	 */
	public MediaRange getRange(int index) {
		var x = toMediaRanges();
		return x == null ? null : x.getRange(index);
	}

	/**
	 * @param part Subtype fragment to find (e.g. {@code "activity"} for {@code text/json+activity}).
	 * @return {@code true} if any range's subtype contains the fragment.
	 */
	public boolean hasSubtypePart(String part) {
		var x = toMediaRanges();
		return x != null && x.hasSubtypePart(part);
	}

	/**
	 * @param mediaTypes Candidate types, in preference order.
	 * @return Index of the best matching type, or {@code -1}.
	 */
	public int match(List<? extends MediaType> mediaTypes) {
		var x = toMediaRanges();
		return x == null ? -1 : x.match(mediaTypes);
	}

	/**
	 * @param other Fallback when no parsed value is present.
	 * @return Parsed ranges or {@code other}.
	 */
	public MediaRanges orElse(MediaRanges other) {
		var x = toMediaRanges();
		return nn(x) ? x : other;
	}

	/**
	 * @return Parsed {@link MediaRanges}, or {@code null} if the wire value is absent.
	 */
	public MediaRanges toMediaRanges() {
		if (lazyMode == LAZY_MEDIA_RANGES)
			return ((Supplier<MediaRanges>) lazySupplier).get();
		if (cachedForStringOrDirect != null)
			return cachedForStringOrDirect;
		var v = super.getValue();
		return v == null ? null : MediaRanges.of(v);
	}
}
