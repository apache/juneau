// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.assertions;

import java.io.*;
import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against lists.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the specified POJO is the specified type.</jc>
 * 	<jsm>assertMap</jsm>(<jv>mymap</jv>).isNotEmpty();
 * </p>
 *
 * @param <K> The map key type.
 * @param <V> The map value type.
 */
@FluentSetters(returns="MapAssertion<K,V>")
public class MapAssertion<K,V> extends FluentMapAssertion<K,V,MapAssertion<K,V>> {

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link MapAssertion} object.
	 */
	public static <K,V> MapAssertion<K,V> create(Map<K,V> value) {
		return new MapAssertion<>(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 */
	public MapAssertion(Map<K,V> value) {
		super(value, null);
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public MapAssertion<K,V> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public MapAssertion<K,V> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public MapAssertion<K,V> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public MapAssertion<K,V> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public MapAssertion<K,V> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
