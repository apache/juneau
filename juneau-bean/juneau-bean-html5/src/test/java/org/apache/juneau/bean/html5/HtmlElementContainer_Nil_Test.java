/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that empty HTML container/mixed elements do not serialize a stray <c>nil='true'</c> attribute
 * when using serializers that add JSON type tags (the default).
 */
class HtmlElementContainer_Nil_Test extends TestBase {

	@Test void a01_emptyContainer_html_noNil() {
		assertEquals("<table></table>", HtmlSerializer.DEFAULT_SQ.toString(table()));
	}

	@Test void a02_emptyContainer_xml_noNil() {
		assertEquals("<table></table>", XmlSerializer.DEFAULT_SQ.toString(table()));
	}

	@Test void a03_nestedEmptyContainers_html_noNil() {
		assertEquals("<table><thead></thead></table>", HtmlSerializer.DEFAULT_SQ.toString(table().child(thead())));
	}

	@Test void a04_emptyContainers_variety_html_noNil() {
		assertEquals("<tbody></tbody>", HtmlSerializer.DEFAULT_SQ.toString(tbody()));
		assertEquals("<tr></tr>", HtmlSerializer.DEFAULT_SQ.toString(tr()));
		assertEquals("<ul></ul>", HtmlSerializer.DEFAULT_SQ.toString(ul()));
	}
}
