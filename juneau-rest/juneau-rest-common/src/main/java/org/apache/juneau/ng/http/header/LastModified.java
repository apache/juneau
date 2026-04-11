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

/**
 * Represents an HTTP <c>Last-Modified</c> header.
 *
 * <p>
 * The last modification date of the origin resource.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
import java.time.*;
import java.util.function.*;

/**
 * @since 9.2.1
 */
public class LastModified extends HttpDateHeader {

	public static final String NAME = "Last-Modified";

	public LastModified(String value) {
		super(NAME, value);
	}

	public LastModified(ZonedDateTime value) {
		super(NAME, value);
	}

	private LastModified(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static LastModified of(String value) {
		return new LastModified(value);
	}

	public static LastModified of(ZonedDateTime value) {
		return new LastModified(value);
	}

	public static LastModified ofLazyWire(Supplier<String> supplier) {
		return new LastModified(supplier, LAZY_WIRE_STRING);
	}

	public static LastModified ofLazyParsed(Supplier<ZonedDateTime> supplier) {
		return new LastModified(supplier, LAZY_ZONED_DATE_TIME);
	}
}
