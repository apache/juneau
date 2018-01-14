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

import java.lang.reflect.*;

/**
 * Represents the input to a ComboTest.
 * @param <T>
 */
public class ComboInput<T> {

	final String label;
	private final T in;
	final Type type;
	final String json, jsonT, jsonR, xml, xmlT, xmlR, xmlNs, html, htmlT, htmlR, uon, uonT, uonR, urlEncoding,
		urlEncodingT, urlEncodingR, msgPack, msgPackT, rdfXml, rdfXmlT, rdfXmlR;

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
		this.in = in;
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
	 * Returns the input object.
	 * Override this method if you want it dynamically created each time.
	 * @throws Exception 
	 */
	public T getInput() throws Exception {
		return in;
	}
	
	/**
	 * Override this method if you want to do a post-parse verification on the object.
	 * <p>
	 * Note that a Function would be preferred here, but it's not available in Java 6.
	 * 
	 * @param o The object returned by the parser.
	 */
	public void verify(T o) {}
}
