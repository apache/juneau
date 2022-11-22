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
package org.apache.juneau.swaps;

import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Transforms the contents of a {@link Reader} into an {@code Object}.
 *
 * <h5 class='topic'>Description</h5>
 *
 * The {@code Reader} must contain JSON, Juneau-generated XML (output from {@link XmlSerializer}), or Juneau-generated
 * HTML (output from {@link JsonSerializer}) in order to be parsed correctly.
 *
 * <p>
 * Useful for serializing models that contain {@code Readers} created by {@code RestCall} instances.
 *
 * <p>
 * This is a one-way transform, since {@code Readers} cannot be reconstituted.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 *
 * The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Json} - Parses JSON text.
 * 	<li>{@link Xml} - Parses XML text.
 * 	<li>{@link Html} - Parses HTML text.
 * 	<li>{@link PlainText} - Parses plain text.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class ParsedReaderSwap extends ObjectSwap<Reader,Object> {

	/** Reader transform for reading JSON text. */
	public static class Json extends ParsedReaderSwap {
		/** Constructor */
		public Json() {
			super(JsonParser.DEFAULT);
		}
	}

	/** Reader transform for reading XML text. */
	public static class Xml extends ParsedReaderSwap {
		/** Constructor */
		public Xml() {
			super(XmlParser.DEFAULT);
		}
	}

	/** Reader transform for reading HTML text. */
	public static class Html extends ParsedReaderSwap {
		/** Constructor */
		public Html() {
			super(HtmlParser.DEFAULT);
		}
	}

	/** Reader transform for reading plain text. */
	public static class PlainText extends ParsedReaderSwap {
		/** Constructor */
		public PlainText() {
			super(null);
		}
	}

	/** Reader transform for reading plain text. */
	public static class Uon extends ParsedReaderSwap {
		/** Constructor */
		public Uon() {
			super(UonParser.DEFAULT);
		}
	}

	/** Reader transform for reading plain text. */
	public static class UrlEncoding extends ParsedReaderSwap {
		/** Constructor */
		public UrlEncoding() {
			super(UrlEncodingParser.DEFAULT);
		}
	}

	/** The parser to use to parse the contents of the Reader. */
	private ReaderParser parser;

	/**
	 * @param parser The parser to use to convert the contents of the reader to Java objects.
	 */
	public ParsedReaderSwap(ReaderParser parser) {
		this.parser = parser;
	}

	/**
	 * Converts the specified {@link Reader} to an {@link Object} whose type is determined by the contents of the reader.
	 */
	@Override /* ObjectSwap */
	public Object swap(BeanSession session, Reader o) throws Exception {
		if (parser == null)
			return read(o);
		return parser.parse(o, Object.class);
	}
}
