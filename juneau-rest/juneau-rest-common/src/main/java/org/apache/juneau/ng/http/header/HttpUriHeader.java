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

import java.net.*;
import java.util.*;
import java.util.function.*;

/**
 * Base for single-URI headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpUriHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_URI = 1;

	private final URI cachedUri;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpUriHeader(String name, String value) {
		super(name, value);
		this.cachedUri = e(value) ? null : URI.create(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpUriHeader(String name, URI value) {
		super(name, s(value));
		this.cachedUri = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpUriHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> s(((Supplier<URI>) supplier).get()));
		this.cachedUri = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<URI> asUri() {
		return opt(toUri());
	}

	@Override
	public String getValue() {
		return s(toUri());
	}

	public URI orElse(URI other) {
		var x = toUri();
		return nn(x) ? x : other;
	}

	public URI toUri() {
		if (lazyMode == LAZY_URI)
			return ((Supplier<URI>) lazySupplier).get();
		if (cachedUri != null)
			return cachedUri;
		var v = super.getValue();
		return e(v) ? null : URI.create(v);
	}
}
