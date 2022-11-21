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
package org.apache.juneau;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.utils.*;

/**
 * Represents the input to a ComboTest.
 * @param <T>
 */
public class ComboInput<T> {

	final String label;
	final Supplier<T> in;
	String exceptionMsg;
	private Predicate<String> skipTest;
	private Function<T,T> convert;
	private List<Function<T,String>> verify = list();
	List<Class<?>> swaps = list();
	final Type type;
	String json, jsonT, jsonR, xml, xmlT, xmlR, xmlNs, html, htmlT, htmlR, uon, uonT, uonR, urlEncoding,
		urlEncodingT, urlEncodingR, msgPack, msgPackT, rdfXml, rdfXmlT, rdfXmlR;
	List<Tuple2<Class<?>,Consumer<?>>> applies = list();

	public ComboInput<T> beanContext(Consumer<BeanContext.Builder> c) {
		apply(BeanContext.Builder.class, c);
		return this;
	}

	public <T2> ComboInput<T> apply(Class<T2> t, Consumer<T2> c) {
		applies.add(Tuple2.of(t, c));
		return this;
	}

	public ComboInput<T> exceptionMsg(String exceptionMsg) {
		this.exceptionMsg = exceptionMsg;
		return this;
	}

	public ComboInput<T> skipTest(Predicate<String> skipTest) {
		this.skipTest = skipTest;
		return this;
	}

	public ComboInput<T> convert(Function<T,T> convert) {
		this.convert = convert;
		return this;
	}

	public ComboInput<T> verify(Function<T,String> verify) {
		this.verify.add(verify);
		return this;
	}

	public ComboInput<T> verify(Predicate<T> verify, String msg, Object...args) {
		this.verify.add(x -> verify.test(x) ? null : StringUtils.format(msg, args));
		return this;
	}

	public ComboInput<T> swaps(Class<?>...c) {
		this.swaps.addAll(Arrays.asList(c));
		return this;
	}

	public ComboInput(String label, Type type, T in) {
		this.label = label;
		this.type = type;
		this.in = () -> in;
	}

	public ComboInput(String label, Type type, Supplier<T> in) {
		this.label = label;
		this.type = type;
		this.in = in;
	}

	public ComboInput<T> json(String value) {
		this.json = value;
		return this;
	}
	public ComboInput<T> jsonT(String value) {
		this.jsonT = value;
		return this;
	}
	public ComboInput<T> jsonR(String value) {
		this.jsonR = value;
		return this;
	}
	public ComboInput<T> xml(String value) {
		this.xml = value;
		return this;
	}
	public ComboInput<T> xmlT(String value) {
		this.xmlT = value;
		return this;
	}
	public ComboInput<T> xmlR(String value) {
		this.xmlR = value;
		return this;
	}
	public ComboInput<T> xmlNs(String value) {
		this.xmlNs = value;
		return this;
	}
	public ComboInput<T> html(String value) {
		this.html = value;
		return this;
	}
	public ComboInput<T> htmlT(String value) {
		this.htmlT = value;
		return this;
	}
	public ComboInput<T> htmlR(String value) {
		this.htmlR = value;
		return this;
	}
	public ComboInput<T> uon(String value) {
		this.uon = value;
		return this;
	}
	public ComboInput<T> uonT(String value) {
		this.uonT = value;
		return this;
	}
	public ComboInput<T> uonR(String value) {
		this.uonR = value;
		return this;
	}
	public ComboInput<T> urlEnc(String value) {
		this.urlEncoding = value;
		return this;
	}
	public ComboInput<T> urlEncT(String value) {
		this.urlEncodingT = value;
		return this;
	}
	public ComboInput<T> urlEncR(String value) {
		this.urlEncodingR = value;
		return this;
	}
	public ComboInput<T> msgPack(String value) {
		this.msgPack = value;
		return this;
	}
	public ComboInput<T> msgPackT(String value) {
		this.msgPackT = value;
		return this;
	}
	public ComboInput<T> rdfXml(String value) {
		this.rdfXml = value;
		return this;
	}
	public ComboInput<T> rdfXmlT(String value) {
		this.rdfXmlT = value;
		return this;
	}
	public ComboInput<T> rdfXmlR(String value) {
		this.rdfXmlR = value;
		return this;
	}

	public ComboInput(
			String label,
			Type type,
			T in,
			String json,
			String jsonT,
			String jsonR,
			String xml,
			String xmlT,
			String xmlR,
			String xmlNs,
			String html,
			String htmlT,
			String htmlR,
			String uon,
			String uonT,
			String uonR,
			String urlEncoding,
			String urlEncodingT,
			String urlEncodingR,
			String msgPack,
			String msgPackT,
			String rdfXml,
			String rdfXmlT,
			String rdfXmlR
		) {
		this.label = label;
		this.type = type;
		this.in = () -> in;
		this.json = json;
		this.jsonT = jsonT;
		this.jsonR = jsonR;
		this.xml = xml;
		this.xmlT = xmlT;
		this.xmlR = xmlR;
		this.xmlNs = xmlNs;
		this.html = html;
		this.htmlT = htmlT;
		this.htmlR = htmlR;
		this.uon = uon;
		this.uonT = uonT;
		this.uonR = uonR;
		this.urlEncoding = urlEncoding;
		this.urlEncodingT = urlEncodingT;
		this.urlEncodingR = urlEncodingR;
		this.msgPack = msgPack;
		this.msgPackT = msgPackT;
		this.rdfXml = rdfXml;
		this.rdfXmlT = rdfXmlT;
		this.rdfXmlR = rdfXmlR;
	}

	/**
	 * Checks to see if the test should be skipped.
	 */
	public boolean isTestSkipped(String testName) throws Exception {
		return skipTest != null && skipTest.test(testName);
	}

	public T convert(T t) {
		return convert == null ? t : convert.apply(t);
	}

	/**
	 * Override this method if you want to do a post-parse verification on the object.
	 * <p>
	 * Note that a Function would be preferred here, but it's not available in Java 6.
	 *
	 * @param o The object returned by the parser.
	 */
	public void verify(T o, String testName) {
		for (Function<T,String> f : verify) {
			String s = f.apply(o);
			if (! StringUtils.isEmpty(s)) {
				throw new BasicAssertionError("Verification failed on test {0}/{1}: {2}", label, testName, s);
			}
		}
	}
}
