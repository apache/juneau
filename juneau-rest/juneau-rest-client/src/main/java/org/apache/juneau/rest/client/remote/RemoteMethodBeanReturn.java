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

import static org.apache.juneau.internal.ClassUtils.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;

/**
 * Represents the metadata about an {@link Response}-annotated return type on a method on a REST proxy class.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
 * </ul>
 */
public final class RemoteMethodBeanReturn {

	private final ResponseBeanMeta meta;
	private final HttpPartParser parser;

	RemoteMethodBeanReturn(Class<? extends HttpPartParser> parser, ResponseBeanMeta meta) {
		this.parser = newInstance(HttpPartParser.class, parser);
		this.meta = meta;
	}

	/**
	 * Returns the parser to use for parsing parts on the response bean.
	 *
	 * @return The parser to use for parsing parts on the response bean, or <jk>null</jk> if not defined.
	 */
	public HttpPartParser getParser() {
		return parser;
	}

	/**
	 * Returns metadata on the response bean.
	 *
	 * @return Metadata about the bean.
	 */
	public ResponseBeanMeta getMeta() {
		return meta;
	}
}
