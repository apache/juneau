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
package org.apache.juneau.bean.html5;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-keygen-element">&lt;keygen&gt;</a>
 * element.
 *
 * <p>
 * The keygen element represents a key-pair generator control for forms. It generates a public/private
 * key pair and submits the public key to the server. This element is deprecated in HTML5 and should
 * not be used in new projects. Modern web applications should use Web Crypto API instead.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Basic keygen with RSA key type</jc>
 * 	Keygen <jv>keygen1</jv> = <jsm>keygen</jsm>()
 * 		.name(<js>"userkey"</js>)
 * 		.keytype(<js>"RSA"</js>);
 *
 * 	<jc>// Keygen with challenge string</jc>
 * 	Keygen <jv>keygen2</jv> = <jsm>keygen</jsm>()
 * 		.name(<js>"certkey"</js>)
 * 		.keytype(<js>"RSA"</js>)
 * 		.challenge(<js>"server-challenge-string"</js>);
 *
 * 	<jc>// Keygen with form association</jc>
 * 	Keygen <jv>keygen3</jv> = <jsm>keygen</jsm>()
 * 		.name(<js>"formkey"</js>)
 * 		.keytype(<js>"DSA"</js>)
 * 		.form(<js>"myform"</js>);
 *
 * 	<jc>// Keygen with disabled state</jc>
 * 	Keygen <jv>keygen4</jv> = <jsm>keygen</jsm>()
 * 		.name(<js>"disabledkey"</js>)
 * 		.keytype(<js>"RSA"</js>)
 * 		.disabled(<jk>true</jk>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#keygen() keygen()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "keygen")
public class Keygen extends HtmlElementVoid<Keygen> {

	/**
	 * Creates an empty {@link Keygen} element.
	 */
	public Keygen() { /* Empty constructor. */ }

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Keygen autofocus(Object value) {
		attr("autofocus", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-keygen-challenge">challenge</a> attribute.
	 *
	 * <p>
	 * Specifies a challenge string that will be packaged with the generated and signed public key.
	 * This is used for additional security in key generation processes.
	 *
	 * <p>
	 * The challenge string is typically provided by the server and used to verify the key generation.
	 *
	 * @param value The challenge string to package with the generated key. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Keygen challenge(String value) {
		attr("challenge", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-disabled">disabled</a> attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"disabled"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Keygen disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the keygen element with a form element by specifying the form's ID. This allows the keygen
	 * to be placed outside the form element while still being part of the form.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this keygen. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Keygen form(String value) {
		attr("form", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-keygen-keytype">keytype</a> attribute.
	 *
	 * <p>
	 * Specifies the type of cryptographic key to generate.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 *  	<li><js>"RSA"</js> - RSA key pair (default)</li>
	 *  	<li><js>"DSA"</js> - DSA key pair</li>
	 *  	<li><js>"EC"</js> - Elliptic curve key pair</li>
	 * </ul>
	 *
	 * @param value The type of cryptographic key to generate. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Keygen keytype(String value) {
		attr("keytype", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the keygen element. This name is used when the form is submitted and
	 * can be used to access the element via the form.elements API.
	 *
	 * <p>
	 * The name should be unique within the form and should not contain spaces or special characters.
	 *
	 * @param value The name of the keygen element for submission and API access. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Keygen name(String value) {
		attr("name", value);
		return this;
	}

}