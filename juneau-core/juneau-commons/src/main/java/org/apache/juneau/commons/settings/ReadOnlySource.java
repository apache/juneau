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
 * A read-only {@link SettingSource} implementation that delegates to a function.
 *
 * <p>
 * This class provides a read-only source for settings that delegates property lookups to a provided function.
 * It's particularly useful for wrapping existing property sources (e.g., {@link System#getProperty(String)},
 * {@link System#getenv(String)}) as {@link SettingSource} instances.
 *
 * <h5 class='section'>Return Value Semantics:</h5>
 * <ul class='spaced-list'>
 * 	<li>If the function returns <c>null</c>, this source returns <c>null</c> (key doesn't exist).
 * 	<li>If the function returns a non-null value, this source returns <c>Optional.of(value)</c>.
 * </ul>
 *
 * <p>
 * Note: This source cannot distinguish between a key that doesn't exist and a key that exists with a null value,
 * since the function only returns a <c>String</c>. If you need to distinguish these cases, use {@link MapSource} instead.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a read-only source from System.getProperty</jc>
 * 	ReadOnlySource <jv>sysProps</jv> = <jk>new</jk> ReadOnlySource(x -&gt; System.getProperty(x));
 *
 * 	<jc>// Create a read-only source from System.getenv</jc>
 * 	ReadOnlySource <jv>envVars</jv> = <jk>new</jk> ReadOnlySource(x -&gt; System.getenv(x));
 *
 * 	<jc>// Add to Settings</jc>
 * 	Settings.<jsf>get</jsf>().addSource(<jv>sysProps</jv>);
 * </p>
 */
public class ReadOnlySource implements SettingSource {

	private final Function<String,String> function;

	/**
	 * Constructor.
	 *
	 * @param function The function to delegate property lookups to. Must not be <c>null</c>.
	 */
	public ReadOnlySource(Function<String,String> function) {
		this.function = function;
	}

	/**
	 * Returns a setting by delegating to the function.
	 *
	 * <p>
	 * If the function returns <c>null</c>, this method returns <c>null</c> (indicating the key doesn't exist).
	 * If the function returns a non-null value, this method returns <c>Optional.of(value)</c>.
	 *
	 * @param name The property name.
	 * @return The property value, or <c>null</c> if the function returns <c>null</c>.
	 */
	@Override
	public Optional<String> get(String name) {
		var v = function.apply(name);
		return v == null ? null : opt(v);
	}

	/**
	 * Returns <c>false</c> since this source is read-only.
	 *
	 * @return <c>false</c>
	 */
	@Override
	public boolean canWrite() {
		return false;
	}

	/**
	 * No-op since this source is read-only.
	 *
	 * @param name The property name (ignored).
	 * @param value The property value (ignored).
	 */
	@Override
	public void set(String name, String value) {}

	/**
	 * No-op since this source is read-only.
	 *
	 * @param name The property name (ignored).
	 */
	@Override
	public void unset(String name) {}

	/**
	 * No-op since this source is read-only.
	 */
	@Override
	public void clear() {}

}
