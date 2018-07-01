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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Map meant for method-name/operation mappings.
 *
 * <p>
 * Forces entries to be sorted in the following order:
 * <ul>
 * 	<li><code>GET</code>
 * 	<li><code>PUT</code>
 * 	<li><code>POST</code>
 * 	<li><code>DELETE</code>
 * 	<li><code>OPTIONS</code>
 * 	<li><code>HEAD</code>
 * 	<li><code>PATCH</code>
 * 	<li>Everything else.
 * </ul>
 */
public class OperationMap extends TreeMap<String,Operation> {
	private static final long serialVersionUID = 1L;

	private static final Comparator<String> OP_SORTER = new Comparator<String>() {
		private final Map<String,String> methods = new AMap<String,String>()
			.append("get","0")
			.append("put","1")
			.append("post","2")
			.append("delete","3")
			.append("options","4")
			.append("head","5")
			.append("patch","6");

		@Override
		public int compare(String o1, String o2) {
			String s1 = methods.get(o1);
			String s2 = methods.get(o2);
			if (s1 == null)
				s1 = o1;
			if (s2 == null)
				s2 = o2;
			return StringUtils.compare(s1, s2);
		}
	};

	/**
	 * Constructor.
	 */
	public OperationMap() {
		super(OP_SORTER);
	}

	/**
	 * Fluent-style put method.
	 *
	 * @param httpMethodName The HTTP method name.
	 * @param operation The operation.
	 * @return This method (for method chaining).
	 */
	public OperationMap append(String httpMethodName, Operation operation) {
		put(emptyIfNull(httpMethodName).toLowerCase(), operation);
		return this;
	}
}
