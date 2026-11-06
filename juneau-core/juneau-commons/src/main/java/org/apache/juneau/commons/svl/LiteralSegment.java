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
package org.apache.juneau.commons.svl;

import java.io.*;
import java.util.function.*;

/**
 * A {@link TemplateSegment} that contributes fixed (already-resolved) text.
 *
 * <p>
 * Produced by {@link VarTemplateCompiler} for any portion of the template input that contains
 * no variable references. Used both for plain text between markers and as the fallthrough
 * representation of unknown {@code ${X:...}} prefixes.
 *
 * <p>
 * Compile-time stable-value folding may also produce literal segments by eagerly resolving
 * opt-in stable {@link Var}s at compile time and replacing the {@link VarRefSegment} with
 * the resolved string.  Such literals carry the {@link #value} flag so a value encoder still sees them.
 */
final class LiteralSegment extends TemplateSegment {

	/** The fixed text contributed by this segment. Captured verbatim from the source. */
	final String text;

	/**
	 * {@code true} if the text is a folded <em>value</em> (a stable var's output) rather than author text.
	 * Only value literals are passed through a value encoder.
	 */
	final boolean value;

	LiteralSegment(String text) {
		this(text, false);
	}

	LiteralSegment(String text, boolean value) {
		this.text = text;
		this.value = value;
	}

	@Override
	void resolve(VarResolverSession session, StringBuilder out, UnaryOperator<String> encoder) {
		out.append(value ? encoder.apply(text) : text);
	}

	@Override
	void resolve(VarResolverSession session, StringBuilder out) {
		out.append(text);
	}

	@Override
	void resolveTo(VarResolverSession session, Writer w) throws IOException {
		w.write(text);
	}

	@Override
	boolean isLiteral() { return true; }
}
