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

import static org.apache.juneau.internal.ClassUtils.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.urlencoding.*;

/**
 * @deprecated Internal class.
 */
@Deprecated
public class RemoteMethodArg {

	/** The argument name.  Can be blank. */
	public final String name;

	/** The zero-based index of the argument on the Java method. */
	public final int index;

	/** The value is skipped if it's null/empty. */
	public final boolean skipIfNE;

	/** The serializer used for converting objects to strings. */
	public final HttpPartSerializer serializer;

	/**
	 * Constructor.
	 *
	 * @param name The argument name pulled from name().
	 * @param name2 The argument name pulled from value().
	 * @param index The zero-based index of the argument on the Java method.
	 * @param skipIfNE The value is skipped if it's null/empty.
	 * @param serializer
	 * 	The class to use for serializing headers, query parameters, form-data parameters, and path variables.
	 * 	If {@link UrlEncodingSerializer}, then the url-encoding serializer defined on the client will be used.
	 */
	protected RemoteMethodArg(String name, String name2, int index, boolean skipIfNE, Class<? extends HttpPartSerializer> serializer) {
		this.name = name.isEmpty() ? name2 : name;
		this.index = index;
		this.skipIfNE = skipIfNE;
		this.serializer = newInstance(HttpPartSerializer.class, serializer);
	}
}
