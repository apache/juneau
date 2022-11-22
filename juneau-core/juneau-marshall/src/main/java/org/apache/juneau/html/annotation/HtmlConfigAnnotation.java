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

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link HtmlConfig @HtmlConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlConfigAnnotation {

	/**
	 * Applies {@link HtmlConfig} annotations to a {@link org.apache.juneau.html.HtmlSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<HtmlConfig,HtmlSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(HtmlConfig.class, HtmlSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlConfig> ai, HtmlSerializer.Builder b) {
			HtmlConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesHtml(x));
			bool(a.addKeyValueTableHeaders()).ifPresent(x -> b.addKeyValueTableHeaders(x));
			bool(a.disableDetectLabelParameters()).ifPresent(x -> b.disableDetectLabelParameters(x));
			bool(a.disableDetectLinksInStrings()).ifPresent(x -> b.disableDetectLinksInStrings(x));
			string(a.labelParameter()).ifPresent(x -> b.labelParameter(x));
			string(a.uriAnchorText()).map(AnchorText::valueOf).ifPresent(x -> b.uriAnchorText(x));
		}
	}

	/**
	 * Applies {@link HtmlConfig} annotations to a {@link org.apache.juneau.html.HtmlParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<HtmlConfig,HtmlParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(HtmlConfig.class, HtmlParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlConfig> ai, HtmlParser.Builder b) {
		}
	}
}