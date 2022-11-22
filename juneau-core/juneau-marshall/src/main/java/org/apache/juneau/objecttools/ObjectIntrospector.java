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
package org.apache.juneau.objecttools;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;

/**
 * POJO method introspector.
 *
 * <p>
 * 	This class is used to invoke methods on {@code Objects} using arguments in serialized form.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>string1</jv> = <js>"foobar"</js>;
 * 	String <jv>string2</jv> = ObjectIntrospector
 * 		.create(<jv>string</jv>)
 * 		.invoke(String.<jk>class</jk>, <js>"substring(int,int)"</js>, <js>"[3,6]"</js>);  <jc>// "bar"</jc>
 * </p>
 * <p>
 * 	The arguments passed to the identified method are POJOs serialized in JSON format.  Arbitrarily complex arguments can be passed
 * 	in as arguments.
 * </p>
 * <ul>
 * 	<li class='warn'>This is an extremely powerful but potentially dangerous tool.  Use wisely.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class ObjectIntrospector {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
	 * @param parser The parser to use to parse the method arguments.
	 * @return A new {@link ObjectIntrospector} object.
	 */
	public static ObjectIntrospector create(Object o, ReaderParser parser) {
		return new ObjectIntrospector(o, parser);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Object object;
	private final ReaderParser parser;

	/**
	 * Constructor.
	 *
	 * @param object The object on which Java methods will be invoked.
	 * @param parser The parser to use to parse the method arguments.
	 * If <jk>null</jk>, {@link JsonParser#DEFAULT} is used.
	 */
	public ObjectIntrospector(Object object, ReaderParser parser) {
		if (parser == null)
			parser = JsonParser.DEFAULT;
		this.object = object;
		this.parser = parser;
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> ObjectIntrospector(o, <jk>null</jk>);</code>
	 *
	 * @param o The object on which Java methods will be invoked.
	 */
	public ObjectIntrospector(Object o) {
		this(o, null);
	}

	/**
	 * Primary method.
	 *
	 * <p>
	 * Invokes the specified method on this bean.
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
	 */
	public Object invokeMethod(Method method, Reader args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, ParseException, IOException {
		if (object == null)
			return null;
		Object[] params = args == null ? null : parser.parseArgs(args, method.getGenericParameterTypes());
		return method.invoke(object, params);
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
	 */
	public <T> T invokeMethod(Class<T> returnType, Method method, Reader args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, ParseException, IOException {
		return returnType.cast(invokeMethod(method, args));
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
	 */
	public Object invokeMethod(String method, String args) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, IllegalAccessException, ParseException, IOException {
		if (object == null)
			return null;
		Method m = parser.getBeanContext().getClassMeta(object.getClass()).getPublicMethods().get(method);
		if (m == null)
			throw new NoSuchMethodException(method);
		return invokeMethod(m, args == null ? null : new StringReader(args));
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
	 */
	public <T> T invokeMethod(Class<T> returnType, String method, String args) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, IllegalAccessException, ParseException, IOException {
		return returnType.cast(invokeMethod(method, args));
	}
}
