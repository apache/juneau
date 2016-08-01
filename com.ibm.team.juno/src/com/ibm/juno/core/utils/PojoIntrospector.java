/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;

/**
 * Used to invoke methods on {@code Objects} using arguments in serialized form.
 *	<p>
 *	Example:
 *	<p class='bcode'>
 *		String s = <js>"foobar"</js>;
 *		String s2 = (String)<jk>new</jk> PojoIntrospector(s).invoke(<js>"substring(int,int)"</js>, <js>"[3,6]"</js>);  <jc>// "bar"</jc>
 *	</p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
	 * 	These will automatically be converted to the appropriate object type if possible.<br>
	 * 	Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul>
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
		ClassMeta<?>[] argTypes = p.getBeanContext().getClassMetas(method.getParameterTypes());
		Object[] params = args == null ? null : p.parseArgs(args, -1, argTypes);
		return method.invoke(o, params);
	}

	/**
	 * Convenience method for invoking argument from method signature (@see {@link ClassUtils#getMethodSignature(Method)}.
	 *
	 * @param method The method being invoked.
	 * @param args The arguments to pass as parameters to the method.<br>
	 * 	These will automatically be converted to the appropriate object type if possible.<br>
	 * 	Can be <jk>null</jk> if method has no arguments.
	 * @return The object returned by the call to the method, or <jk>null</jk> if target object is <jk>null</jk>.
	 * @throws NoSuchMethodException If method does not exist.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul>
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
		Method m = p.getBeanContext().getClassMeta(o.getClass()).getPublicMethods().get(method);
		if (m == null)
			throw new NoSuchMethodException(method);
		return invokeMethod(m, args == null ? null : new StringReader(args));
	}
}
