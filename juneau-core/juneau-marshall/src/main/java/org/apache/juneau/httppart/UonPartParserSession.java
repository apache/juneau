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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UonPartParser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
@SuppressWarnings({ "unchecked" })
public class UonPartParserSession extends UonParserSession implements HttpPartParserSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected UonPartParserSession(UonPartParser ctx, ParserSessionArgs args) {
		super(ctx, args);
	}

	/**
	 * Convenience method for parsing a part to a map or collection.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @param args The type arguments of the map or collection.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, java.lang.reflect.Type type, java.lang.reflect.Type...args) throws ParseException, SchemaValidationException {
		return (T)parse(null, schema, in, getClassMeta(type, args));
	}

	/**
	 * Convenience method for parsing a part.
	 *
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartSchema schema, String in, Class<T> type) throws ParseException, SchemaValidationException {
		return parse(null, schema, in, getClassMeta(type));
	}

	/**
	 * Convenience method for parsing a part.
	 *
	 * @param partType
	 * 	The part type being parsed.
	 * 	<br>May be <jk>null</jk>.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @return The parsed value.
	 * @throws ParseException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, String in, ClassMeta<T> type) throws ParseException, SchemaValidationException {
		return parse(partType, null, in, type);
	}

	@Override /* HttpPartParser */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) throws ParseException, SchemaValidationException {
		if (in == null)
			return null;
		if (type.isString() && in.length() > 0) {
			// Shortcut - If we're returning a string and the value doesn't start with "'" or is "null", then
			// just return the string since it's a plain value.
			// This allows us to bypass the creation of a UonParserSession object.
			char x = firstNonWhitespaceChar(in);
			if (x != '\'' && x != 'n' && in.indexOf('~') == -1)
				return (T)in;
			if (x == 'n' && "null".equals(in))
				return null;
		}
		try (ParserPipe pipe = createPipe(in)) {
			try (UonReader r = getUonReader(pipe, false)) {
				return parseAnything(type, r, null, true, null);
			}
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
