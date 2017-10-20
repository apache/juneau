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
package org.apache.juneau.html;

import static org.apache.juneau.html.HtmlSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link HtmlSerializer} class.
 */
public class HtmlSerializerContext extends XmlSerializerContext {

	final AnchorText uriAnchorText;
	final boolean
		lookForLabelParameters,
		detectLinksInStrings,
		addKeyValueTableHeaders,
		addBeanTypeProperties;
	final String labelParameter;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public HtmlSerializerContext(PropertyStore ps) {
		super(ps);
		uriAnchorText = ps.getProperty(HTML_uriAnchorText, AnchorText.class, AnchorText.TO_STRING);
		lookForLabelParameters = ps.getProperty(HTML_lookForLabelParameters, Boolean.class, true);
		detectLinksInStrings = ps.getProperty(HTML_detectLinksInStrings, Boolean.class, true);
		labelParameter = ps.getProperty(HTML_labelParameter, String.class, "label");
		addKeyValueTableHeaders = ps.getProperty(HTML_addKeyValueTableHeaders, Boolean.class, false);
		addBeanTypeProperties = ps.getProperty(HTML_addBeanTypeProperties, boolean.class,
			ps.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("HtmlSerializerContext", new ObjectMap()
				.append("uriAnchorText", uriAnchorText)
				.append("lookForLabelParameters", lookForLabelParameters)
				.append("detectLinksInStrings", detectLinksInStrings)
				.append("labelParameter", labelParameter)
				.append("addKeyValueTableHeaders", addKeyValueTableHeaders)
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}
