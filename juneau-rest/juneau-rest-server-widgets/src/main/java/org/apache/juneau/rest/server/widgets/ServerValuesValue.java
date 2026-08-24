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
package org.apache.juneau.rest.server.widgets;

import java.util.function.*;

import org.apache.juneau.commons.svl.*;

/**
 * An immutable named server-side scalar value declaration held by a {@link ServerValues} bean.
 *
 * <p>
 * Each value pairs a stable {@code name} (the key an author interpolates as <js>"$FV{name}"</js>) with a
 * session-aware {@code provider}.  The provider is invoked at serve time against a per-render
 * {@link VarResolverSession} and must return a scalar ({@link String} / {@link Number} / {@link Boolean}) or
 * <jk>null</jk>; non-scalar results are rejected at resolve time rather than {@code toString()}-ed.
 *
 * <p>
 * {@code cacheable} defaults to <jk>false</jk> &mdash; the provider re-runs on every reference within a render.
 * Set it <jk>true</jk> only for values that are stable across all references in a single response.
 *
 * @since 10.0.0
 */
public final class ServerValuesValue {

	/** The interpolation name (the {@code x} in <js>"$FV{x}"</js>).  Must not be null or blank. */
	public final String name;

	/** The session-aware scalar provider.  Must not be null. */
	public final Function<VarResolverSession,?> provider;

	/** Whether the resolved value may be memoized for the lifetime of a single render.  Defaults to false. */
	public final boolean cacheable;

	/**
	 * Constructor.
	 *
	 * @param name The interpolation name.
	 * @param provider The session-aware scalar provider.
	 * @param cacheable Whether the resolved value may be memoized within a single render.
	 */
	private ServerValuesValue(String name, Function<VarResolverSession,?> provider, boolean cacheable) {
		this.name = name;
		this.provider = provider;
		this.cacheable = cacheable;
	}

	/**
	 * Creates a non-cacheable value.
	 *
	 * @param name The interpolation name.
	 * @param provider The session-aware scalar provider.
	 * @return A new {@link ServerValuesValue} with {@code cacheable=false}.
	 */
	public static ServerValuesValue of(String name, Function<VarResolverSession,?> provider) {
		return new ServerValuesValue(name, provider, false);
	}

	/**
	 * Creates a value with an explicit cacheable flag.
	 *
	 * @param name The interpolation name.
	 * @param provider The session-aware scalar provider.
	 * @param cacheable Whether the resolved value may be memoized within a single render.
	 * @return A new {@link ServerValuesValue}.
	 */
	public static ServerValuesValue of(String name, Function<VarResolverSession,?> provider, boolean cacheable) {
		return new ServerValuesValue(name, provider, cacheable);
	}
}
