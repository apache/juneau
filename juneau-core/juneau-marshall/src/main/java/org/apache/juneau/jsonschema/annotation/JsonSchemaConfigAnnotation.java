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

import org.apache.juneau.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link JsonSchemaConfig @JsonSchemaConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
public class JsonSchemaConfigAnnotation {

	/**
	 * Applies {@link JsonSchemaConfig} annotations to a {@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder}.
	 */
	public static class Apply extends AnnotationApplier<JsonSchemaConfig,JsonSchemaGenerator.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(JsonSchemaConfig.class, JsonSchemaGenerator.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonSchemaConfig> ai, JsonSchemaGenerator.Builder b) {
			JsonSchemaConfig a = ai.inner();

			string(a.addDescriptionsTo()).map(TypeCategory::parseArray).ifPresent(x -> b.addDescriptionsTo(x));
			string(a.addExamplesTo()).map(TypeCategory::parseArray).ifPresent(x -> b.addExamplesTo(x));
			bool(a.allowNestedDescriptions()).ifPresent(x -> b.allowNestedDescriptions(x));
			bool(a.allowNestedExamples()).ifPresent(x -> b.allowNestedExamples(x));
			type(a.beanDefMapper()).ifPresent(x -> b.beanDefMapper(x));
			string(a.ignoreTypes()).ifPresent(x -> b.ignoreTypes(x));
			bool(a.useBeanDefs()).ifPresent(x -> b.useBeanDefs(x));
			bool(a.detectRecursions()).ifPresent(x -> b.detectRecursions(x));
			bool(a.ignoreRecursions()).ifPresent(x -> b.ignoreRecursions(x));
			integer(a.initialDepth(), "initialDepth").ifPresent(x -> b.initialDepth(x));
			integer(a.maxDepth(), "maxDepth").ifPresent(x -> b.maxDepth(x));
		}
	}
}