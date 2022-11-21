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
package org.apache.juneau.common.internal;

import java.text.*;

/**
 * Method argument utility methods.
 */
public class ArgUtils {

	/**
	 * Throws an {@link IllegalArgumentException} if the specified argument is <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.internal.ArgUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(String <jv>foo</jv>) {
	 *		<jsm>assertArgNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param <T> The argument data type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same argument.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final <T> T assertArgNotNull(String name, T o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		return o;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified expression is <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.internal.ArgUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(List&lt;String&gt; <jv>foo</jv>) {
	 *		<jsm>assertArg</jsm>(<jv>foo</jv> != <jk>null</jk> &amp;&amp; ! <jv>foo</jv>.isEmpty(), <js>"'foo' cannot be null or empty."</js>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param expression The boolean expression to check.
	 * @param msg The exception message.
	 * @param args The exception message args.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArg(boolean expression, String msg, Object...args) throws IllegalArgumentException {
		if (! expression)
			throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified value doesn't have all subclasses of the specified type.
	 *
	 * @param <E> The element type.
	 * @param name The argument name.
	 * @param type The expected parent class.
	 * @param value The array value being checked.
	 * @return The value cast to the specified array type.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	@SuppressWarnings("unchecked")
	public static final <E> Class<E>[] assertClassArrayArgIsType(String name, Class<E> type, Class<?>[] value) throws IllegalArgumentException {
		for (int i = 0; i < value.length; i++)
			if (! type.isAssignableFrom(value[i]))
				throw new IllegalArgumentException("Arg "+name+" did not have arg of type "+type.getName()+" at index "+i+": "+value[i].getName());
		return (Class<E>[])value;
	}

}
