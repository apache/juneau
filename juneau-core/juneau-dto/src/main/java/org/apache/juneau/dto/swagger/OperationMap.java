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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.common.internal.*;

/**
 * Map meant for method-name/operation mappings.
 *
 * <p>
 * Forces entries to be sorted in the following order:
 * <ul>
 * 	<li><c>GET</c>
 * 	<li><c>PUT</c>
 * 	<li><c>POST</c>
 * 	<li><c>DELETE</c>
 * 	<li><c>OPTIONS</c>
 * 	<li><c>HEAD</c>
 * 	<li><c>PATCH</c>
 * 	<li>Everything else.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 *
 * @serial exclude
 */
public class OperationMap extends TreeMap<String,Operation> {
	private static final long serialVersionUID = 1L;

	private static final Comparator<String> OP_SORTER = new Comparator<String>() {
		private final Map<String,String> methods = mapBuilder(String.class,String.class)
			.add("get","0").add("put","1").add("post","2").add("delete","3").add("options","4").add("head","5").add("patch","6")
			.build();

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
	 * @return This object.
	 */
	public OperationMap append(String httpMethodName, Operation operation) {
		put(emptyIfNull(httpMethodName).toLowerCase(), operation);
		return this;
	}
}
