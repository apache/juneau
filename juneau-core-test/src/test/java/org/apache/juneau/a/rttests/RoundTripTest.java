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

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"unchecked","javadoc"})
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
				new JsonSerializer().setTrimNullProperties(false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 1 */
				"Json - lax",
				new JsonSerializer.Simple().setTrimNullProperties(false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 2 */
				"Json - lax, readable",
				new JsonSerializer.SimpleReadable().setTrimNullProperties(false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 3 */
				"Xml - namespaces, validation, readable",
				new XmlSerializer.NsSq().setTrimNullProperties(false).setAddNamespaceUrisToRoot(true).setUseWhitespace(true),
				XmlParser.DEFAULT,
				CHECK_XML_WHITESPACE | VALIDATE_XML
			},
			{ /* 4 */
				"Xml - no namespaces, validation",
				new XmlSerializer.Sq().setTrimNullProperties(false),
				XmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 5 */
				"Html - default",
				new HtmlSerializer().setTrimNullProperties(false),
				HtmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 6 */
				"Html - readable",
				new HtmlSerializer.SqReadable().setTrimNullProperties(false),
				HtmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 7 */
				"Html - with key/value headers",
				new HtmlSerializer().setAddKeyValueTableHeaders(true),
				HtmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 8 */
				"Uon - default",
				new UonSerializer().setTrimNullProperties(false),
				UonParser.DEFAULT,
				0
			},
			{ /* 9 */
				"Uon - readable",
				new UonSerializer.Readable().setTrimNullProperties(false),
				UonParser.DEFAULT,
				0
			},
			{ /* 10 */
				"Uon - encoded",
				new UonSerializer.Encoding().setTrimNullProperties(false),
				UonParser.DEFAULT_DECODING,
				0
			},
			{ /* 11 */
				"UrlEncoding - default",
				new UrlEncodingSerializer().setTrimNullProperties(false),
				UrlEncodingParser.DEFAULT,
				0
			},
			{ /* 12 */
				"UrlEncoding - readable",
				new UrlEncodingSerializer.Readable().setTrimNullProperties(false),
				UrlEncodingParser.DEFAULT,
				0
			},
			{ /* 13 */
				"UrlEncoding - expanded params",
				new UrlEncodingSerializer().setExpandedParams(true),
				new UrlEncodingParser().setExpandedParams(true),
				0
			},
			{ /* 14 */
				"Rdf.Xml",
				new RdfSerializer.Xml().setTrimNullProperties(false).setAddLiteralTypes(true),
				RdfParser.DEFAULT_XML,
				0
			},
			{ /* 15 */
				"Rdf.XmlAbbrev",
				new RdfSerializer.XmlAbbrev().setTrimNullProperties(false).setAddLiteralTypes(true),
				RdfParser.DEFAULT_XML,
				0
			},
			{ /* 16 */
				"Rdf.Turtle",
				new RdfSerializer.Turtle().setTrimNullProperties(false).setAddLiteralTypes(true),
				RdfParser.DEFAULT_TURTLE,
				0
			},
			{ /* 17 */
				"Rdf.NTriple",
				new RdfSerializer.NTriple().setTrimNullProperties(false).setAddLiteralTypes(true),
				RdfParser.DEFAULT_NTRIPLE,
				0
			},
			{ /* 18 */
				"Rdf.N3",
				new RdfSerializer.N3().setTrimNullProperties(false).setAddLiteralTypes(true),
				RdfParser.DEFAULT_N3,
				0
			},
			{ /* 19 */
				"MsgPack",
				new MsgPackSerializer().setTrimNullProperties(false),
				MsgPackParser.DEFAULT,
				0
			},

			// Validation testing only
			{ /* 20 */
				"Json schema",
				new JsonSchemaSerializer().setTrimNullProperties(false),
				null,
				RETURN_ORIGINAL_OBJECT
			},
			{ /* 21 */
				"Xml schema",
				new XmlSchemaSerializer().setTrimNullProperties(false),
				new XmlValidatorParser(),
				RETURN_ORIGINAL_OBJECT | CHECK_XML_WHITESPACE
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

	public RoundTripTest(String label, Serializer s, Parser p, int flags) throws Exception {
		this.s = s.clone().addBeanFilters(getBeanFilters()).addPojoSwaps(getPojoSwaps()).addToBeanDictionary(getDictionary()).setProperties(getProperties());
		this.p = p == null ? null : p.clone().addBeanFilters(getBeanFilters()).addPojoSwaps(getPojoSwaps()).addToBeanDictionary(getDictionary()).setProperties(getProperties());
		this.label = label;

		Map<Class<Object>, Class<? extends Object>> m = getImplClasses();
		if (m != null) {
			for (Entry<Class<Object>, Class<? extends Object>> e : m.entrySet()) {
				this.s.addImplClass(e.getKey(), e.getValue());
				if (this.p != null)
					this.p.addImplClass(e.getKey(), e.getValue());
			}
		}
		this.validateXmlWhitespace = (flags & CHECK_XML_WHITESPACE) > 0;
		this.validateXml = (flags & VALIDATE_XML) > 0;
		this.returnOriginalObject = (flags & RETURN_ORIGINAL_OBJECT) > 0;
	}


	public Class<?>[] getBeanFilters() {
		return new Class<?>[0];
	}

	public Class<?>[] getPojoSwaps() {
		return new Class<?>[0];
	}

	public Class<?>[] getDictionary() {
		return new Class<?>[0];
	}
	
	public ObjectMap getProperties() {
		return ObjectMap.EMPTY_MAP;
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

	protected void addBeanFilters(Class<?>...c) {
		s.addBeanFilters(c);
		if (p != null)
			p.addBeanFilters(c);
	}

	protected void addPojoSwaps(Class<?>...c) {
		s.addPojoSwaps(c);
		if (p != null)
			p.addPojoSwaps(c);
	}

	protected void addToBeanDictionary(Class<?>...c) {
		s.addToBeanDictionary(c);
		if (p != null)
			p.addToBeanDictionary(c);
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
			System.err.println("Serialized contents from ["+label+"]...\n---START---\n" + (out instanceof byte[] ? TestUtils.toReadableBytes((byte[])out) : out) + "\n---END---\n"); // NOT DEBUG

		if (validateXmlWhitespace)
			TestUtils.checkXmlWhitespace(out.toString());

		if (validateXml)
			TestUtils.validateXml(object, (XmlSerializer)s);

		return out;
	}
}
