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
package org.apache.juneau.client;

import java.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;

/**
 * Convenience class for constructing instances of <code>List&lt;NameValuePair&gt;</code>
 * 	for the {@link UrlEncodedFormEntity} class.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs()
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"j_username"</js>, user))
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"j_password"</js>, pw));
 * 	request.setEntity(<jk>new</jk> UrlEncodedFormEntity(params));
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class NameValuePairs extends LinkedList<NameValuePair> {

	private static final long serialVersionUID = 1L;

	/**
	 * Appends the specified pair to the end of this list.
	 *
	 * @param pair The pair to append to this list.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(NameValuePair pair) {
		super.add(pair);
		return this;
	}
}
