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

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.bean.*;

/**
 * Represents the metadata about an {@link Request}-annotated argument of a method on a REST proxy class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public final class RemoteOperationBeanArg {

	private final int index;
	private final RequestBeanMeta meta;

	RemoteOperationBeanArg(int index, RequestBeanMeta meta) {
		this.index = index;
		this.meta = meta;
	}

	/**
	 * Returns the index of the parameter in the method that is a request bean.
	 *
	 * @return The index of the parameter in the method that is a request bean.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns metadata on the request bean.
	 *
	 * @return Metadata about the bean.
	 */
	public RequestBeanMeta getMeta() {
		return meta;
	}
}
