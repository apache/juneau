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
 * Session object that lives for the duration of a single use of {@link HttpPartParser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public interface HttpPartParserSession {

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException;

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException;

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The POJO type arguments for Collection and Map types.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException;

	/**
	 * Same as {@link #parse(HttpPartType, HttpPartSchema, String, ClassMeta)} but defaults to a <jk>null</jk> part type.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException;

	/**
	 * Same as {@link #parse(HttpPartType, HttpPartSchema, String, Class)} but defaults to a <jk>null</jk> part type.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException;

	/**
	 * Same as {@link #parse(HttpPartType, HttpPartSchema, String, Type, Type...)} but defaults to a <jk>null</jk> part type.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The POJO type arguments for Collection and Map types.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException;
}
