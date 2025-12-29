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
package org.apache.juneau.httppart;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;

/**
 * Interface used to convert HTTP headers, query parameters, form-data parameters, and URI path variables to POJOs
 *
 * <p>
 * The following default implementations are provided:
 * <ul class='doctree'>
 * 	<li class='jc'>{@link org.apache.juneau.oapi.OpenApiParser} - Parts encoded in based on OpenAPI schema.
 * 	<li class='jc'>{@link org.apache.juneau.uon.UonParser} - Parts encoded in UON notation.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.SimplePartParser} - Parts encoded in plain text.
 * </ul>
 *
 * <p>
 * Implementations must include either a public no-args constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public interface HttpPartParser {
	/**
	 * A creator for a part parser.
	 */
	public static class Creator extends ContextBeanCreator<HttpPartParser> {
		Creator() {
			super(HttpPartParser.class);
		}

		Creator(Creator creator) {
			super(creator);
		}

		/**
		 * Associates an existing bean context builder with this part parser.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Creator beanContext(BeanContext.Builder value) {
			builder(BeanContextable.Builder.class).ifPresent(x -> x.beanContext(value));
			return this;
		}

		@Override
		public Creator copy() {
			return new Creator(this);
		}

		@Override
		public Creator impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override
		public Creator type(Class<? extends HttpPartParser> value) {
			super.type(value);
			return this;
		}
	}

	/**
	 * Represent "no" part parser.
	 *
	 * <p>
	 * Used to represent the absence of a part parser in annotations.
	 */
	public interface Void extends HttpPartParser {}

	/**
	 * Instantiates a creator for a part parser.
	 * @return A new creator.
	 */
	static Creator creator() {
		return new Creator();
	}

	/**
	 * Returns metadata about the specified class.
	 *
	 * @param <T> The class type.
	 * @param c The class type.
	 * @return Metadata about the specified class.
	 */
	<T> ClassMeta<T> getClassMeta(Class<T> c);

	/**
	 * Returns metadata about the specified class.
	 *
	 * @param <T> The class type.
	 * @param t The class type.
	 * @param args The class type args.
	 * @return Metadata about the specified class.
	 */
	<T> ClassMeta<T> getClassMeta(Type t, Type...args);

	/**
	 * Creates a new parser session.
	 *
	 * @return A new parser session.
	 */
	HttpPartParserSession getPartSession();
}