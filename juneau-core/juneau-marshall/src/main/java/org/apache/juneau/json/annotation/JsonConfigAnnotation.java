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

import static org.apache.juneau.json.JsonParser.*;
import static org.apache.juneau.json.JsonSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link JsonConfig @JsonConfig} annotation.
 */
public class JsonConfigAnnotation {

	/**
	 * Applies {@link JsonConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends AnnotationApplier<JsonConfig,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(JsonConfig.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonConfig> ai, ContextPropertiesBuilder b) {
			JsonConfig a = ai.getAnnotation();

			bool(a.addBeanTypes()).ifPresent(x -> b.set(JSON_addBeanTypes, x));
			bool(a.escapeSolidus()).ifPresent(x -> b.set(JSON_escapeSolidus, x));
			bool(a.simpleMode()).ifPresent(x -> b.set(JSON_simpleMode, x));
			bool(a.validateEnd()).ifPresent(x -> b.set(JSON_validateEnd, x));
		}
	}
}