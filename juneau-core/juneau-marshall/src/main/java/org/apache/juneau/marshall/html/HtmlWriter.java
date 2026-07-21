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
package org.apache.juneau.marshall.html;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Specialized writer for serializing HTML.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlSupport">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"resource", // Writer resource managed by calling code
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class HtmlWriter<SELF extends HtmlWriter<SELF>> extends XmlWriter<SELF> {

	/**
	 * Copy constructor.
	 *
	 * @param w Writer being copied.
	 */
	public HtmlWriter(HtmlWriter<?> w) {
		super(w);
	}

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	public HtmlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver, false, null);
	}

	@Override /* Overridden from SerializerWriter */
	public SELF cr(int depth) {
		if (depth > 0)
			super.cr(depth);
		return self();
	}

	@Override /* Overridden from SerializerWriter */
	public SELF cre(int depth) {
		if (depth > 0)
			super.cre(depth);
		return self();
	}

	@Override /* Overridden from XmlSerializerWriter */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for text serialization with whitespace handling
	})
	public SELF text(Object o, boolean preserveWhitespace) {

		if (o == null) {
			append("<null/>");
			return self();
		}
		var s = o.toString();
		if (s.isEmpty()) {
			append("<sp/>");
			return self();
		}

		for (var i = 0; i < s.length(); i++) {
			var test = s.charAt(i);
			if (test == '&')
				append("&amp;");
			else if (test == '<')
				append("&lt;");
			else if (test == '>')
				append("&gt;");
			else if (test == '\n')
				append(preserveWhitespace ? "\n" : "<br/>");
			else if (test == '\f')  // XML 1.0 doesn't support form feeds or backslashes, so we have to invent something.
				append(preserveWhitespace ? "\f" : "<ff/>");
			else if (test == '\b')
				append(preserveWhitespace ? "\b" : "<bs/>");
			else if (test == '\t')
				append(preserveWhitespace ? "\t" : "<sp>&#x2003;</sp>");
			else if ((i == 0 || i == s.length() - 1) && Character.isWhitespace(test)) {
				if (preserveWhitespace)
					w(test);
				else if (test == ' ')
					append("<sp> </sp>");
				else
					append("<sp>&#x").append(toHex4(test)).append(";</sp>");
			} else if (Character.isISOControl(test))
				append("&#" + (int)test + ";");
			else
				w(test);
		}

		return self();
	}
}