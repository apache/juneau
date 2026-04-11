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

import java.util.function.Supplier;

/**
 * Represents an HTTP <c>Content-Length</c> header.
 *
 * <p>
 * The size of the entity-body in bytes.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
public class ContentLength extends HttpLongHeader {

	public static final String NAME = "Content-Length";

	public ContentLength(String value) {
		super(NAME, value);
	}

	public ContentLength(long value) {
		super(NAME, Long.valueOf(value));
	}

	public ContentLength(Long value) {
		super(NAME, value);
	}

	private ContentLength(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static ContentLength of(String value) {
		return new ContentLength(value);
	}

	public static ContentLength of(long value) {
		return new ContentLength(value);
	}

	public static ContentLength of(Long value) {
		return new ContentLength(value);
	}

	public static ContentLength ofLazyWire(Supplier<String> supplier) {
		return new ContentLength(supplier, LAZY_WIRE_STRING);
	}

	public static ContentLength ofLazyParsed(Supplier<Long> supplier) {
		return new ContentLength(supplier, LAZY_LONG);
	}
}
