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
package org.apache.juneau.rest.httppart;

/**
 * Represents the possible parameter types as defined by the Swagger 2.0 specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
public enum RestPartType {

	/** Path variable */
	PATH("path"),

	/** Header value */
	HEADER("header"),

	/** Form data entry */
	FORM_DATA("formData"),

	/** Query parameter */
	QUERY("query"),

	/** Request body */
	BODY("body"),

	//-----------------------------------------------------------------------------------------------------------------
	// The following are additional parameter types not defined in Swagger
	//-----------------------------------------------------------------------------------------------------------------

	/** Response value */
	RESPONSE("response"),

	/** Response value */
	RESPONSE_BODY("responseBody"),

	/** Response header value */
	RESPONSE_HEADER("responseHeader"),

	/** Response status value */
	RESPONSE_CODE("responseCode"),

	/** Not a standard Swagger-defined field */
	OTHER("other");

	private final String value;

	private RestPartType(String value) {
		this.value = value;
	}

	/**
	 * Returns <jk>true</jk> if this type is any in the specified list.
	 *
	 * @param t The list to check against.
	 * @return <jk>true</jk> if this type is any in the specified list.
	 */
	public boolean isAny(RestPartType...t) {
		for (RestPartType tt : t)
			if (this == tt)
				return true;
		return false;
	}

	@Override /* Object */
	public String toString() {
		return value;
	}
}
