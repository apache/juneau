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
package org.apache.juneau.soap;


/**
 * Properties associated with the {@link SoapXmlSerializer} class.
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the SOAP/XML serializer</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #SOAPXML_SOAPAction}</td>
 * 		<td>The <code>SOAPAction</code> HTTP header value to set on responses.</td>
 * 		<td><code>String</code></td>
 * 		<td><js>"http://www.w3.org/2003/05/soap-envelope"</js></td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Inherited configurable properties</h6>
 * <ul class='doctree'>
 * 	<li class='jc'><a class="doclink" href="../BeanContext.html#ConfigProperties">BeanContext</a>
 * 		- Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='jc'><a class="doclink"
 * 			href="../serializer/SerializerContext.html#ConfigProperties">SerializerContext</a>
 * 			- Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
 */
public final class SoapXmlSerializerContext {

	/**
	 * <b>Configuration property:</b>  The <code>SOAPAction</code> HTTP header value to set on responses.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"SoapXmlSerializer.SOAPAction"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"http://www.w3.org/2003/05/soap-envelope"</js>
	 * </ul>
	 */
	public static final String SOAPXML_SOAPAction = "SoapXmlSerializer.SOAPAction";
}
