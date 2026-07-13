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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;

/**
 * Class used to add properties to a context builder (e.g. {@link MarshallingContext.Builder}) from an annotation (e.g. {@link MarshalledConfig}).
 *
 * <p>
 * Used by {@link Context.Builder#applyAnnotations(Class...)} and {@link Context.Builder#applyAnnotations(Object...)} to apply
 * annotations to context beans.
 *
 * <p>
 * The following code shows the general design pattern.
 *
 * <p class='bjava'>
 * 	<jc>// The annotation applied to classes and methods.</jc>
 * 	<ja>@Target</ja>({METHOD,TYPE})
 * 	<ja>@Retention</ja>(<jsf>RUNTIME</jsf>)
 * 	<ja>@ContextApply</ja>(MarshalledConfigAnnotationApplier.<jk>class</jk>)
 * 	<jk>public</jk> <jk>@interface </jk>MarshalledConfig {
 *
 * 		String unsortedProperties() <jk>default</jk> <js>""</js>;
 *
 * 	}
 *
 * 	<jc>// The applier that applies the annotation to the bean context builder.</jc>
 * 	<jk>public class</jk> MarshalledConfigAnnotationApplier <jk>extends</jk> AnnotationApplier&lt;<ja>MarshalledConfig</ja>,MarshallingContext.Builder<?>&gt; {
 *
 *		<jc>// Required constructor. </jc>
 * 		<jk>public</jk> Applier(VarResolverSession <jv>vr</jv>) {
 * 			<jk>super</jk>(MarshalledConfig.<jk>class</jk>, MarshallingContext.Builder.<jk>class</jk>, <jv>vr</jv>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> apply(AnnotationInfo&lt;MarshalledConfig&gt; <jv>annotationInfo</jv>, MarshallingContext.Builder<?> <jv>builder</jv>) {
 * 			<ja>MarshalledConfig</ja> <jv>beanConfig</jv> = <jv>annotationInfo</jv>.getAnnotation();
 *
 * 			String <jv>unsortedProperties</jv> = <jv>beanConfig</jv>.unsortedProperties();
 * 			<jk>if</jk> (! <jv>unsortedProperties</jv>.isEmpty())
 * 				<jv>builder</jv>.unsortedProperties(Boolean.<jsm>parseBoolean</jsm>(<jv>unsortedProperties</jv>));
 * 		}
 * 	}
 *
 *	<jc>// An annotated class opting out of the default sorted behavior.</jc>
 * 	<ja>@MarshalledConfig</ja>(unsortedProperties=<js>"true"</js>)
 * 	<jk>public class</jk> AnnotatedClass {}
 *
 *	<jc>// Putting it together.</jc>
 * 	<jk>public static void</jk> main(String[] <jv>args</jv>) {
 *
 *		<jc>// Create a JSON serializer where AnnotatedClass uses natural JVM order.</jc>
 * 		Serializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().applyAnnotations(AnnotatedClass.<jk>class</jk>).build();
 * 	}
 * </p>
 *
 * @param <A> The annotation that this applier reads from.
 * @param <B> The builder class to apply the annotation to.
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public abstract class AnnotationApplier<A extends Annotation,B> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_annotationClass = "annotationClass";
	private static final String ARG_builderClass = "builderClass";
	private static final String ARG_vr = "vr";

	private final VarResolverSession vr;
	private final Class<A> ca;
	private final Class<B> cb;

	/**
	 * Constructor.
	 *
	 * @param annotationClass The annotation class.
	 * @param builderClass The builder class.
	 * @param varResolverSession The string resolver to use for resolving strings.
	 */
	protected AnnotationApplier(Class<A> annotationClass, Class<B> builderClass, VarResolverSession varResolverSession) {
		ca = assertArgNotNull(ARG_annotationClass, annotationClass);
		cb = assertArgNotNull(ARG_builderClass, builderClass);
		vr = assertArgNotNull(ARG_vr, varResolverSession);
	}

	/**
	 * Apply the specified annotation to the specified property store builder.
	 *
	 * @param annotationInfo The annotation.
	 * @param builder The property store builder.
	 */
	public abstract void apply(AnnotationInfo<A> annotationInfo, B builder);

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
	 * Returns <jk>true</jk> if this apply can be appied to the specified builder.
	 *
	 * @param builder The builder to check.
	 * @return <jk>true</jk> if this apply can be appied to the specified builder.
	 */
	public boolean canApply(Object builder) {
		return cb.isInstance(builder);
	}

	private Character toCharacter(String in, String loc) {
		if (in.length() != 1)
			throw new ConfigException("Invalid syntax for character on annotation @{0}({1}): {2}", ca.getSimpleName(), loc, in);
		return in.charAt(0);
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> cdl(String in) {
		return Arrays.stream(splita(vr.resolve(in))).filter(Shorts::ine);
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
	 * Returns the specified class array as an {@link Optional}.
	 *
	 * <p>
	 * If the array is empty, then returns {@link Optional#empty()}.
	 *
	 * @param in The class array.
	 * @return The array wrapped in an {@link Optional}.
	 */
	protected Optional<Class<?>[]> classes(Class<?>[] in) {
		return o(in.length == 0 ? null : in);
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
			throw new ConfigException(e, "Invalid syntax for integer on annotation @{0}({1}): {2}", ca.getSimpleName(), loc, in);
		}
	}

	/**
	 * Resolves the specified string as a comma-delimited list of strings.
	 *
	 * @param in The CDL string containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	protected Stream<String> stream(String[] in) {
		return Arrays.stream(in).map(vr::resolve).filter(Shorts::ine);
	}

	/**
	 * Resolves the specified string.
	 *
	 * @param in The string containing variables to resolve.
	 * @return An optional containing the specified string if it exists, or {@link Optional#empty()} if it does not.
	 */
	protected Optional<String> string(String in) {
		in = vr.resolve(in);
		return o(isEmpty(in) ? null : in);
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
		return o(in.length == 0 ? null : Arrays.stream(in).map(vr::resolve).filter(Shorts::ine).toArray(String[]::new));
	}

	/**
	 * Returns the specified value if it's simple name is not <js>"void"</js>.
	 *
	 * @param <T> The value to return.
	 * @param in The value to return.
	 * @return An optional containing the specified value.
	 */
	protected <T> Optional<Class<T>> type(Class<T> in) {
		return o(in).filter(NOT_VOID);
	}

	/**
	 * Returns the var resolver session for this apply.
	 *
	 * @return The var resolver session for this apply.
	 */
	protected VarResolverSession vr() {
		return vr;
	}
}