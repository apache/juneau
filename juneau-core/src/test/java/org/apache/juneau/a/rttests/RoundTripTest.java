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
import static org.apache.juneau.jena.RdfSerializerContext.*;
import static org.apache.juneau.urlencoding.UonSerializerContext.*;
import static org.apache.juneau.urlencoding.UrlEncodingContext.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;

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
@SuppressWarnings({"unchecked","hiding","javadoc"})
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
				new JsonSerializer().setProperty(SERIALIZER_trimNullProperties, false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 1 */
				"Json - lax",
				new JsonSerializer.Simple().setProperty(SERIALIZER_trimNullProperties, false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 2 */
				"Json - lax, readable",
				new JsonSerializer.SimpleReadable().setProperty(SERIALIZER_trimNullProperties, false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 3 */
				"Xml - namespaces, validation, readable",
				new XmlSerializer.XmlJsonSq().setProperty(SERIALIZER_trimNullProperties, false).setProperty(XML_addNamespaceUrisToRoot, true).setProperty(SERIALIZER_useIndentation, true),
				XmlParser.DEFAULT,
				CHECK_XML_WHITESPACE | VALIDATE_XML
			},
			{ /* 4 */
				"Xml - no namespaces, validation",
				new XmlSerializer.SimpleXmlJsonSq().setProperty(SERIALIZER_trimNullProperties, false),
				XmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 5 */
				"Html - default",
				new HtmlSerializer().setProperty(SERIALIZER_trimNullProperties, false),
				HtmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 6 */
				"Html - readable",
				new HtmlSerializer.SqReadable().setProperty(SERIALIZER_trimNullProperties, false),
				HtmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 7 */
				"Uon - default",
				new UonSerializer().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UonParser.DEFAULT,
				0
			},
			{ /* 8 */
				"Uon - readable",
				new UonSerializer.Readable().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UonParser.DEFAULT_WS_AWARE,
				0
			},
			{ /* 9 */
				"Uon - encoded",
				new UonSerializer.Encoding().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UonParser.DEFAULT_DECODING,
				0
			},
			{ /* 10 */
				"UrlEncoding - default",
				new UrlEncodingSerializer().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UrlEncodingParser.DEFAULT,
				0
			},
			{ /* 11 */
				"UrlEncoding - readable",
				new UrlEncodingSerializer.Readable().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UrlEncodingParser.DEFAULT_WS_AWARE,
				0
			},
			{ /* 12 */
				"UrlEncoding - expanded params",
				new UrlEncodingSerializer().setProperty(URLENC_expandedParams, true).setProperty(UON_simpleMode, false),
				new UrlEncodingParser().setProperty(URLENC_expandedParams, true),
				0
			},
			{ /* 13 */
				"Rdf.Xml",
				new RdfSerializer.Xml().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
				RdfParser.DEFAULT_XML,
				0
			},
			{ /* 14 */
				"Rdf.XmlAbbrev",
				new RdfSerializer.XmlAbbrev().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
				RdfParser.DEFAULT_XML,
				0
			},
			{ /* 15 */
				"Rdf.Turtle",
				new RdfSerializer.Turtle().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
				RdfParser.DEFAULT_TURTLE,
				0
			},
			{ /* 16 */
				"Rdf.NTriple",
				new RdfSerializer.NTriple().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
				RdfParser.DEFAULT_NTRIPLE,
				0
			},
			{ /* 17 */
				"Rdf.N3",
				new RdfSerializer.N3().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
				RdfParser.DEFAULT_N3,
				0
			},
			{ /* 18 */
				"MsgPack",
				new MsgPackSerializer().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
				MsgPackParser.DEFAULT,
				0
			},

			// Validation testing only
			{ /* 19 */
				"Json schema",
				new JsonSchemaSerializer().setProperty(SERIALIZER_trimNullProperties, false),
				null,
				RETURN_ORIGINAL_OBJECT
			},
			{ /* 20 */
				"Xml schema",
				new XmlSchemaSerializer().setProperty(SERIALIZER_trimNullProperties, false),
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
		this.s = s.clone().addBeanFilters(getBeanFilters()).addPojoSwaps(getPojoSwaps());
		this.p = p == null ? null : p.clone().addBeanFilters(getBeanFilters()).addPojoSwaps(getPojoSwaps());
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

	public <T> Map<Class<T>,Class<? extends T>> getImplClasses() {
		return null;
	}

	public <T> T roundTrip(T object, ClassMeta<? extends T> t) throws Exception {
		Object out = serialize(object, this.s);
		if (p == null)
			return object;
		T o = this.p.parse(out, t);
		return (returnOriginalObject ? object : o);
	}
	public <T> T roundTrip(T object, Class<? extends T> c) throws Exception {
		Object out = serialize(object, this.s);
		if (p == null)
			return object;
		T o = this.p.parse(out, p.getBeanContext().getClassMeta(c));
		return (returnOriginalObject ? object : o);
	}
	public <K,V,T extends Map<K,V>> T roundTripMap(T object, Class<? extends T> c, Class<K> k, Class<V> v) throws Exception {
		Object out = serialize(object, this.s);
		if (p == null)
			return object;
		ClassMeta<? extends T> cm = p.getBeanContext().getMapClassMeta(c, k, v);
		T o = this.p.parse(out, cm);
		return (returnOriginalObject ? object : o);
	}
	public <E,T extends Collection<E>> T roundTripCollection(T object, Class<? extends T> c, Class<E> e) throws Exception {
		Object out = serialize(object, this.s);
		if (p == null)
			return object;
		ClassMeta<? extends T> cm = p.getBeanContext().getCollectionClassMeta(c, e);
		T o = this.p.parse(out, cm);
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
			System.err.println("Serialized contents from ["+label+"]...\n---START---\n" + (out instanceof byte[] ? TestUtils.toReadableBytes((byte[])out) : out) + "\n---END---\n");

		if (validateXmlWhitespace)
			TestUtils.checkXmlWhitespace(out.toString());

		if (validateXml)
			TestUtils.validateXml(object, (XmlSerializer)s);

		return out;
	}
}
