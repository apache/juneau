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
package org.apache.juneau.dto.html5;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import org.apache.juneau.xml.annotation.*;

/**
 * An object that gets serialized as raw XML by the XML and HTML serializers.
 *
 * <p>
 * Can be used to serialize text containing markup without escaping the markup.
 * For example:
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	HtmlText <jv>htmlText</jv> = <jk>new</jk> HtmlText(<js>"&lt;span&gt;&amp;#2753;&lt;/span&gt;"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Xml(format=XMLTEXT)
public class HtmlText {
	private final String text;

	/**
	 * Constructor.
	 *
	 * @param text Raw text.
	 */
	public HtmlText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}
