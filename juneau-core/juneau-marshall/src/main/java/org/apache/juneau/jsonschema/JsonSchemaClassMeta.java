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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Metadata on classes specific to the JSON-Schema serializer and pulled from the {@link Schema @Schema} annotation on
 * the class.
 */
public class JsonSchemaClassMeta extends ClassMetaExtended {

	private final Schema jsonSchema;
	private final String type, format, description;
	private Object example;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 */
	public JsonSchemaClassMeta(ClassMeta<?> cm) {
		super(cm);
		this.jsonSchema = ClassUtils.getAnnotation(Schema.class, getInnerClass());
		if (jsonSchema != null) {
			type = nullIfEmpty(jsonSchema.type());
			format = nullIfEmpty(jsonSchema.format());
			description = nullIfEmpty(joinnl(jsonSchema.description()));
			try {
				example = jsonSchema.example().length == 0 ? null : JsonParser.DEFAULT.parse(joinnl(jsonSchema.example()), Object.class);
			} catch (ParseException e) {
				throw new BeanRuntimeException(e);
			}
		} else {
			type = null;
			format = null;
			description = null;
		}
	}

	/**
	 * Returns the {@link Schema @Schema} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected Schema getAnnotation() {
		return jsonSchema;
	}

	/**
	 * Returns the {@link Schema#type() @Schema(type)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getType() {
		return type;
	}

	/**
	 * Returns the {@link Schema#format() @Schema(format)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getFormat() {
		return format;
	}

	/**
	 * Returns the {@link Schema#description() @Schema(description)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getDescription() {
		return description;
	}

	/**
	 * Returns the {@link Schema#example() @Schema(example)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected Object getExample() {
		return example;
	}
}
