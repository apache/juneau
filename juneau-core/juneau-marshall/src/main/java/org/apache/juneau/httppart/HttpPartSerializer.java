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
package org.apache.juneau.httppart;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;

/**
 * Interface used to convert POJOs to simple strings in HTTP headers, query parameters, form-data parameters, and URI
 * path variables.
 *
 * <p>
 * The following default implementations are provided:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link org.apache.juneau.oapi.OpenApiSerializer} - Parts encoded based on OpenAPI schema.
 * 	<li class='jc'>{@link org.apache.juneau.uon.UonSerializer} - Parts encoded in UON notation.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.SimplePartSerializer} - Parts encoded in plain text.
 * </ul>
 *
 * <p>
 * This class is used in the following locations:
 * <ul class='javatree'>
 * 	<li class='ja'>{@link FormData#serializer()}
 * 	<li class='ja'>{@link Query#serializer()}
 * 	<li class='ja'>{@link Header#serializer()}
 * 	<li class='ja'>{@link Path#serializer()}
 * 	<li class='ja'>{@link Request#serializer()}
 * 	<li class='ja'>{@link Response#serializer()}
 * 	<li class='jc'><c>RestClient.Builder.partSerializer(Class)</c>
 * </ul>
 *
 * <p>
 * Implementations must include either a public no-args constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public interface HttpPartSerializer {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represent "no" part part serializer.
	 *
	 * <p>
	 * Used to represent the absence of a part serializer in annotations.
	 */
	public static interface Void extends HttpPartSerializer {}

	/**
	 * Instantiates a creator for a part serializer.
	 * @return A new creator.
	 */
	public static Creator creator() {
		return new Creator();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Creator
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A creator for a part serializer.
	 */
	public static class Creator extends ContextBeanCreator<HttpPartSerializer> {

		Creator() {
			super(HttpPartSerializer.class);
		}

		Creator(Creator builder) {
			super(builder);
		}

		@Override
		public Creator impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override
		public Creator type(Class<? extends HttpPartSerializer> value) {
			super.type(value);
			return this;
		}

		@Override
		public Creator copy() {
			return new Creator(this);
		}

		/**
		 * Associates an existing bean context builder with this part serializer.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Creator beanContext(BeanContext.Builder value) {
			builder(BeanContextable.Builder.class).ifPresent(x -> x.beanContext(value));
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new serializer session.
	 *
	 * @return A new serializer session.
	 */
	public HttpPartSerializerSession getPartSession();
}
