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

import java.util.*;

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * An {@link HtmlRender} that renders any <jk>enum</jk> property as a {@link Tag}/{@code chrome.css} pill, wired via
 * {@link Html#render() @Html(render=TagHtmlRender.class)} on the property or the enum type.
 *
 * <p>
 * The pill's {@code domain} (the CSS class between {@code tag} and the lowercased constant name, e.g.
 * {@code status} in {@code .tag.status.released}) comes from the enum <b>type's</b> {@link TagDomain @TagDomain}
 * annotation, defaulting to {@code "status"} when absent &mdash; never from {@link Class#getSimpleName()}.
 *
 * <h5 class='section'>Per-property domain override:</h5>
 * <p>
 * Subclass and override {@link #domain(Enum)} (or hard-code a different constant), then wire the subclass via a
 * different {@code @Html(render=...)} &mdash; not a new {@code @Html}/marshall member (see {@link TagDomain}'s
 * javadoc for why {@code @Html} itself cannot grow a {@code domain} attribute).
 *
 * @since 10.0.0
 */
public class TagHtmlRender extends HtmlRender<Enum<?>> {

	@Override
	public Object getContent(SerializerSession session, Enum<?> value) {
		if (value == null)
			return null;
		var d = domain(value);
		var v = value.name().toLowerCase(Locale.ROOT);
		return Tag.of(d, v);
	}

	/**
	 * Resolves the pill domain for the specified enum constant.
	 *
	 * <p>
	 * Reads {@link TagDomain @TagDomain} off {@link Enum#getDeclaringClass()} &mdash; deliberately <b>not</b>
	 * {@link Object#getClass()} &mdash; because {@code getDeclaringClass()} is the direct, unambiguous way to say
	 * "the domain is a property of the enum <i>type</i>, never an individual constant", and stays correct
	 * regardless of {@code @TagDomain}'s own {@code @Inherited} meta-annotation.
	 *
	 * <p>
	 * <b>Verified-during-development correction to the plan's stated rationale:</b> the plan's Phase 4 gate
	 * justified this choice by claiming {@code @Inherited} "does not extend across enum-constant-body
	 * subclassing the way it does for ordinary class inheritance" for a constant with an anonymous body (e.g.
	 * {@code enum E { A { ... }, B }}). That claim was checked against a standalone JDK 17 reflection probe and is
	 * factually incorrect: {@code A}'s anonymous runtime class's {@code getSuperclass()} <b>is</b> {@code E}
	 * itself, so {@code @Inherited}'s superclass walk finds {@code E}'s {@code @TagDomain} from
	 * {@code A.getClass().getAnnotation(TagDomain.class)} too &mdash; {@code getClass()} and
	 * {@code getDeclaringClass()} give the <i>same</i> answer for this case, given that {@code TagDomain} is
	 * {@code @Inherited}. {@code getDeclaringClass()} is kept anyway: it is the more direct expression of intent,
	 * one reflective call instead of two, and does not silently depend on {@code @Inherited} staying on
	 * {@code TagDomain} in some future revision. See the phase manifest for the reflection probe.
	 *
	 * @param value The enum constant being rendered. Never <jk>null</jk> (guarded by {@link #getContent}).
	 * @return The resolved domain, defaulting to {@code "status"}.
	 */
	protected String domain(Enum<?> value) {
		var t = value.getDeclaringClass().getAnnotation(TagDomain.class);
		return (t == null || t.domain().isEmpty()) ? "status" : t.domain();
	}
}
