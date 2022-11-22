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
package org.apache.juneau.assertions;

import java.util.function.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;

/**
 * Utility class for performing simple validations on objects.
 *
 * <p>
 * Verifications that pass return a null string.  Verifications that don't pass return a string with a useful
 * error message.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Validates that our POJO is of type MyBean.</jc>
 * 	String <jv>errorMessage</jv> = <jsm>verify</jsm>(<jv>myPojo</jv>).isType(MyBean.<jk>class</jk>);
 * 	<jk>if</jk> (<jv>errorMessage</jv> != <jk>null</jk>)
 * 		<jk>throw new</jk> RuntimeException(<jv>errorMessage</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 */
public class Verify {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(Verify.class, "Messages");
	static final String
		MSG_unexpectedType = MESSAGES.getString("unexpectedType"),
		MSG_unexpectedValue = MESSAGES.getString("unexpectedValue");

	/**
	 * Create a new verifier object.
	 *
	 * @param o The object being verified.
	 * @return A new verifier object.
	 */
	public static Verify verify(Object o) {
		return new Verify(o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Object o;
	private Supplier<String> msg;

	/**
	 * Create a new verifier object.
	 *
	 * @param o The object being verified.
	 */
	protected Verify(Object o) {
		this.o = o;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Config setters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Overrides the default error message produced by the verification.
	 *
	 * @param msg The error message.
	 * @param args Optional message arguments.
	 * @return This object.
	 */
	public Verify msg(String msg, Object args) {
		this.msg = () -> StringUtils.format(msg, args);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Verifies that this object is of the specified type.
	 *
	 * @param type The type to test against.
	 * @return An error message if the object is not of the specified type, otherwise <jk>null</jk>.
	 */
	public String isType(Class<?> type) {
		if ((type == null && o == null) || (type != null && type.isInstance(o)))
			return null;
		return msg != null ? msg.get() : StringUtils.format(MSG_unexpectedType, type, (o == null ? null : o.getClass()));
	}

	/**
	 * Verifies that this object is equal to the specified object.
	 *
	 * @param expected The object to test against for equality.
	 * @return An error message if the object is not equal to the specified object, otherwise <jk>null</jk>.
	 */
	public String is(Object expected) {
		if (expected == o)
			return null;
		if (expected == null || o == null || ! expected.equals(o))
			return msg != null ? msg.get() : StringUtils.format(MSG_unexpectedValue, expected, o);
		return null;
	}

	/**
	 * Verifies that this object is equal to {@link Boolean#TRUE}.
	 *
	 * @return An error message if the object is not true, otherwise <jk>null</jk>.
	 */
	public String isTrue() {
		return is(true);
	}

	/**
	 * Verifies that this object is equal to {@link Boolean#FALSE}.
	 *
	 * @return An error message if the object is not false, otherwise <jk>null</jk>.
	 */
	public String isFalse() {
		return is(false);
	}
}
