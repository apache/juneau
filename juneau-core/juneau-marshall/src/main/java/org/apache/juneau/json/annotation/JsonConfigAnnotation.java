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
package org.apache.juneau.json.annotation;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link JsonConfig @JsonConfig} annotation.
 */
public class JsonConfigAnnotation {

	/**
	 * Applies {@link JsonConfig} annotations to a {@link JsonSerializerBuilder}.
	 */
	public static class SerializerApply extends AnnotationApplier<JsonConfig,JsonSerializerBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(JsonConfig.class, JsonSerializerBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonConfig> ai, JsonSerializerBuilder b) {
			JsonConfig a = ai.getAnnotation();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesJson(x));
			bool(a.escapeSolidus()).ifPresent(x -> b.escapeSolidus(x));
			bool(a.simpleMode()).ifPresent(x -> b.simpleMode(x));
		}
	}

	/**
	 * Applies {@link JsonConfig} annotations to a {@link JsonParserBuilder}.
	 */
	public static class ParserApply extends AnnotationApplier<JsonConfig,JsonParserBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(JsonConfig.class, JsonParserBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonConfig> ai, JsonParserBuilder b) {
			JsonConfig a = ai.getAnnotation();

			bool(a.validateEnd()).ifPresent(x -> b.validateEnd(x));
		}
	}
}