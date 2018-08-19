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

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Metadata on bean properties specific to the JSON-Schema pulled from the {@link JsonSchema @JsonSchema} annotation
 * on the bean property.
 */
public class JsonSchemaBeanPropertyMeta extends BeanPropertyMetaExtended {

	/**
	 * Default instance.
	 */
	public static final JsonSchemaBeanPropertyMeta DEFAULT = new JsonSchemaBeanPropertyMeta();

	private String type, format, description;
	private Object example;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 */
	public JsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		super(bpm);

		if (bpm.getField() != null)
			findInfo(bpm.getField().getAnnotation(JsonSchema.class));
		if (bpm.getGetter() != null)
			findInfo(bpm.getGetter().getAnnotation(JsonSchema.class));
		if (bpm.getSetter() != null)
			findInfo(bpm.getSetter().getAnnotation(JsonSchema.class));
	}

	private JsonSchemaBeanPropertyMeta() {
		super(null);
		this.type = null;
		this.format = null;
		this.description = null;
		this.example = null;
	}

	private void findInfo(JsonSchema js) {
		if (js == null)
			return;
		if (! js.type().isEmpty())
			type = js.type();
		if (! js.format().isEmpty())
			format = js.format();
		if (! js.description().isEmpty())
			description = js.description();
		if (! js.example().isEmpty()) {
			try {
				example = JsonParser.DEFAULT.parse(js.example(), Object.class);
			} catch (ParseException e) {
				throw new BeanRuntimeException(e);
			}
		}
	}

	/**
	 * Returns the {@link JsonSchema#type() @JsonSchema(type)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getType() {
		return type;
	}

	/**
	 * Returns the {@link JsonSchema#format() @JsonSchema(format)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getFormat() {
		return format;
	}

	/**
	 * Returns the {@link JsonSchema#description() @JsonSchema(description)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getDescription() {
		return description;
	}

	/**
	 * Returns the {@link JsonSchema#example() @JsonSchema(example)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected Object getExample() {
		return example;
	}
}
