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
package org.apache.juneau.oapi.annotation;

import static org.apache.juneau.oapi.OpenApiCommon.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link OpenApiConfig @OpenApiConfig} annotation.
 */
public class OpenApiConfigAnnotation {

	/**
	 * Applies {@link OpenApiConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<OpenApiConfig,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(OpenApiConfig.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<OpenApiConfig> ai, ContextPropertiesBuilder b) {
			OpenApiConfig a = ai.getAnnotation();

			b.setIfNotEmpty(OAPI_format, string(a.format()));
			b.setIfNotEmpty(OAPI_collectionFormat, string(a.collectionFormat()));
		}
	}
}