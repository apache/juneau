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

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

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
 * Implementations must include either a public no-args constructor or a public constructor that takes in a single
 * {@link ContextProperties} object.
 */
public interface HttpPartParser {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represent "no" part parser.
	 *
	 * <p>
	 * Used to represent the absence of a part parser in annotations.
	 */
	public static interface Null extends HttpPartParser {}

	/**
	 * Instantiates a creator for a part parser.
	 * @return A new creator.
	 */
	public static Creator creator() {
		return new Creator();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Creator
	//-----------------------------------------------------------------------------------------------------------------

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

		@Override
		public Creator impl(HttpPartParser value) {
			super.impl(value);
			return this;
		}

		@Override
		public Creator type(Class<? extends HttpPartParser> value) {
			super.type(value);
			return this;
		}

		@Override
		public Creator copy() {
			return new Creator(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new parser session.
	 *
	 * @param args The runtime arguments for the session.
	 * @return A new parser session.
	 */
	public HttpPartParserSession createPartSession(ParserSessionArgs args);

	/**
	 * Returns metadata about the specified class.
	 *
	 * @param <T> The class type.
	 * @param c The class type.
	 * @return Metadata about the specified class.
	 */
	public <T> ClassMeta<T> getClassMeta(Class<T> c);

	/**
	 * Returns metadata about the specified class.
	 *
	 * @param <T> The class type.
	 * @param t The class type.
	 * @param args The class type args.
	 * @return Metadata about the specified class.
	 */
	public <T> ClassMeta<T> getClassMeta(Type t, Type...args);
}
