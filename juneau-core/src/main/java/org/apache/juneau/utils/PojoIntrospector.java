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
package org.apache.juneau.utils;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * Used to invoke methods on {@code Objects} using arguments in serialized form.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	String s = <js>"foobar"</js>;
 * 	String s2 = (String)<jk>new</jk> PojoIntrospector(s).invoke(<js>"substring(int,int)"</js>, <js>"[3,6]"</js>);  <jc>// "bar"</jc>
 * </p>
 */
public final class PojoIntrospector {

	private final Object o;
	private final ReaderParser p;

	/**
	 * Constructor.
	 *
	 * @param o The object on which Java methods will be invoked.
	 * @param p The parser to use to parse the method arguments.  If <jk>null</jk>, {@link JsonParser#DEFAULT} is used.
	 */
	public PojoIntrospector(Object o, ReaderParser p) {
		if (p == null)
			p = JsonParser.DEFAULT;
		this.o = o;
		this.p = p;
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> PojoIntrospector(o, <jk>null</jk>);</code>
	 *
	 * @param o The object on which Java methods will be invoked.
	 */
	public PojoIntrospector(Object o) {
		this(o, null);
	}

	/**
	 * Primary method.  Invokes the specified method on this bean.
	 *
	 * @param method The method being invoked.
	 * @param args The arguments to pass as parameters to the method.<br>
	 * These will automatically be converted to the appropriate object type if possible.<br>
	 * Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>The number of actual and formal parameters differ.
	 * 		<li>An unwrapping conversion for primitive arguments fails.
	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
	 * 		<li>The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 * @throws IOException
	 */
	public Object invokeMethod(Method method, Reader args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, ParseException, IOException {
		if (o == null)
			return null;
		Object[] params = args == null ? null : p.parseArgs(args, method.getGenericParameterTypes());
		return method.invoke(o, params);
	}

	/**
	 * Convenience method for invoking argument from method signature (@see {@link ClassUtils#getMethodSignature(Method)}.
	 *
	 * @param method The method being invoked.
	 * @param args The arguments to pass as parameters to the method.<br>
	 * These will automatically be converted to the appropriate object type if possible.<br>
	 * Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws NoSuchMethodException If method does not exist.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>The number of actual and formal parameters differ.
	 * 		<li>An unwrapping conversion for primitive arguments fails.
	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
	 * 		<li>The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 * @throws IOException
	 */
	public Object invokeMethod(String method, String args) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, IllegalAccessException, ParseException, IOException {
		if (o == null)
			return null;
		Method m = p.getBeanContext().createSession().getClassMeta(o.getClass()).getPublicMethods().get(method);
		if (m == null)
			throw new NoSuchMethodException(method);
		return invokeMethod(m, args == null ? null : new StringReader(args));
	}
}
