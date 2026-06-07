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
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

/**
 * Supported wire formats for {@link Class} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#classFormat(ClassFormat)},
 * {@link org.apache.juneau.marshall.Marshalled#classFormat()},
 * {@link org.apache.juneau.marshall.MarshalledProp#classFormat()}, and
 * {@link org.apache.juneau.marshall.MarshalledConfig#classFormat()} to control how {@link Class} values
 * are written to text-based wire formats.
 *
 * <p>
 * The default is {@link #FQCN} which mirrors the historical {@code ClassSwap} behavior (canonical dotted
 * name with sensible fallback to the JVM binary name).
 *
 * <h5 class='topic'>FQCN vs BINARY_NAME for nested classes</h5>
 *
 * <p>
 * For nested-type encoding the two forms diverge:
 * <ul>
 * 	<li>{@link #FQCN} uses {@link Class#getCanonicalName()} when available — {@code "java.util.Map.Entry"}
 * 		— and falls back to {@link Class#getName()} for local / anonymous classes (where
 * 		{@code getCanonicalName()} returns {@code null}).
 * 	<li>{@link #BINARY_NAME} always uses {@link Class#getName()} — {@code "java.util.Map$Entry"}.
 * </ul>
 *
 * <p>
 * For arrays the difference is more dramatic:
 * <ul>
 * 	<li>{@link #FQCN} produces {@code "int[]"} / {@code "java.lang.String[]"}.
 * 	<li>{@link #BINARY_NAME} produces {@code "[I"} / {@code "[Ljava.lang.String;"} (JVM descriptor form).
 * </ul>
 *
 * <h5 class='topic'>SIMPLE_NAME is serialize-only</h5>
 *
 * <p>
 * {@link #SIMPLE_NAME} discards package and outer-class context and is <b>not round-trip safe</b>. The
 * parser path for this constant throws {@link UnsupportedOperationException}; use {@link #FQCN} or
 * {@link #BINARY_NAME} for round-trippable wires.
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link org.apache.juneau.marshall.MarshalledProp#classFormat() @MarshalledProp(classFormat=…)} on the bean property.
 * 	<li>{@link org.apache.juneau.marshall.Marshalled#classFormat() @Marshalled(classFormat=…)} on the bean class.
 * 	<li>{@link org.apache.juneau.marshall.MarshalledConfig#classFormat() @MarshalledConfig(classFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder#classFormat(ClassFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.classFormat</c>.
 * 	<li>The default constant ({@link #FQCN}).
 * </ol>
 *
 * <h5 class='topic'>Parser leniency</h5>
 *
 * <p>
 * Parsers SHALL try {@link Class#forName(String, boolean, ClassLoader)} first regardless of the
 * parser-side {@code ClassFormat} setting — this handles {@link #BINARY_NAME} and most {@link #FQCN}
 * inputs unambiguously. For nested-type {@link #FQCN} input ({@code "java.util.Map.Entry"}), the parser
 * walks back through dotted segments from right to left, replacing each uppercase-prefixed dot with
 * {@code $} and retrying — this resolves multi-level nesting like
 * {@code "com.example.Outer.Inner.Deepest"} to {@code com.example.Outer$Inner$Deepest}.  For
 * {@link #FQCN} array input the parser strips trailing {@code []} pairs, resolves the leaf component
 * type (including primitives like {@code "int"}), and rebuilds the array via {@link Class#arrayType()}
 * — so {@code "int[]"} resolves to {@code int[].class} and {@code "java.lang.String[][]"} resolves to
 * {@code String[][].class}.
 *
 * <h5 class='topic'>Binary serializers</h5>
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Proto / Parquet) emit a UTF-8 string regardless of this
 * setting — there is no native {@code Class} wire type in any supported binary format.
 */
public enum ClassFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Canonical fully-qualified name with dotted nested-type separators (the default).
	 *
	 * <p>
	 * Uses {@link Class#getCanonicalName()} when available; falls back to {@link Class#getName()} for
	 * local / anonymous classes where {@code getCanonicalName()} returns {@code null}.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	java.lang.String
	 * 	java.util.Map.Entry
	 * 	int[]
	 * </p>
	 */
	FQCN,

	/**
	 * JVM binary name per {@link Class#getName()}.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	java.lang.String
	 * 	java.util.Map$Entry
	 * 	[Ljava.lang.String;
	 * 	[I
	 * </p>
	 *
	 * <p>
	 * Identical to {@link #FQCN} for top-level non-array reference types; diverges for nested types
	 * ({@code $} vs {@code .}) and arrays (JVM descriptor vs {@code Type[]} form).
	 */
	BINARY_NAME,

	/**
	 * Unqualified simple name per {@link Class#getSimpleName()}.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	String
	 * 	Entry
	 * 	int
	 * </p>
	 *
	 * <p>
	 * <b>Serialize-only.</b> The parser path for this constant throws
	 * {@link UnsupportedOperationException} — there is no defensible way to resolve {@code "Map"} back
	 * to a unique {@link Class} without a registry hint, and adding such a hint is out of scope.
	 */
	SIMPLE_NAME;

	/**
	 * Formats the specified {@link Class} using this format.
	 *
	 * <p>
	 * {@link #NOT_SET} / {@code null} format are treated as {@link #FQCN}.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #FQCN}).
	 * @return The formatted wire representation, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static String format(Class<?> value, ClassFormat format) {
		if (value == null)
			return null;
		var fmt = format == null ? FQCN : format;
		return switch (fmt) {
			case NOT_SET, FQCN -> {
				// getCanonicalName() returns null for local / anonymous classes; fall back to binary name.
				var canonical = value.getCanonicalName();
				yield canonical != null ? canonical : value.getName();
			}
			case BINARY_NAME -> value.getName();
			case SIMPLE_NAME -> value.getSimpleName();
		};
	}

	/**
	 * Parses the specified wire value into a {@link Class}.
	 *
	 * <p>
	 * Lenient parsing — handles every shape produced by {@link #format(Class, ClassFormat)} for the
	 * round-trippable formats ({@link #FQCN} and {@link #BINARY_NAME}), and accepts cross-form input
	 * as well (an {@link #FQCN} parser still resolves {@link #BINARY_NAME} input and vice versa).
	 *
	 * <p>
	 * Resolution steps:
	 * <ol>
	 * 	<li>Strip trailing {@code []} pairs ({@link #FQCN} array form), tracking dimension count.  Empty
	 * 		brackets at the leading edge (JVM descriptor form like {@code "[I"} /
	 * 		{@code "[Ljava.lang.String;"}) are left to {@link Class#forName(String, boolean, ClassLoader)}
	 * 		because that method already handles them.
	 * 	<li>Resolve the leaf component:
	 * 		<ul>
	 * 			<li>If the leaf names a primitive type ({@code "int"}, {@code "boolean"}, etc. — emitted
	 * 				by {@link #FQCN} for primitive arrays), return the corresponding {@code TYPE} class.
	 * 			<li>Otherwise try {@link Class#forName(String, boolean, ClassLoader)} directly.
	 * 			<li>On failure, walk dots from right to left, replacing each uppercase-prefixed dot with
	 * 				{@code $} (cumulatively — already-replaced dots stay replaced) and retrying.  This
	 * 				resolves multi-level nesting like {@code "com.example.Outer.Inner.Deepest"} to
	 * 				{@code com.example.Outer$Inner$Deepest}.  Dots whose trailing segment starts with a
	 * 				lowercase letter are treated as package boundaries and skipped.
	 * 		</ul>
	 * 	<li>Apply {@link Class#arrayType()} once per stripped dimension to rebuild the array class.
	 * </ol>
	 *
	 * <p>
	 * {@link #SIMPLE_NAME} is not supported on the parser side — throws
	 * {@link UnsupportedOperationException}.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @param format The configured format hint. Can be <jk>null</jk> (treated as {@link #FQCN}).
	 * @param classLoader The class loader. Can be <jk>null</jk> (uses
	 *   {@link Thread#getContextClassLoader() context class loader}).
	 * @return The parsed {@link Class}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value cannot be resolved.
	 * @throws UnsupportedOperationException If {@code format} is {@link #SIMPLE_NAME}.
	 */
	public static Class<?> parse(String value, ClassFormat format, ClassLoader classLoader) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		var fmt = format == null ? FQCN : format;
		if (fmt == SIMPLE_NAME)
			throw new UnsupportedOperationException("SIMPLE_NAME is serialize-only — use FQCN or BINARY_NAME for round-trippable wires");
		var cl = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
		try {
			return resolveWithArraySuffix(s, cl);
		} catch (ClassNotFoundException firstAttempt) {
			throw illegalArg("Could not resolve class ''{0}'' for format {1}: {2}", value, format, firstAttempt.getMessage());
		}
	}

	/**
	 * Strips trailing {@code []} pairs from the FQCN array form, resolves the leaf component, then
	 * rebuilds the array dimensions via {@link Class#arrayType()}.
	 *
	 * <p>
	 * Inputs that don't end in {@code []} (top-level classes and JVM-descriptor array forms like
	 * {@code "[I"}) pass straight through to {@link #resolveLeaf(String, ClassLoader)}.
	 */
	private static Class<?> resolveWithArraySuffix(String s, ClassLoader cl) throws ClassNotFoundException {
		var dims = 0;
		var leaf = s;
		while (leaf.endsWith("[]")) {
			dims++;
			leaf = leaf.substring(0, leaf.length() - 2);
		}
		if (dims == 0)
			return resolveLeaf(s, cl);
		if (leaf.isEmpty())
			throw new ClassNotFoundException(s);
		var result = resolveLeaf(leaf, cl);
		for (var i = 0; i < dims; i++)
			result = result.arrayType();
		return result;
	}

	/**
	 * Resolves a non-array leaf token to a {@link Class}.
	 *
	 * <p>
	 * Checks the primitive-name table first ({@link #FQCN} emits {@code "int"} / {@code "boolean"} /
	 * etc. for primitive array leafs), then falls through to {@link Class#forName(String, boolean,
	 * ClassLoader)} with a multi-level nested-type fallback heuristic.
	 */
	private static Class<?> resolveLeaf(String s, ClassLoader cl) throws ClassNotFoundException {
		var prim = primitiveByName(s);
		if (prim != null)
			return prim;
		try {
			return Class.forName(s, true, cl);
		} catch (ClassNotFoundException firstAttempt) {
			var nested = nestedTypeFallback(s, cl);
			if (nested != null)
				return nested;
			throw firstAttempt;
		}
	}

	/**
	 * Walks dots from right to left, replacing each uppercase-prefixed dot with {@code $} and retrying
	 * {@link Class#forName(String, boolean, ClassLoader)}.  Replacements accumulate — once a dot has
	 * been replaced, it stays replaced for subsequent iterations — so multi-level nesting like
	 * {@code "Outer.Inner.Deepest"} resolves to {@code "Outer$Inner$Deepest"}.
	 *
	 * <p>
	 * Returns <jk>null</jk> if no rewrite resolves to a loadable class.  Dots whose trailing segment
	 * starts with a lowercase letter are treated as package boundaries and left alone.
	 */
	private static Class<?> nestedTypeFallback(String s, ClassLoader cl) {
		var chars = s.toCharArray();
		for (var i = chars.length - 1; i > 0; i--) {
			if (chars[i] == '.' && i + 1 < chars.length && Character.isUpperCase(chars[i + 1])) {
				chars[i] = '$';
				try {
					return Class.forName(new String(chars), true, cl);
				} catch (@SuppressWarnings("unused") ClassNotFoundException ignored) {
					// Keep walking; the previous-dot replacement persists in chars so deeper nesting can resolve.
				}
			}
		}
		return null;
	}

	private static Class<?> primitiveByName(String s) {
		return switch (s) {
			case "boolean" -> boolean.class;
			case "byte" -> byte.class;
			case "char" -> char.class;
			case "short" -> short.class;
			case "int" -> int.class;
			case "long" -> long.class;
			case "float" -> float.class;
			case "double" -> double.class;
			case "void" -> void.class;
			default -> null;
		};
	}

	/**
	 * Returns <jk>true</jk> if this format emits a numeric wire value.
	 *
	 * <p>
	 * Always <jk>false</jk> — every constant emits a textual representation.
	 *
	 * @return <jk>false</jk>.
	 */
	@SuppressWarnings({
		"static-method", // Kept as an instance method for polymorphic-by-convention symmetry with the other Format classes (BigNumberFormat, FloatFormat, DurationFormat, etc.) where isNumeric() depends on the enum constant.
		"java:S3400"     // Same rationale — must remain an instance method, not a constant, to match the cross-Format API contract.
	})
	public boolean isNumeric() {
		return false;
	}
}
