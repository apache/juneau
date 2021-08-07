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
package org.apache.juneau.html.annotation;

import static org.apache.juneau.html.HtmlSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link HtmlConfig @HtmlConfig} annotation.
 */
public class HtmlConfigAnnotation {

	/**
	 * Applies {@link HtmlConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<HtmlConfig> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(HtmlConfig.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlConfig> ai, ContextPropertiesBuilder b) {
			HtmlConfig a = ai.getAnnotation();

			b.setIfNotEmpty(HTML_addBeanTypes, bool(a.addBeanTypes()));
			b.setIfNotEmpty(HTML_addKeyValueTableHeaders, bool(a.addKeyValueTableHeaders()));
			b.setIfNotEmpty(HTML_disableDetectLabelParameters, bool(a.disableDetectLabelParameters()));
			b.setIfNotEmpty(HTML_disableDetectLinksInStrings, bool(a.disableDetectLinksInStrings()));
			b.setIfNotEmpty(HTML_labelParameter, string(a.labelParameter()));
			b.setIfNotEmpty(HTML_uriAnchorText, string(a.uriAnchorText()));
		}
	}
}