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
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.BeanTraverseContext.*;
import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;

import org.apache.juneau.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link JsonSchemaConfig @JsonSchemaConfig} annotation.
 */
public class JsonSchemaConfigAnnotation {

	/**
	 * Applies {@link JsonSchemaConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<JsonSchemaConfig> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<JsonSchemaConfig> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonSchemaConfig> ai, ContextPropertiesBuilder b) {
			JsonSchemaConfig a = ai.getAnnotation();

			b.setIfNotEmpty(JSONSCHEMA_addDescriptionsTo, string(a.addDescriptionsTo()));
			b.setIfNotEmpty(JSONSCHEMA_addExamplesTo, string(a.addExamplesTo()));
			b.setIfNotEmpty(JSONSCHEMA_allowNestedDescriptions, bool(a.allowNestedDescriptions()));
			b.setIfNotEmpty(JSONSCHEMA_allowNestedExamples, bool(a.allowNestedExamples()));
			b.setIf(a.beanDefMapper() != BeanDefMapper.Null.class, JSONSCHEMA_beanDefMapper, a.beanDefMapper());
			b.setIfNotEmpty(JSONSCHEMA_ignoreTypes, string(a.ignoreTypes()));
			b.setIfNotEmpty(JSONSCHEMA_useBeanDefs, bool(a.useBeanDefs()));
			b.setIfNotEmpty(BEANTRAVERSE_detectRecursions, bool(a.detectRecursions()));
			b.setIfNotEmpty(BEANTRAVERSE_ignoreRecursions, bool(a.ignoreRecursions()));
			b.setIfNotEmpty(BEANTRAVERSE_initialDepth, integer(a.initialDepth(), "initialDepth"));
			b.setIfNotEmpty(BEANTRAVERSE_maxDepth, integer(a.maxDepth(), "maxDepth"));
		}
	}
}