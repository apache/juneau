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
 * Base class for implementations of {@link HttpPartParser}
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public abstract class BaseHttpPartParser extends BeanContextable implements HttpPartParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public abstract static class Builder extends BeanContextable.Builder {

		/**
		 * Constructor.
		 */
		protected Builder() {
			super();
		}

		/**
		 * Copy constructor.
		 *
		 * @param builder The builder to copy.
		 */
		protected Builder(Builder builder) {
			super(builder);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected BaseHttpPartParser(Builder builder) {
		super(builder);
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param <T> The POJO type to transform the input into.
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
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
		return getPartSession().parse(partType, schema, in, toType);
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param <T> The POJO type to transform the input into.
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
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException {
		return getPartSession().parse(partType, schema, in, getClassMeta(toType));
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param <T> The POJO type to transform the input into.
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The generic type arguments of the POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException {
		return getPartSession().parse(partType, schema, in, getClassMeta(toType, toTypeArgs));
	}

	@Override /* HttpPartParser */
	public <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return BeanContext.DEFAULT.getClassMeta(c);
	}

	@Override /* HttpPartParser */
	public <T> ClassMeta<T> getClassMeta(Type t, Type...args) {
		return BeanContext.DEFAULT.getClassMeta(t, args);
	}
}
