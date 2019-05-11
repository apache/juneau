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
import org.apache.juneau.svl.*;

/**
 * Applies {@link HtmlConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class HtmlConfigApply extends ConfigApply<HtmlConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public HtmlConfigApply(Class<HtmlConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@Override
	public void apply(HtmlConfig a, PropertyStoreBuilder psb) {
		if (! a.addBeanTypes().isEmpty())
			psb.set(HTML_addBeanTypes, bool(a.addBeanTypes()));
		if (! a.addKeyValueTableHeaders().isEmpty())
			psb.set(HTML_addKeyValueTableHeaders, bool(a.addKeyValueTableHeaders()));
		if (! a.detectLabelParameters().isEmpty())
			psb.set(HTML_detectLabelParameters, bool(a.detectLabelParameters()));
		if (! a.detectLinksInStrings().isEmpty())
			psb.set(HTML_detectLinksInStrings, bool(a.detectLinksInStrings()));
		if (! a.labelParameter().isEmpty())
			psb.set(HTML_labelParameter, string(a.labelParameter()));
		if (! a.uriAnchorText().isEmpty())
			psb.set(HTML_uriAnchorText, string(a.uriAnchorText()));
	}
}
