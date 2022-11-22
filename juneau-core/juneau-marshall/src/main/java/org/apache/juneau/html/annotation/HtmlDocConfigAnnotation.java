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
 * Utility classes and methods for the {@link HtmlDocConfig @HtmlDocConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlDocConfigAnnotation {

	/**
	 * Applies {@link HtmlDocConfig} annotations to a {@link org.apache.juneau.html.HtmlDocSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<HtmlDocConfig,HtmlDocSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(HtmlDocConfig.class, HtmlDocSerializer.Builder.class, vr);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void apply(AnnotationInfo<HtmlDocConfig> ai, HtmlDocSerializer.Builder b) {
			HtmlDocConfig a = ai.inner();

			strings(a.aside()).ifPresent(x -> b.aside(x));
			strings(a.footer()).ifPresent(x -> b.footer(x));
			strings(a.head()).ifPresent(x -> b.head(x));
			strings(a.header()).ifPresent(x -> b.header(x));
			strings(a.nav()).ifPresent(x -> b.nav(x));
			strings(a.navlinks()).ifPresent(x -> b.navlinks(x));
			strings(a.script()).ifPresent(x -> b.script(x));
			strings(a.style()).ifPresent(x -> b.style(x));
			strings(a.stylesheet()).ifPresent(x -> b.stylesheet(x));
			string(a.asideFloat()).filter(x -> ! "DEFAULT".equalsIgnoreCase(x)).map(AsideFloat::valueOf).ifPresent(x -> b.asideFloat(x));
			string(a.noResultsMessage()).ifPresent(x -> b.noResultsMessage(x));
			bool(a.nowrap()).ifPresent(x -> b.nowrap(x));
			type(a.template()).ifPresent(x -> b.template(x));
			classes(a.widgets()).ifPresent(x -> b.widgets((Class<? extends HtmlWidget>[]) x));
		}
	}
}