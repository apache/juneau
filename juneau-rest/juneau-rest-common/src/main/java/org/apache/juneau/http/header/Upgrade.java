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


import java.util.function.*;

/**
 * Represents an HTTP <c>Upgrade</c> header.
 *
 * <p>
 * Protocols the client wants to upgrade to.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack.
 *
 * @since 9.2.1
 */
public class Upgrade extends HttpCsvHeader {

	public static final String NAME = "Upgrade";

	public Upgrade(String value) {
		super(NAME, value);
	}

	public Upgrade(String... values) {
		super(NAME, values);
	}

	private Upgrade(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static Upgrade of(String value) {
		return new Upgrade(value);
	}

	public static Upgrade of(String... values) {
		return new Upgrade(values);
	}

	// Distinct 2-arg factory so two literal String args bind here (returning Upgrade) instead of the inherited
	// fixed-arity HttpCsvHeader.of(String name, String value), which Java overload resolution would otherwise prefer.
	public static Upgrade of(String value1, String value2) {
		return new Upgrade(value1, value2);
	}

	public static Upgrade ofLazyWire(Supplier<String> supplier) {
		return new Upgrade(supplier, LAZY_WIRE_STRING);
	}

	public static Upgrade ofLazyTokens(Supplier<String[]> supplier) {
		return new Upgrade(supplier, LAZY_TOKENS);
	}
}
