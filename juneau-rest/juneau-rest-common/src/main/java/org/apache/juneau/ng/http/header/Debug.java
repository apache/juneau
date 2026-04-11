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
 * Represents an HTTP <c>Debug</c> header.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
import java.util.function.*;

/**
 * @since 9.2.1
 */
public class Debug extends HttpBooleanHeader {

	public static final String NAME = "Debug";

	public Debug(String value) {
		super(NAME, value);
	}

	public Debug(boolean value) {
		super(NAME, Boolean.valueOf(value));
	}

	public Debug(Boolean value) {
		super(NAME, value);
	}

	private Debug(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static Debug of(String value) {
		return new Debug(value);
	}

	public static Debug of(boolean value) {
		return new Debug(value);
	}

	public static Debug of(Boolean value) {
		return new Debug(value);
	}

	public static Debug ofLazyWire(Supplier<String> supplier) {
		return new Debug(supplier, LAZY_WIRE_STRING);
	}

	public static Debug ofLazyParsed(Supplier<Boolean> supplier) {
		return new Debug(supplier, LAZY_BOOLEAN);
	}
}
