// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Class used to add properties to a context builder from an annotation (e.g. {@link BeanConfig}).
 *
 * @param <A> The annotation that this <c>ConfigApply</c> reads from.
 * @param <B> The builder class to apply the annotation to.
 */
public abstract class AnnotationApplier<A extends Annotation, B> {

	private final VarResolverSession vr;
	private final Class<A> ca;
	private final Class<B> cb;

	/**
	 * Constructor.
	 *
	 * @param annotationClass The annotation class.
	 * @param builderClass The annotation class.
	 * @param vr The string resolver to use for resolving strings.
	 */
	protected AnnotationApplier(Class<A> annotationClass, Class<B> builderClass, VarResolverSession vr) {
		this.vr = vr == null ? VarResolver.DEFAULT.createSession() : vr;
		this.ca = annotationClass;
		this.cb = builderClass;
	}

	/**
	 * Apply the specified annotation to the specified property store builder.
	 *
	 * @param annotationInfo The annotation.
	 * @param builder The property store builder.
	 */
	public abstract void apply(AnnotationInfo<A> annotationInfo, B builder);

	/**
	 * Returns <jk>true</jk> if this apply can be appied to the specified builder.
	 *
	 * @param builder The builder to check.
	 * @return <jk>true</jk> if this apply can be appied to the specified builder.
	 */
	public boolean canApply(Object builder) {
		return cb.isInstance(builder);
	}

	/**
	 * Returns the builder class that this applier applies to.
	 *
	 * @return The builder class that this applier applies to.
	 */
	public Class<?> getBuilderClass() {
		return cb;
	}

	/**
	 * Returns the var resolver session for this apply.
	 *
	 * @return The var resolver session for this apply.
	 */
	protected VarResolverSession vr() {
		return vr;
	}

	/**
	 * Resolves the specified string.
	 *
	 * @param in The string containing variables to resolve.
	 * @return An optional containing the specified string if it exists, or {@link Optional#empty()} if it does not.
	 */
	protected Optional<String> string(String in) {
		in = vr.resolve(in);
		return isEmpty(in) ? Optional.empty() : Optional.of(in);
	}

	/**
	 * Returns the specified value if it's simple name is not <js>"Null"</js>.
	 *
	 * @param in The value to return.
	 * @return An optional containing the specified value.
	 */
	protected <T> Optional<Class<T>> type(Class<T> in) {
		return in.getSimpleName().equals("Null") ? Optional.empty() : Optional.of(in);
	}

	/**
	 * Returns the specified string array as an {@link Optional}.
	 *
	 * <p>
	 * If the array is empty, then returns {@link Optional#empty()}.
	 *
	 * @param in The string array.
	 * @return The array wrapped in an {@link Optional}.
	 */
	protected Optional<String[]> strings(String[] in) {
		return Optional.ofNullable(in.length == 0 ? null : Arrays.asList(in).stream().map(x -> vr.resolve(x)).filter(x -> !StringUtils.isEmpty(x)).toArray(String[]::new));
	}

	/**
	 * Resolves the specified strings in the string array.
	 *
	 * @param in The string array containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected List<String> stringList(String[] in) {
		return stream(in).collect(Collectors.toList());
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> stream(String[] in) {
		return Arrays.asList(in).stream().map(x -> vr.resolve(x)).filter(x -> !StringUtils.isEmpty(x));
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> strings_cdl(String in) {
		return Arrays.asList(StringUtils.split(vr.resolve(in))).stream().filter(x -> !StringUtils.isEmpty(x));
	}

	/**
	 * Resolves the specified string and converts it to a boolean.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved boolean.
	 */
	public Optional<Boolean> bool(String in) {
		return string(in).map(Boolean::parseBoolean);
	}

	/**
	 * Resolves the specified string and converts it to an int.
	 *
	 * @param in The string containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return The resolved int.
	 */
	protected Optional<Integer> integer(String in, String loc) {
		try {
			return string(in).map(Integer::parseInt);
		} catch (NumberFormatException e) {
			throw new ConfigException("Invalid syntax for integer on annotation @{0}({1}): {2}", ca.getSimpleName(), loc, in);
		}
	}

	/**
	 * Resolves the specified string and converts it to a Visibility.
	 *
	 * @param in The string containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return The resolved Visibility.
	 */
	protected Optional<Visibility> visibility(String in, String loc) {
		try {
			return string(in).map(Visibility::valueOf);
		} catch (IllegalArgumentException e) {
			throw new ConfigException("Invalid syntax for visibility on annotation @{0}({1}): {2}", ca.getSimpleName(), loc, in);
		}
	}

	/**
	 * Resolves the specified string and converts it to a Charset.
	 *
	 * @param in The string containing variables to resolve.
	 * @return The resolved Charset.
	 */
	protected Optional<Charset> charset(String in) {
		return string(in).map(x -> "default".equalsIgnoreCase(x) ? Charset.defaultCharset() : Charset.forName(x));
	}

	/**
	 * Resolves the specified string and converts it to a Character.
	 *
	 * @param in The string containing variables to resolve.
	 * @param loc The annotation field name.
	 * @return The resolved Character.
	 */
	protected Optional<Character> character(String in, String loc) {
		return string(in).map(x -> toCharacter(x, loc));
	}

	private Character toCharacter(String in, String loc) {
		if (in.length() != 1)
			throw new ConfigException("Invalid syntax for character on annotation @{0}({1}): {2}", ca.getSimpleName(), loc, in);
		return in.charAt(0);
	}

	/**
	 * Returns the specified class array as an {@link Optional}.
	 *
	 * <p>
	 * If the array is empty, then returns {@link Optional#empty()}.
	 *
	 * @param in The class array.
	 * @return The array wrapped in an {@link Optional}.
	 */
	protected Optional<Class<?>[]> classes(Class<?>[] in) {
		return Optional.ofNullable(in.length == 0 ? null : in);
	}

	/**
	 * Convenience method for detecting if an array is empty.
	 *
	 * @param value The array to check.
	 * @return <jk>true</jk> if the specified array is empty.
	 */
	protected boolean isEmpty(Object value) {
		return ObjectUtils.isEmpty(value);
	}

	/**
	 * Represents a no-op configuration apply.
	 */
	public static class NoOp extends AnnotationApplier<Annotation,Object> {

		/**
		 * Constructor.
		 *
		 * @param r The string resolver to use for resolving strings.
		 */
		public NoOp(VarResolverSession r) {
			super(Annotation.class, Object.class, r);
		}

		@Override /* ConfigApply */
		public void apply(AnnotationInfo<Annotation> ai, Object b) {}
	}
}
