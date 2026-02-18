/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * A functional interface for creating read-only {@link SettingSource} instances from a function.
 *
 * <p>
 * This functional interface allows you to create setting sources directly from lambda expressions or method references,
 * making it easy to wrap existing property sources (e.g., {@link System#getProperty(String)},
 * {@link System#getenv(String)}) as {@link SettingSource} instances.
 *
 * <p>
 * Functional sources are read-only and do not implement {@link SettingStore}. If you need a writable source,
 * use {@link MapStore} or {@link FunctionalStore} instead.
 *
 * <h5 class='section'>Return Value Semantics:</h5>
 * <ul class='spaced-list'>
 * 	<li>If the function returns <c>null</c>, this source returns <c>null</c> (key doesn't exist).
 * 	<li>If the function returns a non-null value, this source returns <c>Optional.of(value)</c>.
 * </ul>
 *
 * <p>
 * Note: This source cannot distinguish between a key that doesn't exist and a key that exists with a null value,
 * since the function only returns a <c>String</c>. If you need to distinguish these cases, use {@link MapStore} instead.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a read-only source directly from a lambda (returns Optional)</jc>
 * 	Settings.<jsf>get</jsf>().addSource(name -&gt; opt(System.getProperty(name)));
 *
 * 	<jc>// Using the static factory method (takes Function&lt;String, String&gt;)</jc>
 * 	Settings.<jsf>get</jsf>().addSource(FunctionalSource.<jsf>of</jsf>(System::getProperty));
 *
 * 	<jc>// Create a read-only source from System.getenv</jc>
 * 	Settings.<jsf>get</jsf>().addSource(FunctionalSource.<jsf>of</jsf>(System::getenv));
 *
 * 	<jc>// Explicit creation for reuse</jc>
 * 	FunctionalSource <jv>sysProps</jv> = FunctionalSource.<jsf>of</jsf>(System::getProperty);
 * 	Settings.<jsf>get</jsf>().addSource(<jv>sysProps</jv>);
 * </p>
 */
@FunctionalInterface
public interface FunctionalSource extends SettingSource {

	/**
	 * Returns a setting by applying the function.
	 *
	 * <p>
	 * If the function returns <c>null</c>, this method returns <c>null</c> (indicating the key doesn't exist).
	 * If the function returns a non-null value, this method returns <c>Optional.of(value)</c>.
	 *
	 * @param name The property name.
	 * @return The property value, or <c>null</c> if the function returns <c>null</c>.
	 */
	@Override
	Optional<String> get(String name);

	/**
	 * Creates a functional source from a function that returns a string.
	 *
	 * <p>
	 * This is a convenience factory method for creating functional sources from functions that return
	 * <c>String</c> values. The function's return value is converted to an <c>Optional</c> as follows:
	 * <ul>
	 * 	<li>If the function returns <c>null</c>, the source returns <c>null</c> (key doesn't exist).
	 * 	<li>If the function returns a non-null value, the source returns <c>Optional.of(value)</c>.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create from a lambda</jc>
	 * 	FunctionalSource <jv>source1</jv> = FunctionalSource.<jsf>of</jsf>(name -&gt; System.getProperty(name));
	 *
	 * 	<jc>// Create from a method reference</jc>
	 * 	FunctionalSource <jv>source2</jv> = FunctionalSource.<jsf>of</jsf>(System::getProperty);
	 * </p>
	 *
	 * @param function The function to delegate property lookups to. Must not be <c>null</c>.
	 * @return A new functional source instance.
	 */
	static FunctionalSource of(UnaryOperator<String> function) {
		return name -> {
			var v = function.apply(name);
			return v == null ? null : opt(v);
		};
	}
}
