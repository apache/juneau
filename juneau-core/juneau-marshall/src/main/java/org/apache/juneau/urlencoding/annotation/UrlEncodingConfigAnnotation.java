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
package org.apache.juneau.urlencoding.annotation;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.*;

/**
 * Utility classes and methods for the {@link UrlEncodingConfig @UrlEncodingConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.UrlEncodingDetails">URL-Encoding Details</a>
 * </ul>
 */
public class UrlEncodingConfigAnnotation {

	/**
	 * Applies {@link UrlEncodingConfig} annotations to a {@link org.apache.juneau.urlencoding.UrlEncodingSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<UrlEncodingConfig,UrlEncodingSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(UrlEncodingConfig.class, UrlEncodingSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<UrlEncodingConfig> ai, UrlEncodingSerializer.Builder b) {
			UrlEncodingConfig a = ai.inner();

			bool(a.expandedParams()).ifPresent(x -> b.expandedParams(x));
		}
	}

	/**
	 * Applies {@link UrlEncodingConfig} annotations to a {@link org.apache.juneau.urlencoding.UrlEncodingParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<UrlEncodingConfig,UrlEncodingParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(UrlEncodingConfig.class, UrlEncodingParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<UrlEncodingConfig> ai, UrlEncodingParser.Builder b) {
			UrlEncodingConfig a = ai.inner();

			bool(a.expandedParams()).ifPresent(x -> b.expandedParams(x));
		}
	}
}