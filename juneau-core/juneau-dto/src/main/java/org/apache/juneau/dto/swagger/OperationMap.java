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
		private final Map<String,Integer> methods = new AMap<String,Integer>()
			.append("get",7)
			.append("put",6)
			.append("post",5)
			.append("delete",4)
			.append("options",3)
			.append("head",2)
			.append("patch",1);

		@Override
		public int compare(String o1, String o2) {
			Integer i1 = methods.get(o1);
			Integer i2 = methods.get(o2);
			if (i1 == null)
				i1 = 0;
			if (i2 == null)
				i2 = 0;
			return i2.compareTo(i1);
		}
	};

	/**
	 * Constructor.
	 */
	public OperationMap() {
		super(OP_SORTER);
	}
	
	/**
	 * Fluent-style {@link #put(String, Operation)} method.
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
