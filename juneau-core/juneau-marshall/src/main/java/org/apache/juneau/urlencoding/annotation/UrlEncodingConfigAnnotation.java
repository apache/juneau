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

import static org.apache.juneau.BeanContext.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.*;

/**
 * Utility classes and methods for the {@link UrlEncodingConfig @UrlEncodingConfig} annotation.
 */
public class UrlEncodingConfigAnnotation {

	/**
	 * Applies {@link UrlEncodingConfig} annotations to a {@link PropertyStoreBuilder}.
	 */
	public static class Apply extends ConfigApply<UrlEncodingConfig> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<UrlEncodingConfig> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<UrlEncodingConfig> ai, PropertyStoreBuilder psb, VarResolverSession vr) {
			UrlEncodingConfig a = ai.getAnnotation();

			if (! a.expandedParams().isEmpty()) {
				psb.set(UrlEncodingSerializer.URLENC_expandedParams, bool(a.expandedParams()));
				psb.set(UrlEncodingParser.URLENC_expandedParams, bool(a.expandedParams()));
			}

			if (a.applyUrlEncoding().length > 0)
				psb.prependTo(BEAN_annotations, a.applyUrlEncoding());
		}
	}
}