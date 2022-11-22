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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Class used to add properties to a context builder (e.g. {@link BeanContext.Builder}) from an annotation (e.g. {@link BeanConfig}).
 *
 * <p>
 * Used by {@link Context.Builder#applyAnnotations(Class...)} and {@link Context.Builder#applyAnnotations(java.lang.reflect.Method...)} to apply
 * annotations to context beans.
 *
 * <p>
 * The following code shows the general design pattern.
 *
 * <p class='bjava'>
 * 	<jc>// The annotation applied to classes and methods.</jc>
 * 	<ja>@Target</ja>({METHOD,TYPE})
 * 	<ja>@Retention</ja>(<jsf>RUNTIME</jsf>)
 * 	<ja>@ContextApply</ja>(BeanConfigAnnotationApplier.<jk>class</jk>)
 * 	<jk>public</jk> <jk>@interface </jk>BeanConfig {
 *
 * 		String sortProperties() <jk>default</jk> <js>""</js>;
 *
 * 	}
 *
 * 	<jc>// The applier that applies the annotation to the bean context builder.</jc>
 * 	<jk>public class</jk> BeanConfigAnnotationApplier <jk>extends</jk> AnnotationApplier&lt;<ja>BeanConfig</ja>,BeanContext.Builder&gt; {
 *
 *		<jc>// Required constructor. </jc>
 * 		<jk>public</jk> Applier(VarResolverSession <jv>vr</jv>) {
 * 			<jk>super</jk>(BeanConfig.<jk>class</jk>, BeanContext.Builder.<jk>class</jk>, <jv>vr</jv>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> apply(AnnotationInfo&lt;BeanConfig&gt; <jv>annotationInfo</jv>, BeanContext.Builder <jv>builder</jv>) {
 * 			<ja>BeanConfig</ja> <jv>beanConfig</jv> = <jv>annotationInfo</jv>.getAnnotation();
 *
 * 			String <jv>sortProperties</jv> = <jv>beanConfig</jv>.sortProperties();
 * 			<jk>if</jk> (! <jv>sortProperties</jv>.isEmpty())
 * 				<jv>builder</jv>.sortProperties(Boolean.<jsm>parseBoolean</jsm>(<jv>sortProperties</jv>));
 * 		}
 * 	}
 *
 *	<jc>// An annotated class.</jc>
 * 	<ja>@BeanConfig</ja>(sortProperties=<js>"true"</js>)
 * 	<jk>public class</jk> AnnotatedClass {}
 *
 *	<jc>// Putting it together.</jc>
 * 	<jk>public static void</jk> main(String[] <jv>args</jv>) {
 *
 *		<jc>// Create a JSON serializer with sorted properties.</jc>
 * 		Serializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().applyAnnotations(AnnotatedClass.<jk>class</jk>).build();
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <A> The annotation that this applier reads from.
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
		return optional(isEmpty(in) ? null : in);
	}

	/**
	 * Returns the specified value if it's simple name is not <js>"void"</js>.
	 *
	 * @param <T> The value to return.
	 * @param in The value to return.
	 * @return An optional containing the specified value.
	 */
	protected <T> Optional<Class<T>> type(Class<T> in) {
		return optional(in).filter(NOT_VOID);
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
		return optional(in.length == 0 ? null : Arrays.stream(in).map(x -> vr.resolve(x)).filter(x -> isNotEmpty(x)).toArray(String[]::new));
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> stream(String[] in) {
		return Arrays.stream(in).map(x -> vr.resolve(x)).filter(x -> isNotEmpty(x));
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> cdl(String in) {
		return Arrays.stream(split(vr.resolve(in))).filter(x -> isNotEmpty(x));
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
		return optional(in.length == 0 ? null : in);
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
