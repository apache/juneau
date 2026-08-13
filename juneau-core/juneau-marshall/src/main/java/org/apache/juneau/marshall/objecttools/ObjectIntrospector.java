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
package org.apache.juneau.marshall.objecttools;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;

/**
 * POJO method introspector.
 *
 * <p>
 * 	This class is used to invoke methods on {@code Objects} using arguments in serialized form.
 * </p>
 *
 * <h5 class='section'>Security - secure by default:</h5>
 * <p>
 * 	As of Juneau 10.0, reflective method dispatch is <b>denied by default</b>.  Before calling
 * 	{@link #invokeMethod(String, String) invokeMethod(...)} (in any of its overloaded forms), the caller must
 * 	explicitly allow-list the method(s) that may be invoked via {@link #allow(Class, String...) allow(...)} or
 * 	{@link #allow(Predicate) allow(Predicate)}.  Invoking a method that isn't allow-listed throws
 * 	{@link MethodNotAllowlistedException}.
 * </p>
 * <p>
 * 	For trusted, in-process callers that need the pre-10.0 behavior of dispatching to any public,
 * 	non-deprecated method, call {@link #allowAll()} as a one-line migration.  <b>Never</b> call
 * 	{@link #allowAll()} on an introspector whose method name/arguments are derived from an untrusted
 * 	source (e.g. a REST request parameter) &mdash; allow-list the specific methods instead.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>string1</jv> = <js>"foobar"</js>;
 * 	String <jv>string2</jv> = ObjectIntrospector
 * 		.create(<jv>string1</jv>)
 * 		.allow(String.<jk>class</jk>, <js>"substring(int,int)"</js>)  <jc>// Explicit allow-list.</jc>
 * 		.invokeMethod(String.<jk>class</jk>, <js>"substring(int,int)"</js>, <js>"[3,6]"</js>);  <jc>// "bar"</jc>
 * </p>
 * <p>
 * 	For trusted, in-process-only use, {@link #allowAll()} can be used instead of an explicit allow-list:
 * </p>
 * <p class='bjava'>
 * 	String <jv>string2</jv> = ObjectIntrospector
 * 		.create(<jv>string1</jv>)
 * 		.allowAll()
 * 		.invokeMethod(String.<jk>class</jk>, <js>"substring(int,int)"</js>, <js>"[3,6]"</js>);  <jc>// "bar"</jc>
 * </p>
 * <p>
 * 	The arguments passed to the identified method are POJOs serialized in JSON format.  Arbitrarily complex arguments can be passed
 * 	in as arguments.
 * </p>
 * <ul>
 * 	<li class='warn'>This is an extremely powerful but potentially dangerous tool.  Use wisely.
 * </ul>
 *
 */
public class ObjectIntrospector {

	/**
	 * Static creator.
	 * @param o The object on which Java methods will be invoked.
	 * @return A new {@link ObjectIntrospector} object.
	 */
	public static ObjectIntrospector create(Object o) {
		return new ObjectIntrospector(o);
	}

	/**
	 * Static creator.
	 * @param o The object on which Java methods will be invoked.
	 * @param parser The parser to use to parse the method arguments.  If <jk>null</jk>, {@link Json5Parser#DEFAULT} is used.
	 * @return A new {@link ObjectIntrospector} object.
	 */
	public static ObjectIntrospector create(Object o, ReaderParser parser) {
		return new ObjectIntrospector(o, parser);
	}

	private final Object object;
	private final ReaderParser parser;

	/** Allow-list filter.  <jk>null</jk> means no methods are allow-listed (secure default = deny-all). */
	private Predicate<Method> allowed;

	/**
	 * Shortcut for calling <code><jk>new</jk> ObjectIntrospector(o, <jk>null</jk>);</code>
	 *
	 * @param o The object on which Java methods will be invoked.
	 */
	public ObjectIntrospector(Object o) {
		this(o, null);
	}

	/**
	 * Constructor.
	 *
	 * @param object The object on which Java methods will be invoked.  Can be <jk>null</jk> (subsequent method invocations return <jk>null</jk>).
	 * @param parser The parser to use to parse the method arguments.
	 * If <jk>null</jk>, {@link Json5Parser#DEFAULT} is used.
	 */
	public ObjectIntrospector(Object object, ReaderParser parser) {
		if (parser == null)
			parser = Json5Parser.DEFAULT;
		this.object = object;
		this.parser = parser;
	}

	/**
	 * Allow-lists methods matching the specified filter for invocation via {@link #invokeMethod}.
	 *
	 * <p>
	 * Can be called multiple times; the filters are OR'ed together, so a method is allowed if it matches
	 * <b>any</b> filter that was added.
	 *
	 * @param filter Filter that returns <jk>true</jk> for methods that may be invoked.
	 * @return This object.
	 */
	public ObjectIntrospector allow(Predicate<Method> filter) {
		if (filter != null)
			allowed = (allowed == null) ? filter : allowed.or(filter);
		return this;
	}

	/**
	 * Allow-lists specific method signatures declared on (or inherited by) the specified class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ObjectIntrospector.<jsm>create</jsm>(<jv>myBean</jv>).allow(MyBean.<jk>class</jk>, <js>"getName"</js>, <js>"getAge"</js>);
	 * </p>
	 *
	 * @param declaringClass The class the allow-listed methods must be declared on (or a subtype thereof).
	 * @param signatures
	 * 	One or more method signatures as returned by {@link MethodInfo#getSignature()} (e.g. <js>"getName"</js>,
	 * 	<js>"substring(int,int)"</js>).
	 * @return This object.
	 */
	public ObjectIntrospector allow(Class<?> declaringClass, String...signatures) {
		var sigs = Set.of(signatures);
		return allow(m -> declaringClass.isAssignableFrom(m.getDeclaringClass()) && sigs.contains(MethodInfo.of(m).getSignature()));
	}

	/**
	 * Disables allow-list enforcement, restoring the pre-10.0 behavior of allowing any public, non-deprecated
	 * method to be invoked.
	 *
	 * <p>
	 * <b>Use only for trusted, in-process callers.</b>  Never call this method on an introspector whose method
	 * name and/or arguments are sourced from an untrusted caller (e.g. parsed from an HTTP request) &mdash;
	 * allow-list the specific methods instead via {@link #allow(Class, String...) allow(...)}.
	 *
	 * @return This object.
	 */
	public ObjectIntrospector allowAll() {
		allowed = m -> true;
		return this;
	}

	/**
	 * Primary method.
	 *
	 * <p>
	 * Invokes the specified method on this bean.
	 *
	 * @param <T> The return type of the method call.
	 * @param returnType The return type of the method call.
	 * @param method The method being invoked.
	 * @param args
	 * 	The arguments to pass as parameters to the method.
	 * 	These will automatically be converted to the appropriate object type if possible.
	 * 	Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws IllegalAccessException
	 * 	If the <c>Constructor</c> object enforces Java language access control and the underlying constructor is
	 * 	inaccessible.
	 * @throws IllegalArgumentException
	 * 	If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			The number of actual and formal parameters differ.
	 * 		<li>
	 * 			An unwrapping conversion for primitive arguments fails.
	 * 		<li>
	 * 			A parameter value cannot be converted to the corresponding formal parameter type by a method invocation
	 * 			conversion.
	 * 		<li>
	 * 			The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @throws MethodNotAllowlistedException If the method has not been allow-listed via {@link #allow(Class, String...) allow(...)} or {@link #allowAll()}.
	 */
	public <T> T invokeMethod(Class<T> returnType, Method method, Reader args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, ParseException, IOException {
		return returnType.cast(invokeMethod(method, args));
	}

	/**
	 * Convenience method for invoking argument from method signature (@see {@link MethodInfo#getSignature()}.
	 *
	 * @param <T> The return type of the method call.
	 * @param returnType The return type of the method call.
	 * @param method The method being invoked.
	 * @param args
	 * 	The arguments to pass as parameters to the method.
	 * 	These will automatically be converted to the appropriate object type if possible.
	 * 	Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws NoSuchMethodException If method does not exist.
	 * @throws IllegalAccessException
	 * 	If the <c>Constructor</c> object enforces Java language access control and
	 * 	the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException
	 * 	If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			The number of actual and formal parameters differ.
	 * 		<li>
	 * 			An unwrapping conversion for primitive arguments fails.
	 * 		<li>
	 * 			A parameter value cannot be converted to the corresponding formal parameter type by a method invocation
	 * 			conversion.
	 * 		<li>
	 * 			The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @throws MethodNotAllowlistedException If the method has not been allow-listed via {@link #allow(Class, String...) allow(...)} or {@link #allowAll()}.
	 */
	public <T> T invokeMethod(Class<T> returnType, String method, String args)
		throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, IllegalAccessException, ParseException, IOException {
		return returnType.cast(invokeMethod(method, args));
	}

	/**
	 * Primary method.
	 *
	 * <p>
	 * Invokes the specified method on this bean.
	 *
	 * <p>
	 * The method must have been allow-listed via {@link #allow(Class, String...) allow(...)} (or
	 * {@link #allow(Predicate) allow(Predicate)}) or {@link #allowAll()} prior to calling this method, otherwise
	 * a {@link MethodNotAllowlistedException} is thrown.  See the class-level javadoc for details.
	 *
	 * @param method The method being invoked.
	 * @param args
	 * 	The arguments to pass as parameters to the method.
	 * 	These will automatically be converted to the appropriate object type if possible.
	 * 	Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws IllegalAccessException
	 * 	If the <c>Constructor</c> object enforces Java language access control and the underlying constructor is
	 * 	inaccessible.
	 * @throws IllegalArgumentException
	 * 	If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			The number of actual and formal parameters differ.
	 * 		<li>
	 * 			An unwrapping conversion for primitive arguments fails.
	 * 		<li>
	 * 			A parameter value cannot be converted to the corresponding formal parameter type by a method invocation
	 * 			conversion.
	 * 		<li>
	 * 			The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @throws MethodNotAllowlistedException If the method has not been allow-listed via {@link #allow(Class, String...) allow(...)} or {@link #allowAll()}.
	 */
	public Object invokeMethod(Method method, Reader args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, ParseException, IOException {
		if (object == null)
			return null;
		if (allowed == null || !allowed.test(method))
			throw new MethodNotAllowlistedException(
				"Method '%s' on class '%s' has not been allow-listed for reflective invocation via ObjectIntrospector. "
				+ "Call allow(declaringClass, signatures) to allow-list specific methods, or allowAll() to permit any public, "
				+ "non-deprecated method (trusted in-process callers only).",
				method.getName(), method.getDeclaringClass().getName());
		Object[] params = args == null ? null : parser.readArgs(args, method.getGenericParameterTypes());
		return method.invoke(object, params);
	}

	/**
	 * Convenience method for invoking argument from method signature (@see {@link MethodInfo#getSignature()}.
	 *
	 * @param method The method being invoked.
	 * @param args
	 * 	The arguments to pass as parameters to the method.
	 * 	These will automatically be converted to the appropriate object type if possible.
	 * 	Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws NoSuchMethodException If method does not exist.
	 * @throws IllegalAccessException
	 * 	If the <c>Constructor</c> object enforces Java language access control and
	 * 	the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException
	 * 	If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			The number of actual and formal parameters differ.
	 * 		<li>
	 * 			An unwrapping conversion for primitive arguments fails.
	 * 		<li>
	 * 			A parameter value cannot be converted to the corresponding formal parameter type by a method invocation
	 * 			conversion.
	 * 		<li>
	 * 			The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @throws MethodNotAllowlistedException If the method has not been allow-listed via {@link #allow(Class, String...) allow(...)} or {@link #allowAll()}.
	 */
	public Object invokeMethod(String method, String args) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, IllegalAccessException, ParseException, IOException {
		if (object == null)
			return null;
		var m = parser.getMarshallingContext()
			.getClassMeta(object.getClass())
			.getPublicMethods()
			.stream()
			.filter(x -> x.isNotDeprecated() && eq(x.getSignature(), method))
			.findFirst()
			.orElseThrow(() -> new NoSuchMethodException(method));
		return invokeMethod(m.inner(), args == null ? null : new StringReader(args));
	}
}
