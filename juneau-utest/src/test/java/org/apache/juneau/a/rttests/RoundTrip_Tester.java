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
package org.apache.juneau.a.rttests;

import static java.util.Collections.*;
import static java.util.Optional.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

public class RoundTrip_Tester {

	public static Builder create(int index, String label) {
		return new Builder().index(index).label(label);
	}

	static class Builder {

		private int index;
		public Builder index(int value) { index = value; return this; }

		private String label;
		public Builder label(String value) { label = value; return this; }

		private Serializer.Builder s;
		public Builder serializer(Serializer.Builder value) { s = value; return this; }

		private Parser.Builder p;
		public Builder parser(Parser.Builder value) { p = value; return this; }

		private boolean validateXmlWhitespace;
		public Builder validateXmlWhitespace() { validateXmlWhitespace = true; return this; }

		private boolean returnOriginalObject;
		public Builder returnOriginalObject() { returnOriginalObject = true; return this; }

		private boolean validateXml;
		public Builder validateXml() { validateXml = true; return this; }

		private boolean debug;
		public Builder debug() { debug = true; return this; }

		private Map<Class<?>,Class<?>> implClasses = emptyMap();
		public Builder implClasses(Map<Class<?>,Class<?>> value) { implClasses = value; return this; }

		private Class<?>[] pojoSwaps = a();
		public Builder pojoSwaps(Class<?>...value) { pojoSwaps = value; return this; }

		private Class<?>[] dictionary = a();
		public Builder dictionary(Class<?>...value) { dictionary = value; return this; }

		private Class<?>[] annotatedClasses = a();
		public Builder annotatedClasses(Class<?>...value) { annotatedClasses = value; return this; }

		public RoundTrip_Tester build() {
			return new RoundTrip_Tester(this);
		}
	}

	protected String label;
	protected Serializer s;
	protected Parser p;
	private boolean validateXmlWhitespace;
	protected boolean returnOriginalObject;
	private boolean validateXml;
	public boolean debug;

	private RoundTrip_Tester(Builder b) {
		label = "[" + b.index + "] " + b.label;

		var bs = b.s;
		var bp = ofNullable(b.p);

		if (! (b.implClasses.isEmpty() && b.pojoSwaps.length == 0 && b.dictionary.length == 0 && b.annotatedClasses.length == 0)) {
			bs = bs.copy();
			bp = bp.map(Parser.Builder::copy);
			for (var e : b.implClasses.entrySet()) {
				bs.implClass(e.getKey(), e.getValue());
				bp.ifPresent(x -> x.implClass(e.getKey(), e.getValue()));
			}
			bs.swaps(b.pojoSwaps).beanDictionary(b.dictionary).applyAnnotations(b.annotatedClasses);
			bp.ifPresent(x -> x.swaps(b.pojoSwaps).beanDictionary(b.dictionary).applyAnnotations(b.annotatedClasses));
		}

		s = bs.build();
		p = bp.map(Parser.Builder::build).orElse(null);
		validateXmlWhitespace = b.validateXmlWhitespace;
		validateXml = b.validateXml;
		returnOriginalObject = b.returnOriginalObject;
		debug = b.debug;
	}

	public <T> T roundTrip(T object, Type c, Type...args) throws Exception {
		var out = serialize(object, s);
		if (p == null)
			return object;
		var o = (T)p.parse(out, c, args);
		return (returnOriginalObject ? object : o);
	}

	public <T> T roundTrip(T object) throws Exception {
		return roundTrip(object, s, p);
	}

	public <T> T roundTrip(T object, Serializer serializer, Parser parser) throws Exception {
		var out = serialize(object, serializer);
		if (parser == null)
			return object;
		var o = (T)parser.parse(out,  object == null ? Object.class : object.getClass());
		return (returnOriginalObject ? object : o);
	}

	public Serializer getSerializer() {
		return s;
	}

	public Parser getParser() {
		return p;
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
			checkXmlWhitespace(out.toString());

		if (validateXml)
			validateXml(object, (XmlSerializer)s);

		return out;
	}
}