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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.yaml.*;

/**
 * Represents the input to a ComboTest.
 * @param <T>
 */
public class ComboRoundTrip_Tester<T> {

	public static <T> Builder<T> create(int index, String label, Type type, Supplier<T> in) {
		return new Builder<>(index, label, type, in);
	}

	public static class Builder<T> {
		private int index;
		private String label;
		private Supplier<T> in;
		private String exceptionMsg;
		private Predicate<String> skipTest = x -> false;
		private Function<T,T> postConvert = x -> x;
		private List<Function<T,String>> verify = list();
		private List<Class<?>> swaps = list();
		private Type type;
		private Map<String,String> expected = map();
		private List<Tuple2<Class<?>,Consumer<?>>> applies = list();
		private Consumer<Serializer.Builder> serializerApply = x -> {};
		private Consumer<Parser.Builder> parserApply = x -> {};

		public Builder(int index, String label, Type type, T in) {
			this.index = index;
			this.label = label;
			this.type = type;
			this.in = () -> in;
		}

		public Builder(int index, String label, Type type, Supplier<T> in) {
			this.index = index;
			this.label = label;
			this.type = type;
			this.in = in;
		}

		public Builder<T> beanContext(Consumer<BeanContext.Builder> c) { apply(BeanContext.Builder.class, c); return this; }

		public <T2> Builder<T> apply(Class<T2> t, Consumer<T2> c) { applies.add(Tuple2.of(t, c)); return this; }

		public Builder<T> exceptionMsg(String v) { exceptionMsg = v; return this; }

		public Builder<T> skipTest(Predicate<String> v) { skipTest = v; return this; }

		public Builder<T> postConvert(Function<T,T> v) { postConvert = v; return this; }

		public Builder<T> verify(Function<T,String> v) { verify.add(v); return this; }

		public Builder<T> verify(Predicate<T> p, String msg, Object...args) { verify.add(x -> p.test(x) ? null : f(msg, args)); return this; }

		public Builder<T> swaps(Class<?>...c) { swaps.addAll(l(c)); return this; }

		public Builder<T> serializerApply(Consumer<Serializer.Builder> v) { serializerApply = v; return this; }

		public Builder<T> parserApply(Consumer<Parser.Builder> v) { parserApply = v; return this; }

		public Builder<T> json(String value) { expected.put("json", value); return this; }
		public Builder<T> jsonT(String value) { expected.put("jsonT", value); return this; }
		public Builder<T> jsonR(String value) { expected.put("jsonR", value); return this; }
		public Builder<T> xml(String value) { expected.put("xml", value); return this; }
		public Builder<T> xmlT(String value) { expected.put("xmlT", value); return this; }
		public Builder<T> xmlR(String value) { expected.put("xmlR", value); return this; }
		public Builder<T> xmlNs(String value) { expected.put("xmlNs", value); return this; }
		public Builder<T> html(String value) { expected.put("html", value); return this; }
		public Builder<T> htmlT(String value) { expected.put("htmlT", value); return this; }
		public Builder<T> htmlR(String value) { expected.put("htmlR", value); return this; }
		public Builder<T> uon(String value) { expected.put("uon", value); return this; }
		public Builder<T> uonT(String value) { expected.put("uonT", value); return this; }
		public Builder<T> uonR(String value) { expected.put("uonR", value); return this; }
		public Builder<T> urlEnc(String value) { expected.put("urlEnc", value); return this; }
		public Builder<T> urlEncT(String value) { expected.put("urlEncT", value); return this; }
		public Builder<T> urlEncR(String value) { expected.put("urlEncR", value); return this; }
		public Builder<T> msgPack(String value) { expected.put("msgPack", value); return this; }
		public Builder<T> msgPackT(String value) { expected.put("msgPackT", value); return this; }
		public Builder<T> rdfXml(String value) { expected.put("rdfXml", value); return this; }
		public Builder<T> rdfXmlT(String value) { expected.put("rdfXmlT", value); return this; }
		public Builder<T> rdfXmlR(String value) { expected.put("rdfXmlR", value); return this; }
		public Builder<T> rdfXmlAbbrev(String value) { expected.put("rdfXmlAbbrev", value); return this; }
		public Builder<T> rdfTurtle(String value) { expected.put("rdfTurtle", value); return this; }
		public Builder<T> rdfN3(String value) { expected.put("rdfN3", value); return this; }
		public Builder<T> rdfNtriple(String value) { expected.put("rdfNtriple", value); return this; }
		public Builder<T> rdfNquads(String value) { expected.put("rdfNquads", value); return this; }
		public Builder<T> rdfTrig(String value) { expected.put("rdfTrig", value); return this; }
		public Builder<T> rdfJsonLd(String value) { expected.put("rdfJsonLd", value); return this; }
		public Builder<T> rdfJson(String value) { expected.put("rdfJson", value); return this; }
		public Builder<T> rdfTriX(String value) { expected.put("rdfTriX", value); return this; }
		public Builder<T> rdfThrift(String value) { expected.put("rdfThrift", value); return this; }
		public Builder<T> rdfThriftT(String value) { expected.put("rdfThriftT", value); return this; }
		public Builder<T> rdfProto(String value) { expected.put("rdfProto", value); return this; }
		public Builder<T> rdfProtoT(String value) { expected.put("rdfProtoT", value); return this; }
		public Builder<T> yaml(String value) { expected.put("yaml", value); return this; }
		public Builder<T> yamlT(String value) { expected.put("yamlT", value); return this; }
		public Builder<T> yamlR(String value) { expected.put("yamlR", value); return this; }
		public Builder<T> csv(String value) { expected.put("csv", value); return this; }

		public ComboRoundTrip_Tester<T> build() {
			return new ComboRoundTrip_Tester<>(this);
		}
	}

	private final String label;
	private final Supplier<T> in;
	private final String exceptionMsg;
	private final Predicate<String> skipTest;
	private final List<Function<T,String>> verify;
	private final Type type;
	private final Map<String,String> expected;
	private final Map<String,Serializer> serializers = map();
	private final Map<String,Parser> parsers = map();
	private final Function<T,T> postConvert;

	private ComboRoundTrip_Tester(Builder<T> b) {
		label = "[" + b.index + "] " + b.label;
		type = b.type;
		in = b.in;
		expected = b.expected;
		postConvert = b.postConvert;
		verify = b.verify;
		skipTest = b.skipTest;
		exceptionMsg = b.exceptionMsg;

		serializers.put("json", create(b, Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType()));
		serializers.put("jsonT", create(b, JsonSerializer.create().json5().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("jsonR", create(b, Json5Serializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType()));
		serializers.put("xml", create(b, XmlSerializer.DEFAULT_SQ.copy().addBeanTypes().addRootType()));
		serializers.put("xmlT", create(b, XmlSerializer.create().sq().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("xmlR", create(b, XmlSerializer.DEFAULT_SQ_READABLE.copy().addBeanTypes().addRootType()));
		serializers.put("xmlNs", create(b, XmlSerializer.DEFAULT_NS_SQ.copy().addBeanTypes().addRootType()));
		serializers.put("html", create(b, HtmlSerializer.DEFAULT_SQ.copy().addBeanTypes().addRootType()));
		serializers.put("htmlT", create(b, HtmlSerializer.create().sq().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("htmlR", create(b, HtmlSerializer.DEFAULT_SQ_READABLE.copy().addBeanTypes().addRootType()));
		serializers.put("uon", create(b, UonSerializer.DEFAULT.copy().addBeanTypes().addRootType()));
		serializers.put("uonT", create(b, UonSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("uonR", create(b, UonSerializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType()));
		serializers.put("urlEnc", create(b, UrlEncodingSerializer.DEFAULT.copy().addBeanTypes().addRootType()));
		serializers.put("urlEncT", create(b, UrlEncodingSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("urlEncR", create(b, UrlEncodingSerializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType()));
		serializers.put("msgPack", create(b, MsgPackSerializer.create().addBeanTypes().addRootType()));
		serializers.put("msgPackT", create(b, MsgPackSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("rdfXml", create(b, RdfXmlSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfXmlT", create(b, RdfXmlSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("rdfXmlR", create(b, RdfXmlSerializer.create().useWhitespace().addBeanTypes().addRootType()));
		serializers.put("rdfXmlAbbrev", create(b, RdfXmlAbbrevSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfTurtle", create(b, TurtleSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfN3", create(b, N3Serializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfNtriple", create(b, NTripleSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfNquads", create(b, NQuadsSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfTrig", create(b, TriGSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfJsonLd", create(b, JsonLdSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfJson", create(b, RdfJsonSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfTriX", create(b, TriXSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfThrift", create(b, RdfThriftSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfThriftT", create(b, RdfThriftSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("rdfProto", create(b, RdfProtoSerializer.create().addBeanTypes().addRootType()));
		serializers.put("rdfProtoT", create(b, RdfProtoSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("yaml", create(b, YamlSerializer.DEFAULT.copy().addBeanTypes().addRootType()));
		serializers.put("yamlT", create(b, YamlSerializer.create().typePropertyName("t").addBeanTypes().addRootType()));
		serializers.put("yamlR", create(b, YamlSerializer.DEFAULT_READABLE.copy().addBeanTypes().addRootType()));
		serializers.put("csv", create(b, CsvSerializer.create()));

		parsers.put("json", create(b, JsonParser.DEFAULT.copy()));
		parsers.put("jsonT", create(b, JsonParser.create().typePropertyName("t")));
		parsers.put("jsonR", create(b, JsonParser.DEFAULT.copy()));
		parsers.put("xml", create(b, XmlParser.DEFAULT.copy()));
		parsers.put("xmlT", create(b, XmlParser.create().typePropertyName("t")));
		parsers.put("xmlR", create(b, XmlParser.DEFAULT.copy()));
		parsers.put("xmlNs", create(b, XmlParser.DEFAULT.copy()));
		parsers.put("html", create(b, HtmlParser.DEFAULT.copy()));
		parsers.put("htmlT", create(b, HtmlParser.create().typePropertyName("t")));
		parsers.put("htmlR", create(b, HtmlParser.DEFAULT.copy()));
		parsers.put("uon", create(b, UonParser.DEFAULT.copy()));
		parsers.put("uonT", create(b, UonParser.create().typePropertyName("t")));
		parsers.put("uonR", create(b, UonParser.DEFAULT.copy()));
		parsers.put("urlEnc", create(b, UrlEncodingParser.DEFAULT.copy()));
		parsers.put("urlEncT", create(b, UrlEncodingParser.create().typePropertyName("t")));
		parsers.put("urlEncR", create(b, UrlEncodingParser.DEFAULT.copy()));
		parsers.put("msgPack", create(b, MsgPackParser.DEFAULT.copy()));
		parsers.put("msgPackT", create(b, MsgPackParser.create().typePropertyName("t")));
		parsers.put("rdfXml", create(b, RdfXmlParser.DEFAULT.copy()));
		parsers.put("rdfXmlT", create(b, RdfXmlParser.create().typePropertyName("t")));
		parsers.put("rdfXmlR", create(b, RdfXmlParser.DEFAULT.copy()));
		parsers.put("rdfXmlAbbrev", create(b, RdfXmlParser.DEFAULT.copy()));
		parsers.put("rdfTurtle", create(b, TurtleParser.DEFAULT.copy()));
		parsers.put("rdfN3", create(b, N3Parser.DEFAULT.copy()));
		parsers.put("rdfNtriple", create(b, NTripleParser.DEFAULT.copy()));
		parsers.put("rdfNquads", create(b, NQuadsParser.DEFAULT.copy()));
		parsers.put("rdfTrig", create(b, TriGParser.DEFAULT.copy()));
		parsers.put("rdfJsonLd", create(b, JsonLdParser.DEFAULT.copy()));
		parsers.put("rdfJson", create(b, RdfJsonParser.DEFAULT.copy()));
		parsers.put("rdfTriX", create(b, TriXParser.DEFAULT.copy()));
		parsers.put("rdfThrift", create(b, RdfThriftParser.DEFAULT.copy()));
		parsers.put("rdfThriftT", create(b, RdfThriftParser.create().typePropertyName("t")));
		parsers.put("rdfProto", create(b, RdfProtoParser.DEFAULT.copy()));
		parsers.put("rdfProtoT", create(b, RdfProtoParser.create().typePropertyName("t")));
		parsers.put("yaml", create(b, YamlParser.DEFAULT.copy()));
		parsers.put("yamlT", create(b, YamlParser.create().typePropertyName("t")));
		parsers.put("yamlR", create(b, YamlParser.DEFAULT.copy()));
		parsers.put("csv", create(b, CsvParser.create()));
	}

	private Serializer create(Builder<?> tb, Serializer.Builder sb) {
		tb.serializerApply.accept(sb);
		sb.swaps(tb.swaps);
		tb.applies.forEach(x -> {
			if (x.getA().equals(BeanContext.Builder.class))
				sb.beanContext((Consumer<BeanContext.Builder>) x.getB());
			else if (x.getA().isInstance(sb))
				sb.asSubtype(Serializer.Builder.class).ifPresent((Consumer<Serializer.Builder>) x.getB());
		});
		return sb.build();
	}

	private Parser create(Builder<?> tb, Parser.Builder pb) {
		tb.parserApply.accept(pb);
		pb.swaps(tb.swaps);
		tb.applies.forEach(x -> {
			if (x.getA().equals(BeanContext.Builder.class))
				pb.beanContext((Consumer<BeanContext.Builder>) x.getB());
			else if (x.getA().isInstance(pb))
				pb.asSubtype(Parser.Builder.class).ifPresent((Consumer<Parser.Builder>) x.getB());
		});
		return pb.build();
	}

	private void verify(T o, String testName) {
		for (var v : verify) {
			var s = v.apply(o);
			if (ne(s)) {
				throw new BasicAssertionError("Verification failed on test {0}/{1}: {2}", label, testName, s);
			}
		}
	}

	private boolean isSkipped(String testName, String expected) {
		return expected == null || "SKIP".equals(expected) || skipTest.test(testName);
	}

	public void testSerialize(String testName) throws Exception {
		var s = serializers.get(testName);
		var exp = expected.get(testName);
		try {
			if (isSkipped(testName + "-serialize", exp)) return;

			var r = s.serializeToString(in.get());

			// Specifying "xxx" in the expected results will spit out what we should populate the field with.
			if (eq(exp, "xxx")) {
				System.out.println(getClass().getName() + ": " + label + "/" + testName + "=\n" + r.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")); // NOT DEBUG
				System.out.println(r);
			}

			assertEquals(exp, r, fs("{0}/{1} serialize-normal failed.", label, testName));
		} catch (AssertionError e) {
			if (exceptionMsg == null)
				throw e;
			assertContains(exceptionMsg, e.getMessage());
		} catch (Exception e) {
			if (exceptionMsg == null)
				throw new BasicAssertionError(e, "{0}/{1} failed.  exception={2}", label, testName, e.getLocalizedMessage());
			assertContains(exceptionMsg, e.getMessage());
		}
	}

	public void testParse(String testName) throws Exception {
		var s = serializers.get(testName);
		var exp = expected.get(testName);
		var p = parsers.get(testName);
		try {
			if (isSkipped(testName + "-parse", exp)) return;

			var r = s.serializeToString(in.get());
			var o = p.parse(r, type);
			o = postConvert.apply((T)o);
			r = s.serializeToString(o);

			assertEquals(exp, r, fs("{0}/{1} parse-normal failed", label, testName));
		} catch (AssertionError e) {
			if (exceptionMsg == null)
				throw e;
			assertContains(exceptionMsg, e.getMessage());
		} catch (Throwable e) {
			if (exceptionMsg == null)
				throw new BasicAssertionError(e, "{0}/{1} failed.  exception={2}", label, testName, e.getLocalizedMessage());
			assertContains(exceptionMsg, e.getMessage());
		}
	}

	public void testParseVerify(String testName) throws Exception {
		var s = serializers.get(testName);
		var p = parsers.get(testName);
		try {
			if (isSkipped(testName + "verify", expected.get(testName))) return;

			var r = s.serializeToString(in.get());
			var o = p.parse(r, type);

			verify((T)o, testName);
		} catch (AssertionError e) {
			if (exceptionMsg == null)
				throw e;
			assertContains(exceptionMsg, e.getMessage());
		} catch (Exception e) {
			if (exceptionMsg == null)
				throw new BasicAssertionError(e, "{0}/{1} failed.  exception={2}", label, testName, e.getLocalizedMessage());
			assertContains(exceptionMsg, e.getMessage());
		}
	}

	public void testParseJsonEquivalency(String testName) throws Exception {
		var s = serializers.get(testName);
		var js = (WriterSerializer)serializers.get("json");
		var exp = expected.get("json");
		var p = parsers.get(testName);
		try {
			if (isSkipped(testName + "-parseJsonEquivalency", expected.get(testName))) return;

			var r = s.serializeToString(in.get());
			var o = p.parse(r, type);
			r = js.serialize(o);
			assertEquals(exp, r, fs("{0}/{1} parse-normal failed on JSON equivalency", label, testName));
		} catch (AssertionError e) {
			if (exceptionMsg == null)
				throw e;
			assertContains(exceptionMsg, e.getMessage());
		} catch (Exception e) {
			if (exceptionMsg == null)
				throw new BasicAssertionError(e, "{0}/{1} failed.  exception={2}", label, testName, e.getLocalizedMessage());
			assertContains(exceptionMsg, e.getMessage());
		}
	}

	@Override
	public String toString() {
		return "ComboRoundTripTester: " + label;
	}
}