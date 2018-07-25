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
 * 	<li class='jc'>{@link org.apache.juneau.httppart.OpenApiPartParser} - Parts encoded in based on OpenAPI schema.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.UonPartParser} - Parts encoded in UON notation.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.SimplePartParser} - Parts encoded in plain text.
 * </ul>
 *
 * <p>
 * Implementations must include either a public no-args constructor or a public constructor that takes in a single
 * {@link PropertyStore} object.
 */
public interface HttpPartParser {

	/**
	 * Represent "no" part parser.
	 *
	 * <p>
	 * Used to represent the absence of a part parser in annotations.
	 */
	public static interface Null extends HttpPartParser {}

	/**
	 * Creates a new parser session.
	 *
	 * @param args The runtime arguments for the session.
	 * @return A new parser session.
	 */
	public HttpPartParserSession createSession(ParserSessionArgs args);

//	/**
//	 * Convenience method for creating a no-arg session and parsing a part.
//	 *
//	 * @param partType The category of value being parsed.
//	 * @param schema
//	 * 	Schema information about the part.
//	 * 	<br>May be <jk>null</jk>.
//	 * 	<br>Not all part parsers use the schema information.
//	 * @param in The value being parsed.
//	 * @param toType The POJO type to transform the input into.
//	 * @return The parsed value.
//	 * @throws ParseException If a problem occurred while trying to parse the input.
//	 * @throws SchemaValidationException If the input fails schema validation.
//	 */
//	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException;
//
	/**
	 * Convenience method for creating a no-arg session and parsing a part.
	 *
	 * @param partType The category of value being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The value being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException;

	/**
	 * Convenience method for creating a no-arg session and parsing a part.
	 *
	 * @param partType The category of value being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The value being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The POJO type arguments for Collections and Maps.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException;

//	/**
//	 * Convenience method for creating a no-arg session and parsing a part of an unspecified part type.
//	 *
//	 * @param schema
//	 * 	Schema information about the part.
//	 * 	<br>May be <jk>null</jk>.
//	 * 	<br>Not all part parsers use the schema information.
//	 * @param in The value being parsed.
//	 * @param toType The POJO type to transform the input into.
//	 * @return The parsed value.
//	 * @throws ParseException If a problem occurred while trying to parse the input.
//	 * @throws SchemaValidationException If the input fails schema validation.
//	 */
//	public <T> T parse(HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException;
//
	/**
	 * Convenience method for creating a no-arg session and parsing a part of an unspecified part type.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The value being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException;

	/**
	 * Convenience method for creating a no-arg session and parsing a part of an unspecified part type.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The value being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The POJO type arguments for Collections and Maps.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException;
}
