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
package org.apache.juneau.commons.utils;

/**
 * Package-private terse exception factories shared by the domain utility classes.
 *
 * <p>Mirrors the terse {@code *ex} forms exposed publicly by {@link Shorts}
 * ({@code rex}/{@code iaex}/{@code isex}/{@code uoex}/{@code uoroex}) so the {@code org.apache.juneau.commons.utils}
 * domain classes ({@link CollectionUtils}, {@link StringUtils}, {@link ThrowableUtils}, {@link FileUtils},
 * {@link IoUtils}, {@link AssertionUtils}) can construct exceptions tersely <b>without</b> taking a dependency on
 * {@code Shorts}. Each factory builds the exception directly (self-contained — no delegation to {@code Shorts}),
 * using {@link StringUtils#format(String, Object...)} for message interpolation.
 */
final class Exceptions {

	private Exceptions() {}

	/** Creates a {@link RuntimeException} with a formatted message. */
	static RuntimeException rex(String m, Object...a) { return new RuntimeException(StringUtils.format(m, a)); }

	/** Creates a {@link RuntimeException} wrapping a cause. */
	static RuntimeException rex(Throwable t) { return new RuntimeException(t); }

	/** Creates a {@link RuntimeException} with a cause and formatted message. */
	static RuntimeException rex(Throwable t, String m, Object...a) { return new RuntimeException(StringUtils.format(m, a), t); }

	/** Creates an {@link IllegalArgumentException} with a formatted message. */
	static IllegalArgumentException iaex(String m, Object...a) { return new IllegalArgumentException(StringUtils.format(m, a)); }

	/** Creates an {@link IllegalArgumentException} wrapping a cause. */
	static IllegalArgumentException iaex(Throwable t) { return new IllegalArgumentException(t); }

	/** Creates an {@link IllegalArgumentException} with a cause and formatted message. */
	static IllegalArgumentException iaex(Throwable t, String m, Object...a) { return new IllegalArgumentException(StringUtils.format(m, a), t); }

	/** Creates an {@link IllegalStateException} with a formatted message. */
	static IllegalStateException isex(String m, Object...a) { return new IllegalStateException(StringUtils.format(m, a)); }

	/** Creates an {@link UnsupportedOperationException} with the message "Not supported." */
	static UnsupportedOperationException uoex() { return new UnsupportedOperationException("Not supported."); }

	/** Creates an {@link UnsupportedOperationException} with a formatted message. */
	static UnsupportedOperationException uoex(String m, Object...a) { return new UnsupportedOperationException(StringUtils.format(m, a)); }

	/** Creates an {@link UnsupportedOperationException} wrapping a cause. */
	static UnsupportedOperationException uoex(Throwable t) { return new UnsupportedOperationException(t); }

	/** Creates an {@link UnsupportedOperationException} with a cause and formatted message. */
	static UnsupportedOperationException uoex(Throwable t, String m, Object...a) { return new UnsupportedOperationException(StringUtils.format(m, a), t); }

	/** Creates an {@link UnsupportedOperationException} with the message "Object is read only." */
	static UnsupportedOperationException uoroex() { return new UnsupportedOperationException("Object is read only."); }
}
