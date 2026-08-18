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
package org.apache.juneau.marshall.xml;

/**
 * Marker interface for objects whose {@link #toString()} value should be emitted <b>verbatim</b> as raw element
 * content by the XML and HTML serializers.
 *
 * <p>
 * This is the re-serializable analogue of the raw {@link java.io.Reader}/{@link java.io.InputStream} path: the value is
 * appended as-is (no XML entity escaping, no <c>_x####_</c> whitespace/invalid-char encoding, no wrapping element), but
 * because it is backed by a reusable value (typically a {@code String}) it survives repeated serialization &mdash;
 * unlike a one-shot {@code Reader} which is consumed on first use.
 *
 * <p>
 * Implementations are responsible for producing content that is safe for the target syntax.  Notably, because Juneau
 * parses via a StAX {@link javax.xml.stream.XMLStreamReader}, content containing a raw <js>'&lt;'</js> or a bare
 * <js>'&amp;'</js> will serialize correctly (browser-safe) but will NOT round-trip back through Juneau's parser.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jf'>{@link XmlFormat#RAWTEXT}
 * </ul>
 */
public interface RawContent {

	/**
	 * Returns the raw, verbatim character content to emit.
	 *
	 * @return The raw content.  May be <jk>null</jk> (nothing is emitted).
	 */
	@Override
	String toString();
}
