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
package org.apache.juneau.http.annotation;

import java.lang.annotation.*;

import org.apache.juneau.httppart.*;

/**
 * A concrete implementation of the {@link Request} annotation.
 */
public class RequestAnnotation implements Request {

	private Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	private Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;

	@Override /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Request.class;
	}

	@Override /* Request */
	public Class<? extends HttpPartSerializer> serializer() {
		return serializer;
	}

	/**
	 * Sets the <c>serializer</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RequestAnnotation serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	@Override /* Request */
	public Class<? extends HttpPartParser> parser() {
		return parser;
	}

	/**
	 * Sets the <c>parser</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RequestAnnotation parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}
}
