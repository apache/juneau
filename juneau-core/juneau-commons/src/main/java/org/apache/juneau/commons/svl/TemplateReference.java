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

/**
 * One reference found in a compiled {@link VarTemplate}: a <c>${...}</c> var, a function or operator call, an
 * identifier, or a syntax problem.
 *
 * <p>
 * Offsets are into {@link VarTemplate#getSource()}: <c>start</c> inclusive, <c>end</c> exclusive.  For a var the span is
 * the whole <c>${...}</c> marker; a <c>${name}</c> or <c>${name:default}</c> var reference also yields an {@link Kind#IDENT}
 * entry for its key (the text before the first top-level <c>:</c>); for a function and an identifier it is the name token; for a syntax problem it is the
 * offending marker.
 *
 * <p>
 * References are reported for markers that are evaluated at render time.  A stable var folded at compile time (and an
 * expression folded with it) has no entry.  A <c>${...}</c> inside a nested var body is not analyzed.
 *
 * @param kind What was referenced.
 * @param name The var prefix (<c>P</c> for the <c>${name}</c> shortcut), the function name (operators are
 * 	<c>$op.*</c>), the identifier, or for {@link Kind#SYNTAX} the error message.
 * @param bound Whether the name resolves in the resolver the template was compiled against: a registered var, or a
 * 	registered function permitted by the allowlist.  Always <jk>true</jk> for identifiers.
 * @param listName For an identifier read as a row field (inside the body of a select or projection whose target is a
 * 	top-level identifier), that identifier's name; <jk>null</jk> for a top-level identifier.  Identifiers in a body
 * 	whose target is anything else are not reported.
 * @param start Start offset, inclusive.
 * @param end End offset, exclusive.
 */
public record TemplateReference(Kind kind, String name, boolean bound, String listName, int start, int end) {

	/** The kind of reference. */
	public enum Kind {
		/** A <c>${...}</c> var reference. */ VAR,
		/** A function or operator call inside an expression. */ FUNCTION,
		/** A bare identifier inside an expression. */ IDENT,
		/** A marker that could not be compiled (lenient mode keeps it as text). */ SYNTAX
	}
}
