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
package org.apache.juneau.http.annotation;

/**
 * Static strings used for Swagger parameter types.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2/#data-types">Swagger 2.0 Data Types</a>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/#data-types">OpenAPI 3.0 Data Types</a>
 * </ul>
 */
public class ParameterType {

	/**
	 * Prevents instantiation.
	 */
	private ParameterType() {}

	/** String parameter type. */
	public static final String STRING = "string";

	/** Number parameter type. */
	public static final String NUMBER = "number";

	/** Integer parameter type. */
	public static final String INTEGER = "integer";

	/** Boolean parameter type. */
	public static final String BOOLEAN = "boolean";

	/** Array parameter type. */
	public static final String ARRAY = "array";

	/** Object parameter type. */
	public static final String OBJECT = "object";

	/** File parameter type. */
	public static final String FILE = "file";
}