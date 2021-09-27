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
 */
public class HtmlConfigAnnotation {

	/**
	 * Applies {@link HtmlConfig} annotations to a {@link HtmlSerializerBuilder}.
	 */
	public static class SerializerApply extends AnnotationApplier<HtmlConfig,HtmlSerializerBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(HtmlConfig.class, HtmlSerializerBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlConfig> ai, HtmlSerializerBuilder b) {
			HtmlConfig a = ai.getAnnotation();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesHtml(x));
			bool(a.addKeyValueTableHeaders()).ifPresent(x -> b.addKeyValueTableHeaders(x));
			bool(a.disableDetectLabelParameters()).ifPresent(x -> b.disableDetectLabelParameters(x));
			bool(a.disableDetectLinksInStrings()).ifPresent(x -> b.disableDetectLinksInStrings(x));
			string(a.labelParameter()).ifPresent(x -> b.labelParameter(x));
			string(a.uriAnchorText()).map(AnchorText::valueOf).ifPresent(x -> b.uriAnchorText(x));
		}
	}

	/**
	 * Applies {@link HtmlConfig} annotations to a {@link HtmlParserBuilder}.
	 */
	public static class ParserApply extends AnnotationApplier<HtmlConfig,HtmlParserBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(HtmlConfig.class, HtmlParserBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlConfig> ai, HtmlParserBuilder b) {
		}
	}
}