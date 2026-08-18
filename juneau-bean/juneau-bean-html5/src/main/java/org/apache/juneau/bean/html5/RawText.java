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

import org.apache.juneau.marshall.xml.*;

/**
 * A re-serializable, {@link String}-backed raw-content wrapper that the XML and HTML serializers emit <b>verbatim</b>.
 *
 * <p>
 * This is the reusable analogue of handing the serializer a one-shot {@link java.io.Reader}: the value is emitted
 * without any XML entity escaping or <c>_x####_</c> whitespace encoding, and without a wrapping element &mdash; but
 * because it is backed by a {@code String} it can be serialized any number of times.  This makes a bean carrying a
 * {@code RawText} value survive a serialize&rarr;object&rarr;serialize cycle (for the serialize direction), unlike a
 * consumed {@code Reader}.
 *
 * <p>
 * Typical use is embedding pre-built script/style bodies into a {@link Script}/{@link Style} element:
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Browser-safe raw JSON sidecar ('&lt;' pre-escaped to \\u003c by the caller).</jc>
 * 	Script <jv>s</jv> = <jsm>script</jsm>(<js>"application/json"</js>).text(<jsm>rawText</jsm>(<jv>escapedJson</jv>));
 * </p>
 *
 * <p>
 * <b>Safety:</b> content is emitted verbatim.  Because Juneau parses via a StAX
 * {@link javax.xml.stream.XMLStreamReader}, a value containing a raw <js>'&lt;'</js> (e.g. a literal
 * <c>&lt;/script&gt;</c> sequence) or a bare <js>'&amp;'</js> is browser-correct on serialize but will NOT round-trip
 * back through Juneau's parser.  Callers embedding markup-bearing content are responsible for escaping <js>'&lt;'</js>
 * to the JS/CSS unicode escape <js>"\\u003c"</js> before wrapping.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link HtmlBuilder#rawText(String)}
 * 	<li class='jic'>{@link RawContent}
 * </ul>
 */
public class RawText implements RawContent {

	private final String text;

	/**
	 * Constructor.
	 *
	 * @param text The raw, verbatim text content.  Can be <jk>null</jk> (nothing is emitted).
	 */
	public RawText(String text) {
		this.text = text;
	}

	@Override /* Overridden from RawContent */
	public String toString() {
		return text;
	}
}
