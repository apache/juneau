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
package org.apache.juneau.rest.server.console;

/**
 * A per-resource settings bean selecting the active {@link Theme}, resolved from the {@code BeanStore} the same way
 * {@code QueryableSettings}/{@code IntrospectableSettings}/{@code DumpsSettings} resolve their own settings.
 *
 * <p>
 * <b>Intentionally simpler than {@code QueryableSettings.create().build()}</b>: a single-field wrapper around one
 * {@link Theme} doesn't need builder ceremony, so this class is a plain immutable holder with a static factory
 * rather than a full builder &mdash; it mirrors the config-bean-from-BeanStore <i>idiom</i>, not that class's exact
 * shape.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> ThemeSettings theme() {
 * 		<jk>return</jk> ThemeSettings.<jsm>of</jsm>(mySalesforceTheme);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Theme}
 * 	<li class='jc'>{@link ConsoleChromeMixin}
 * </ul>
 *
 * @since 10.0.0
 */
public final class ThemeSettings {

	/** The default settings: the {@link Theme#OPEN} theme. */
	public static final ThemeSettings DEFAULT = new ThemeSettings(Theme.OPEN);

	private final Theme theme;

	private ThemeSettings(Theme theme) {
		this.theme = theme;
	}

	/**
	 * Creates a new settings bean wrapping the specified theme.
	 *
	 * @param theme The active theme. Must not be <jk>null</jk>.
	 * @return A new {@link ThemeSettings}.
	 */
	public static ThemeSettings of(Theme theme) {
		if (theme == null)
			throw new IllegalArgumentException("theme must not be null.");
		return new ThemeSettings(theme);
	}

	/**
	 * Returns the active theme.
	 *
	 * @return The active theme. Never <jk>null</jk>.
	 */
	public Theme getTheme() { return theme; }
}
