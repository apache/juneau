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
package org.apache.juneau.rest.server.view.freemarker.console;

import org.apache.juneau.rest.server.console.*;

/**
 * Per-render token-capture context shared between a {@code <@theme>} element and the {@code <@token>} elements
 * nested inside it, via {@link freemarker.core.Environment#setCustomState(Object, Object)}.
 *
 * <p>
 * {@code <@theme name="…">} installs a fresh instance holding a {@link Theme.Builder} seeded from the named stock
 * palette before rendering its body. Each nested {@code <@token>} eagerly folds its declaration into that builder:
 * a {@code value=} into {@link Theme.Builder#token(String, String)} (a leaf), an {@code alias=} into
 * {@link Theme.Builder#alias(String, String)} (a derived reference). After the body pass, {@code <@theme>} builds
 * the theme and renders its override block.
 *
 * @since 10.0.0
 */
final class ThemeBuildContext {

	/** The stock-theme name {@code <@theme name="…">} named - also the built {@link Theme}'s name. */
	final String name;

	/** The theme builder, seeded with the named stock palette; {@code <@token value="…">} adds leaves and {@code <@token alias="var(--jc-…)">} adds aliases here. */
	final Theme.Builder themeBuilder;

	/** Set true by the first {@code <@token>} that fires, so {@code <@theme>} emits an override block only when the body declared one. */
	boolean anyDeclared;

	ThemeBuildContext(String name, Theme.Builder themeBuilder) {
		this.name = name;
		this.themeBuilder = themeBuilder;
	}
}
