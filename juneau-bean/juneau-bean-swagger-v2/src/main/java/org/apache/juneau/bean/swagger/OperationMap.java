/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionBuilders.*;

import java.util.*;

import org.apache.juneau.common.utils.*;

/**
 * Map meant for method-name/operation mappings.
 *
 * <p>
 * The OperationMap is a specialized TreeMap that represents the operations available on a single path in Swagger 2.0.
 * It forces entries to be sorted in a specific order to ensure consistent output. This map is used within PathItem
 * objects to define the HTTP methods and their corresponding operations.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The OperationMap represents the operations field in a Path Item Object, where each key is an HTTP method and each
 * value is an Operation object. The supported HTTP methods are:
 * <ul class='spaced-list'>
 * 	<li><c>get</c> ({@link Operation}) - A definition of a GET operation
 * 	<li><c>put</c> ({@link Operation}) - A definition of a PUT operation
 * 	<li><c>post</c> ({@link Operation}) - A definition of a POST operation
 * 	<li><c>delete</c> ({@link Operation}) - A definition of a DELETE operation
 * 	<li><c>options</c> ({@link Operation}) - A definition of an OPTIONS operation
 * 	<li><c>head</c> ({@link Operation}) - A definition of a HEAD operation
 * 	<li><c>patch</c> ({@link Operation}) - A definition of a PATCH operation
 * </ul>
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
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	OperationMap <jv>operations</jv> = <jsm>operationMap</jsm>()
 * 		.append(<js>"get"</js>, <jsm>operation</jsm>().setSummary(<js>"Get users"</js>))
 * 		.append(<js>"post"</jsm>, <jsm>operation</jsm>().setSummary(<js>"Create user"</js>));
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>operations</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>operations</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"get"</js>: { <js>"summary"</js>: <js>"Get users"</js> },
 * 		<js>"post"</js>: { <js>"summary"</js>: <js>"Create user"</js> }
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#path-item-object">Swagger 2.0 Specification &gt; Path Item Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/paths-and-operations/">Swagger Paths and Operations</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 *
 * @serial exclude
 */
public class OperationMap extends TreeMap<String,Operation> {
	private static final long serialVersionUID = 1L;

	private static final Comparator<String> OP_SORTER = new Comparator<>() {
		private final Map<String,String> methods = mapBuilder(String.class, String.class).add("get", "0").add("put", "1").add("post", "2").add("delete", "3").add("options", "4").add("head", "5")
			.add("patch", "6").build();

		@Override
		public int compare(String o1, String o2) {
			// Since keys are now stored in lowercase, we need to normalize them for comparison
			var s1 = methods.get(emptyIfNull(o1).toLowerCase());
			var s2 = methods.get(emptyIfNull(o2).toLowerCase());
			if (s1 == null)
				s1 = emptyIfNull(o1).toLowerCase();
			if (s2 == null)
				s2 = emptyIfNull(o2).toLowerCase();
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
		put(httpMethodName, operation);
		return this;
	}

	/**
	 * Override put to normalize keys to lowercase.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value associated with key, or null if there was no mapping for key.
	 */
	@Override
	public Operation put(String key, Operation value) {
		return super.put(emptyIfNull(key).toLowerCase(), value);
	}
}