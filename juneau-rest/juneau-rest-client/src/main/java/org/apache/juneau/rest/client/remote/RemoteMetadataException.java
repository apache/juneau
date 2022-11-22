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
package org.apache.juneau.rest.client.remote;

import java.lang.reflect.*;
import java.text.*;

import org.apache.juneau.*;

/**
 * Exceptions caused by invalid REST proxy classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 *
 * @serial exclude
 */
public class RemoteMetadataException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RemoteMetadataException(Throwable cause, String message, Object... args) {
		super(cause, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param m The interface method that has an invalid definition.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RemoteMetadataException(Method m, String message, Object...args) {
		this((Throwable)null, getMessage(m.getDeclaringClass(), m, message), args);
	}

	/**
	 * Constructor.
	 *
	 * @param c The interface class that has an invalid definition.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RemoteMetadataException(Class<?> c, String message, Object...args) {
		this((Throwable)null, getMessage(c, null, message), args);
	}

	private static final String getMessage(Class<?> c, Method m, String msg) {
		StringBuilder sb = new StringBuilder("Invalid remote definition found on class ").append(c.getName());
		if (m != null)
			sb.append(" on method ").append(m.getName());
		sb.append(". ").append(msg);
		return sb.toString();
	}
}
