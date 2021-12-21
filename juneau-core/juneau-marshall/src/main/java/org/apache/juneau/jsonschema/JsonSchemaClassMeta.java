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

import static org.apache.juneau.internal.ThrowableUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Metadata on classes specific to the JSON-Schema serializer and pulled from the {@link Schema @Schema} annotation on
 * the class.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class JsonSchemaClassMeta extends ExtendedClassMeta {

	private final OMap schema = new OMap();

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp JSON-schema metadata provider (for finding information about other artifacts).
	 */
	public JsonSchemaClassMeta(ClassMeta<?> cm, JsonSchemaMetaProvider mp) {
		super(cm);
		try {
			for (Schema a : cm.getAnnotations(Schema.class))
				schema.append(SchemaAnnotation.asMap(a));
		} catch (ParseException e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Returns the {@link Schema @Schema} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected OMap getSchema() {
		return schema;
	}
}
