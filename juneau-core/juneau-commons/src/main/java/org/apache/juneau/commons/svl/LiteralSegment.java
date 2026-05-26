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

/**
 * A {@link TemplateSegment} that contributes fixed (already-resolved) text.
 *
 * <p>
 * Produced by {@link VarTemplateCompiler} for any portion of the template input that contains
 * no variable references. Used both for plain text between markers and as the fallthrough
 * representation of unknown {@code $X{...}} prefixes.
 *
 * <p>
 * Compile-time stable-value folding may also produce literal segments by eagerly resolving
 * opt-in stable {@link Var}s at compile time and replacing the {@link VarRefSegment} with
 * the resolved string.
 */
final class LiteralSegment extends TemplateSegment {

	/** The fixed text contributed by this segment. Captured verbatim from the source. */
	final String text;

	LiteralSegment(String text) {
		this.text = text;
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
