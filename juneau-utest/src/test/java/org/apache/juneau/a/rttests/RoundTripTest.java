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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.a.rttests.RoundTripTest.Flags.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.XmlUtils;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"unchecked"})
public abstract class RoundTripTest {

	public static class Flags {
		public static int CHECK_XML_WHITESPACE = 1, VALIDATE_XML = 2, SERIALIZE_SCHEMA = 4, RETURN_ORIGINAL_OBJECT = 8;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {
			// Full round-trip testing
			{ /* 0 */
				"Json - default",
				JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType(),
				JsonParser.create(),
				0
			},
			{ /* 1 */
				"Json - lax",
				JsonSerializer.create().ssq().keepNullProperties().addBeanTypes().addRootType(),
				JsonParser.create(),
				0
			},
			{ /* 2 */
				"Json - lax, readable",
				JsonSerializer.create().ssq().ws().keepNullProperties().addBeanTypes().addRootType(),
				JsonParser.create(),
				0
			},
			{ /* 3 */
				"Xml - namespaces, validation, readable",
				XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType(),
				XmlParser.create(),
				CHECK_XML_WHITESPACE | VALIDATE_XML
			},
			{ /* 4 */
				"Xml - no namespaces, validation",
				XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType(),
				XmlParser.create(),
				CHECK_XML_WHITESPACE
			},
			{ /* 5 */
				"Html - default",
				HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType(),
				HtmlParser.create(),
				CHECK_XML_WHITESPACE
			},
			{ /* 6 */
				"Html - readable",
				HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType(),
				HtmlParser.create(),
				CHECK_XML_WHITESPACE
			},
			{ /* 7 */
				"Html - with key/value headers",
				HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType(),
				HtmlParser.create(),
				CHECK_XML_WHITESPACE
			},
			{ /* 8 */
				"Uon - default",
				UonSerializer.create().keepNullProperties().addBeanTypes().addRootType(),
				UonParser.create(),
				0
			},
			{ /* 9 */
				"Uon - readable",
				UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType(),
				UonParser.create(),
				0
			},
			{ /* 10 */
				"Uon - encoded",
				UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType(),
				UonParser.create().decoding(),
				0
			},
			{ /* 11 */
				"UrlEncoding - default",
				UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType(),
				UrlEncodingParser.create(),
				0
			},
			{ /* 12 */
				"UrlEncoding - readable",
				UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType(),
				UrlEncodingParser.create(),
				0
			},
			{ /* 13 */
				"UrlEncoding - expanded params",
				UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType(),
				UrlEncodingParser.create().expandedParams(),
				0
			},
			{ /* 14 */
				"Rdf.Xml",
				RdfXmlSerializer.create().keepNullProperties().addLiteralTypes().addBeanTypes().addRootType(),
				RdfXmlParser.create(),
				0
			},
			{ /* 15 */
				"Rdf.XmlAbbrev",
				RdfXmlAbbrevSerializer.create().keepNullProperties().addLiteralTypes().addBeanTypes().addRootType(),
				RdfXmlParser.create(),
				0
			},
			{ /* 16 */
				"Rdf.Turtle",
				TurtleSerializer.create().keepNullProperties().addLiteralTypes().addBeanTypes().addRootType(),
				TurtleParser.create(),
				0
			},
			{ /* 17 */
				"Rdf.NTriple",
				NTripleSerializer.create().keepNullProperties().addLiteralTypes().addBeanTypes().addRootType(),
				NTripleParser.create(),
				0
			},
			{ /* 18 */
				"Rdf.N3",
				N3Serializer.create().keepNullProperties().addLiteralTypes().addBeanTypes().addRootType(),
				N3Parser.create(),
				0
			},
			{ /* 19 */
				"MsgPack",
				MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType(),
				MsgPackParser.create(),
				0
			},

			// Validation testing only
			{ /* 20 */
				"Json schema",
				JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType(),
				null,
				RETURN_ORIGINAL_OBJECT
			},
		});
	}

	protected Serializer s;
	protected Parser p;
	private boolean validateXmlWhitespace;
	protected boolean returnOriginalObject;
	private boolean validateXml;
	protected String label;
	public boolean debug = false;

	public RoundTripTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		this.label = label;

		Map<Class<Object>, Class<? extends Object>> m = getImplClasses();
		if (m != null) {
			for (Entry<Class<Object>, Class<? extends Object>> e : m.entrySet()) {
				s.implClass(e.getKey(), e.getValue());
				if (p != null)
					p.implClass(e.getKey(), e.getValue());
			}
		}
		this.s = s.swaps(getPojoSwaps()).dictionary((Object[])getDictionary()).add(getProperties()).build();
		this.p = p == null ? null : p.swaps(getPojoSwaps()).dictionary((Object[])getDictionary()).add(getProperties()).build();
		this.validateXmlWhitespace = (flags & CHECK_XML_WHITESPACE) > 0;
		this.validateXml = (flags & VALIDATE_XML) > 0;
		this.returnOriginalObject = (flags & RETURN_ORIGINAL_OBJECT) > 0;
	}


	public Object[] getBeanFilters() {
		return new Object[0];
	}

	public Object[] getPojoSwaps() {
		return new Class<?>[0];
	}

	public Class<?>[] getDictionary() {
		return new Class<?>[0];
	}

	public OMap getProperties() {
		return OMap.EMPTY_MAP;
	}

	public <T> Map<Class<T>,Class<? extends T>> getImplClasses() {
		return null;
	}

	public <T> T roundTrip(T object, Type c, Type...args) throws Exception {
		Object out = serialize(object, this.s);
		if (p == null)
			return object;
		T o = (T)this.p.parse(out, c, args);
		return (returnOriginalObject ? object : o);
	}

	public <T> T roundTrip(T object) throws Exception {
		return roundTrip(object, s, p);
	}

	public <T> T roundTrip(T object, Serializer serializer, Parser parser) throws Exception {
		Object out = serialize(object, serializer);
		if (parser == null)
			return object;
		T o = (T)parser.parse(out,  object == null ? Object.class : object.getClass());
		return (returnOriginalObject ? object : o);
	}

	public Serializer getSerializer() {
		return s;
	}

	public Parser getParser() {
		return p;
	}

	protected void swaps(Object...c) {
		s = s.builder().swaps(c).build();
		if (p != null)
			p = p.builder().swaps(c).build();
	}

	protected void dictionary(Object...c) {
		s = s.builder().dictionary(c).build();
		if (p != null)
			p = p.builder().dictionary(c).build();
	}

	protected void applyAnnotations(Class<?>...fromClasses) {
		s = s.builder().applyAnnotations(fromClasses).build();
		if (p != null)
			p = p.builder().applyAnnotations(fromClasses).build();
	}

	public boolean isValidationOnly() {
		return returnOriginalObject;
	}

	public <T> Object serialize(T object, Serializer s) throws Exception {

		Object out = null;
		if (s.isWriterSerializer())
			out = ((WriterSerializer)s).serialize(object);
		else {
			out = ((OutputStreamSerializer)s).serialize(object);
		}

		if (debug)
			System.err.println("Serialized contents from ["+label+"]...\n---START---\n" + (out instanceof byte[] ? StringUtils.toReadableBytes((byte[])out) : out) + "\n---END---\n"); // NOT DEBUG

		if (validateXmlWhitespace)
			XmlUtils.checkXmlWhitespace(out.toString());

		if (validateXml)
			XmlUtils.validateXml(object, (XmlSerializer)s);

		return out;
	}
}
