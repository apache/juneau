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
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Metadata on classes specific to the JSON-Schema serializer and pulled from the {@link Schema @Schema} annotation on
 * the class.
 */
public class JsonSchemaClassMeta extends ClassMetaExtended {

	private final ObjectMap schema;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @throws Exception If invalid <ja>@Schema</ja> definition was encountered.
	 */
	public JsonSchemaClassMeta(ClassMeta<?> cm) throws Exception {
		super(cm);
		Schema s = cm.getInfo().getAnnotation(Schema.class);
		schema = s == null ? ObjectMap.EMPTY_MAP : SchemaUtils.asMap(s);
	}

	/**
	 * Returns the {@link Schema @Schema} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected ObjectMap getSchema() {
		return schema;
	}
}
