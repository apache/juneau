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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public class JsonConfigAnnotation {

	/**
	 * Applies {@link JsonConfig} annotations to a {@link org.apache.juneau.json.JsonSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<JsonConfig,JsonSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(JsonConfig.class, JsonSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonConfig> ai, JsonSerializer.Builder b) {
			JsonConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesJson(x));
			bool(a.escapeSolidus()).ifPresent(x -> b.escapeSolidus(x));
			bool(a.simpleAttrs()).ifPresent(x -> b.simpleAttrs(x));
		}
	}

	/**
	 * Applies {@link JsonConfig} annotations to a {@link org.apache.juneau.json.JsonParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<JsonConfig,JsonParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(JsonConfig.class, JsonParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonConfig> ai, JsonParser.Builder b) {
			JsonConfig a = ai.inner();

			bool(a.validateEnd()).ifPresent(x -> b.validateEnd(x));
		}
	}
}