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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.http.*;

/**
 * Base type for headers whose value is a single {@link MediaType} (e.g. {@code Content-Type}).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpMediaTypeHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_MEDIA_TYPE = 1;

	private final MediaType cachedForStringOrDirect;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Wire value. Can be <jk>null</jk>, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpMediaTypeHeader(String name, String value) {
		super(name, value);
		this.cachedForStringOrDirect = parseMediaType(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value The media type value. Can be <jk>null</jk>, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpMediaTypeHeader(String name, MediaType value) {
		super(name, value == null ? null : value.toString());
		this.cachedForStringOrDirect = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param supplier The lazy value supplier. Must not be <jk>null</jk>.
	 * @param lazyMode Either {@link #LAZY_WIRE_STRING} or {@link #LAZY_MEDIA_TYPE}.
	 */
	protected HttpMediaTypeHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> {
				var m = ((Supplier<MediaType>) supplier).get();
				if (m == null)
					return null;
				return m.toString();
			});
		this.cachedForStringOrDirect = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<MediaType> asMediaType() {
		return o(toMediaType());
	}

	public String getParameter(String name) {
		return orElse(MediaType.EMPTY).getParameter(name);
	}

	public List<NameValuePair> getParameters() {
		return orElse(MediaType.EMPTY).getParameters();
	}

	public final String getSubType() {
		return orElse(MediaType.EMPTY).getSubType();
	}

	public final List<String> getSubTypes() {
		return orElse(MediaType.EMPTY).getSubTypes();
	}

	public final String getType() {
		return orElse(MediaType.EMPTY).getType();
	}

	public final boolean hasSubType(String value) {
		return orElse(MediaType.EMPTY).hasSubType(value);
	}

	public final boolean isMetaSubtype() {
		return orElse(MediaType.EMPTY).isMetaSubtype();
	}

	/**
	 * Returns the index of the best-matching media type in the specified list.
	 *
	 * @param mediaTypes The media types to match against. Must not be <jk>null</jk>.
	 * @return The index of the best match, or <c>-1</c> if no suitable match was found.
	 */
	public int match(List<MediaType> mediaTypes) {
		var matchQuant = 0;
		var matchIndex = -1;
		for (var i = 0; i < mediaTypes.size(); i++) {
			var mt = mediaTypes.get(i);
			var matchQuant2 = mt.match(orElse(MediaType.EMPTY), true);
			if (matchQuant2 > matchQuant) {
				matchQuant = matchQuant2;
				matchIndex = i;
			}
		}
		return matchIndex;
	}

	public final int match(MediaType o, boolean allowExtraSubTypes) {
		return orElse(MediaType.EMPTY).match(o, allowExtraSubTypes);
	}

	/**
	 * Returns the parsed value of this header, or the specified default if unset.
	 *
	 * @param other The default value. Can be <jk>null</jk>.
	 * @return The parsed value, or <c>other</c> if the value is unset. Can be <jk>null</jk> if <c>other</c> is <jk>null</jk>.
	 */
	public MediaType orElse(MediaType other) {
		var x = toMediaType();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the parsed value of this header.
	 *
	 * @return The parsed value, or <jk>null</jk> if the value is unset.
	 */
	public MediaType toMediaType() {
		if (lazyMode == LAZY_MEDIA_TYPE)
			return ((Supplier<MediaType>) lazySupplier).get();
		if (cachedForStringOrDirect != null)
			return cachedForStringOrDirect;
		return parseMediaType(super.getValue());
	}

	private static MediaType parseMediaType(String value) {
		if (!nn(value))
			return null;
		var v = value;
		var i = v.indexOf(',');
		if (i != -1)
			v = v.substring(i + 1);
		return MediaType.of(v);
	}
}
