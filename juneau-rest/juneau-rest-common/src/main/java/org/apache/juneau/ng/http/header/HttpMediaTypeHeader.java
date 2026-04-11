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

import org.apache.http.*;
import org.apache.juneau.*;

/**
 * Base type for headers whose value is a single {@link MediaType} (e.g. {@code Content-Type}).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpMediaTypeHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_MEDIA_TYPE = 1;

	private final MediaType cachedForStringOrDirect;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpMediaTypeHeader(String name, String value) {
		super(name, value);
		this.cachedForStringOrDirect = parseMediaType(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpMediaTypeHeader(String name, MediaType value) {
		super(name, value == null ? null : value.toString());
		this.cachedForStringOrDirect = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpMediaTypeHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> {
				var m = ((Supplier<MediaType>) supplier).get();
				return m == null ? null : m.toString();
			});
		this.cachedForStringOrDirect = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<MediaType> asMediaType() {
		return opt(toMediaType());
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

	public MediaType orElse(MediaType other) {
		var x = toMediaType();
		return nn(x) ? x : other;
	}

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
