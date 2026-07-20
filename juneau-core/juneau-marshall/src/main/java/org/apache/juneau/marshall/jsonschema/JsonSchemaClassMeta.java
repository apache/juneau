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
package org.apache.juneau.marshall.jsonschema;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.function.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Metadata on classes specific to the JSON-Schema serializer and pulled from the {@link Schema @Schema} annotation on
 * the class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonSchemaDetails">JSON-Schema Support</a>

 * </ul>
 */
public class JsonSchemaClassMeta extends ExtendedClassMeta {

	private final JsonMap schema = new JsonMap();

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.  Must not be <jk>null</jk>.
	 * @param mp JSON-schema metadata provider (for finding information about other artifacts).
	 */
	public JsonSchemaClassMeta(ClassMeta<?> cm, JsonSchemaMetaProvider mp) {
		super(cm);
		Consumer<Schema> c = x -> {
			try {
				schema.append(SchemaAnnotation.asMap(x));
			} catch (ParseException e) {
				throw toRex(e);
			}
		};
		cm.forEachAnnotation(Schema.class, x -> true, c);
	}

	/**
	 * Returns the {@link Schema @Schema} annotation defined on the class.
	 *
	 * @return The value of the annotation as a generic map.  Never <jk>null</jk> - returns an empty map if not specified.
	 */
	protected JsonMap getSchema() { return schema; }
}