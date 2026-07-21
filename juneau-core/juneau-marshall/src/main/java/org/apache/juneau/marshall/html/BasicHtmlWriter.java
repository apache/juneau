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

import java.io.*;

import org.apache.juneau.marshall.*;

/**
 * Concrete {@link HtmlWriter} leaf for standard HTML output.
 *
 * <p>
 * This is the directly-instantiable form of {@link HtmlWriter}, which is itself an abstract CRTP self-type base so it
 * can also be extended (e.g. by {@link SimpleHtmlWriter} or by the body-var-resolving anonymous writer in
 * {@link BasicHtmlDocTemplate}).
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 */
@SuppressWarnings({
	"resource" // Writer resource managed by calling code
})
public class BasicHtmlWriter extends HtmlWriter<BasicHtmlWriter> {

	/**
	 * Copy constructor.
	 *
	 * @param w Writer being copied.
	 */
	public BasicHtmlWriter(HtmlWriter<?> w) {
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
	public BasicHtmlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver);
	}
}
