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
package org.apache.juneau.utest.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Various utility methods.
 */
public class Utils2 extends Utils {  // NOSONAR - Class name intentional.

	/** Constructor. */
	protected Utils2() {}

	private static final ThreadLocal<TimeZone> SYSTEM_TIME_ZONE = new ThreadLocal<>();
	private static final ThreadLocal<Locale> SYSTEM_LOCALE = new ThreadLocal<>();

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 * Use {@link #unsetTimeZone()} to unset it.
	 *
	 * @param name
	 */
	public static final synchronized void setTimeZone(String name) {
		SYSTEM_TIME_ZONE.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(name));
	}

	public static final synchronized void unsetTimeZone() {
		TimeZone.setDefault(SYSTEM_TIME_ZONE.get());
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 * Use {@link #unsetLocale()} to unset it.
	 *
	 * @param name
	 */
	public static final void setLocale(Locale locale) {
		SYSTEM_LOCALE.set(Locale.getDefault());
		Locale.setDefault(locale);
	}

	public static final void unsetLocale() {
		Locale.setDefault(SYSTEM_LOCALE.get());
	}

	/**
	 * Validates that the whitespace is correct in the specified XML.
	 */
	public static final void checkXmlWhitespace(String out) throws SerializeException {
		if (out.indexOf('\u0000') != -1) {
			for (var s : out.split("\u0000"))
				checkXmlWhitespace(s);
			return;
		}

		var indent = -1;
		var startTag = Pattern.compile("^(\\s*)<[^/>]+(\\s+\\S+=['\"]\\S*['\"])*\\s*>$");  // NOSONAR
		var endTag = Pattern.compile("^(\\s*)</[^>]+>$");
		var combinedTag = Pattern.compile("^(\\s*)<[^>/]+(\\s+\\S+=['\"]\\S*['\"])*\\s*/>$");  // NOSONAR
		var contentOnly = Pattern.compile("^(\\s*)[^\\s\\<]+$");
		var tagWithContent = Pattern.compile("^(\\s*)<[^>]+>.*</[^>]+>$");
		var lines = out.split("\n");
		try {
			for (var i = 0; i < lines.length; i++) {
				var line = lines[i];
				var m = startTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on start tag line ''{0}''", i+1);
					continue;
				}
				m = endTag.matcher(line);
				if (m.matches()) {
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on end tag line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = combinedTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on combined tag line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = contentOnly.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on content-only line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = tagWithContent.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on tag-with-content line ''{0}''", i+1);
					indent--;
					continue;
				}
				throw new SerializeException("Unmatched whitespace line at line number ''{0}''", i+1);
			}
			if (indent != -1)
				throw new SerializeException("Possible unmatched tag.  indent=''{0}''", indent);
		} catch (SerializeException e) {
			printLines(lines);
			throw e;
		}
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static final void validateXml(Object o) throws Exception {
		validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.serialize(o);
		checkXmlWhitespace(xml);
	}

	/**
	 * Creates an input stream from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new input stream.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	/**
	 * Creates a reader from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new reader.
	 */
	public static final StringReader reader(String in) {
		return new StringReader(in);
	}

	/**
	 * Constructs a {@link URL} object from a string.
	 */
	public static URL url(String value) {
		return safe(()->new URI(value).toURL());
	}

	/**
	 * Asserts an exception is not thrown
	 * Example:  assertThrown(()->doSomething());
	 */
	public static void assertNotThrown(Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			fail("Exception thrown.");
		}
	}
}
