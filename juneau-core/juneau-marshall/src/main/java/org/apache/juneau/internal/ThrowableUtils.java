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
package org.apache.juneau.internal;

import java.text.*;

import org.apache.juneau.*;

/**
 * Various utility methods for creating and working with throwables.
 */
public class ThrowableUtils {

	/**
	 * Shortcut for calling <code><jk>new</jk> IllegalArgumentException(MessageFormat.<jsm>format</jsm>(msg, args));</code>
	 *
	 * @param msg The message of the IllegalArgumentException.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static void illegalArg(String msg, Object...args) throws IllegalArgumentException {
		throw new BasicIllegalArgumentException(msg, args);
	}

	/**
	 * Same as {@link Throwable#getCause()} but searches the throwable chain for an exception of the specified type.
	 *
	 * @param c The throwable type to search for.
	 * @param <T> The throwable type to search for.
	 * @param t The throwable to search.
	 * @return The exception, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T getCause(Class<T> c, Throwable t) {
		while (t != null) {
			t = t.getCause();
			if (c.isInstance(t))
				return (T)t;
		}
		return null;
	}
}
