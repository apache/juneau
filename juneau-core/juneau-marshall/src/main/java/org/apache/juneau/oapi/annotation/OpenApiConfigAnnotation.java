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

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link OpenApiConfig @OpenApiConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
public class OpenApiConfigAnnotation {

	/**
	 * Applies {@link OpenApiConfig} annotations to a {@link org.apache.juneau.oapi.OpenApiSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<OpenApiConfig,OpenApiSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(OpenApiConfig.class, OpenApiSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<OpenApiConfig> ai, OpenApiSerializer.Builder b) {
			OpenApiConfig a = ai.inner();

			string(a.format()).map(HttpPartFormat::valueOf).ifPresent(x -> b.format(x));
			string(a.collectionFormat()).map(HttpPartCollectionFormat::valueOf).ifPresent(x -> b.collectionFormat(x));
		}
	}

	/**
	 * Applies {@link OpenApiConfig} annotations to a {@link org.apache.juneau.oapi.OpenApiParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<OpenApiConfig,OpenApiParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(OpenApiConfig.class, OpenApiParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<OpenApiConfig> ai, OpenApiParser.Builder b) {
			OpenApiConfig a = ai.inner();

			string(a.format()).map(HttpPartFormat::valueOf).ifPresent(x -> b.format(x));
			string(a.collectionFormat()).map(HttpPartCollectionFormat::valueOf).ifPresent(x -> b.collectionFormat(x));
		}
	}
}