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
package org.apache.juneau.remoteable;

import java.lang.reflect.*;
import java.text.*;

import org.apache.juneau.*;

/**
 * Exceptions caused by invalid Remoteable classes.
 */
public class RemoteableMetadataException extends FormattedRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param m The interface method that has an invalid definition.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RemoteableMetadataException(Method m, String message, Object...args) {
		super(getMessage(m.getDeclaringClass(), m, message), args);
	}

	/**
	 * Constructor.
	 *
	 * @param c The interface class that has an invalid definition.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RemoteableMetadataException(Class<?> c, String message, Object...args) {
		super(getMessage(c, null, message), args);
	}

	private static final String getMessage(Class<?> c, Method m, String msg) {
		StringBuilder sb = new StringBuilder("Invalid remoteable definition found on class ").append(c.getName());
		if (m != null)
			sb.append(" on method ").append(m.getName());
		sb.append(". ").append(msg);
		return sb.toString();
	}
}
