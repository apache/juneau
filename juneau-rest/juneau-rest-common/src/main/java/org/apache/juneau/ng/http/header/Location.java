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
 * Represents an HTTP <c>Location</c> header.
 *
 * <p>
 * The URI for redirection or creation of a new resource.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
import java.net.*;
import java.util.function.*;

/**
 * @since 9.2.1
 */
public class Location extends HttpUriHeader {

	public static final String NAME = "Location";

	public Location(String value) {
		super(NAME, value);
	}

	public Location(URI value) {
		super(NAME, value);
	}

	private Location(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static Location of(String value) {
		return new Location(value);
	}

	public static Location of(URI value) {
		return new Location(value);
	}

	public static Location ofLazyWire(Supplier<String> supplier) {
		return new Location(supplier, LAZY_WIRE_STRING);
	}

	public static Location ofLazyParsed(Supplier<URI> supplier) {
		return new Location(supplier, LAZY_URI);
	}
}
