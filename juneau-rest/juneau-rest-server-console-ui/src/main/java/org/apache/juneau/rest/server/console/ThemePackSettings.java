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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * A per-resource settings bean selecting the active {@link ThemePack}, resolved from the {@code BeanStore} the same
 * way {@link ThemeSettings} resolves the active {@link Theme}.
 *
 * <p>
 * A structural mirror of {@link ThemeSettings} &mdash; a plain immutable holder with a static factory rather than a
 * builder, for the reason that class's javadoc gives &mdash; with <b>one deliberate difference</b>.
 *
 * <h5 class='section'>There is no DEFAULT constant, and that absence is the point:</h5>
 * <p>
 * {@link ThemeSettings#DEFAULT} exists because there <i>is</i> a default theme: {@link Theme#OPEN}. There is no
 * default <i>pack</i>. A zero-config application renders {@link Theme#OPEN}, and a pack is something an
 * application opts into &mdash; so "there is no default pack" is encoded here as a property of the API rather
 * than left to convention. {@code ConsoleChromeMixin} resolves the active pack as an
 * {@link java.util.Optional Optional} and falls through to the existing theme chain when it is empty.
 *
 * <p>
 * Consequently, publishing this bean is the <i>only</i> way a pack reaches the mixin through the bean store; there
 * is nothing to "unset". If a future release ever makes some pack the shipped default, that change is literally the
 * addition of a {@code DEFAULT} constant here &mdash; a small, reviewable, deliberate act at a named release
 * boundary rather than a side effect of adding a pack.
 *
 * <h5 class='section'>A pack's ASSETS do not arrive through this bean:</h5>
 * <p>
 * A pack supplied here has its <b>tokens and aliases applied in full</b>, but its
 * {@link ThemePack.Builder#logo(String) logo} and
 * {@link ThemePack.Builder#pageBackgroundImage(String) page background} are <b>silently ignored</b>. This bean is
 * resolved per request, whereas the mixin's asset fields and their content-hash cache-busters are fixed at
 * construction &mdash; so only a pack handed to {@code ConsoleChromeMixin.Builder.pack(ThemePack)} can carry assets
 * through. Stated here as well as on that method because this is the type an application actually publishes, and a
 * caller who never opens the builder's javadoc would otherwise expect a bundled logo to appear. To serve a pack's
 * assets, configure the pack on the mixin builder instead of (or in addition to) publishing this bean.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> ThemePackSettings pack() {
 * 		<jk>return</jk> ThemePackSettings.<jsm>of</jsm>(<jv>myCorporatePack</jv>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThemePack}
 * 	<li class='jc'>{@link ThemeSettings}
 * 	<li class='jc'>{@link ConsoleChromeMixin}
 * </ul>
 *
 * @since 10.0.0
 */
public final class ThemePackSettings {

	private final ThemePack pack;

	private ThemePackSettings(ThemePack pack) {
		this.pack = pack;
	}

	/**
	 * Creates a new settings bean wrapping the specified pack.
	 *
	 * @param pack The active pack. Must not be <jk>null</jk>.
	 * @return A new {@link ThemePackSettings}.
	 * @throws IllegalArgumentException If {@code pack} is <jk>null</jk>.
	 */
	public static ThemePackSettings of(ThemePack pack) {
		if (pack == null)
			throw iaex("pack must not be null.");
		return new ThemePackSettings(pack);
	}

	/**
	 * Returns the active pack.
	 *
	 * @return The active pack. Never <jk>null</jk>.
	 */
	public ThemePack getPack() { return pack; }
}
