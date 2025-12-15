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

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;

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
		this.verify.add(x -> verify.test(x) ? null : f(msg, args));
		return this;
	}

	public ComboInput<T> swaps(Class<?>...c) {
		this.swaps.addAll(l(c));
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
		json = value;
		return this;
	}
	public ComboInput<T> jsonT(String value) {
		jsonT = value;
		return this;
	}
	public ComboInput<T> jsonR(String value) {
		jsonR = value;
		return this;
	}
	public ComboInput<T> xml(String value) {
		xml = value;
		return this;
	}
	public ComboInput<T> xmlT(String value) {
		xmlT = value;
		return this;
	}
	public ComboInput<T> xmlR(String value) {
		xmlR = value;
		return this;
	}
	public ComboInput<T> xmlNs(String value) {
		xmlNs = value;
		return this;
	}
	public ComboInput<T> html(String value) {
		html = value;
		return this;
	}
	public ComboInput<T> htmlT(String value) {
		htmlT = value;
		return this;
	}
	public ComboInput<T> htmlR(String value) {
		htmlR = value;
		return this;
	}
	public ComboInput<T> uon(String value) {
		uon = value;
		return this;
	}
	public ComboInput<T> uonT(String value) {
		uonT = value;
		return this;
	}
	public ComboInput<T> uonR(String value) {
		uonR = value;
		return this;
	}
	public ComboInput<T> urlEnc(String value) {
		urlEncoding = value;
		return this;
	}
	public ComboInput<T> urlEncT(String value) {
		urlEncodingT = value;
		return this;
	}
	public ComboInput<T> urlEncR(String value) {
		urlEncodingR = value;
		return this;
	}
	public ComboInput<T> msgPack(String value) {
		msgPack = value;
		return this;
	}
	public ComboInput<T> msgPackT(String value) {
		msgPackT = value;
		return this;
	}
	public ComboInput<T> rdfXml(String value) {
		rdfXml = value;
		return this;
	}
	public ComboInput<T> rdfXmlT(String value) {
		rdfXmlT = value;
		return this;
	}
	public ComboInput<T> rdfXmlR(String value) {
		rdfXmlR = value;
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
	public boolean isTestSkipped(String testName) {
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
		for (var f : verify) {
			var s = f.apply(o);
			if (isNotEmpty(s)) {
				throw new BasicAssertionError("Verification failed on test {0}/{1}: {2}", label, testName, s);
			}
		}
	}
}