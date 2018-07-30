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
package org.apache.juneau.rest;

import org.apache.juneau.httppart.*;

/**
 * A simple pairing of a response object and metadata on how to serialize that response object.
 */
public class ResponseObject {

	private ResponseMeta meta;
	private Object value;

	/**
	 * Constructor.
	 *
	 * @param meta Metadata about the specified value.
	 * @param value The POJO that makes up the response.
	 */
	public ResponseObject(ResponseMeta meta, Object value) {
		this.meta = meta;
		this.value = value;
	}

	/**
	 * Returns the metadata about this response.
	 *
	 * @return
	 * 	The metadata about this response.
	 * 	<jk>Never <jk>null</jk>.
	 */
	public ResponseMeta getMeta() {
		return meta;
	}

	/**
	 * Returns the POJO that makes up this response.
	 *
	 * @return
	 * 	The POJO that makes up this response.
	 * 	<jk>Never <jk>null</jk>.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns <jk>true</jk> if this response object is of the specified type.
	 *
	 * @param c The type to check against.
	 * @return <jk>true</jk> if this response object is of the specified type.
	 */
	public boolean isType(Class<?> c) {
		return c.isInstance(value);
	}

	/**
	 * Returns this value cast to the specified class.
	 *
	 * @param c The class to cast to.
	 * @return This value cast to the specified class.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> c) {
		return (T)value;
	}

	/**
	 * Sets the POJO value for this response.
	 *
	 * @param value The POJO value to set.
	 */
	public void setValue(Object value) {
		this.value = value;
	}
}
