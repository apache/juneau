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

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.net.*;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#the-script-element">&lt;script&gt;</a>
 * element.
 *
 * <p>
 * The script element is used to embed or reference executable code, typically JavaScript. It can
 * contain inline script code or reference external script files. The script element is commonly
 * used to add interactivity and dynamic behavior to web pages.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Inline JavaScript</jc>
 * 	Script <jv>script1</jv> = <jsm>script</jsm>()
 * 		.text(<js>"console.log('Hello, World!');"</js>);
 *
 * 	<jc>// External JavaScript file</jc>
 * 	Script <jv>script2</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/script.js"</js>)
 * 		.type(<js>"text/javascript"</js>);
 *
 * 	<jc>// Async script loading</jc>
 * 	Script <jv>script3</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/analytics.js"</js>)
 * 		.async(<jk>true</jk>)
 * 		.defer(<jk>true</jk>);
 *
 * 	<jc>// Script with integrity check</jc>
 * 	Script <jv>script4</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/library.js"</js>)
 * 		.integrity(<js>"sha384-..."</js>)
 * 		.crossorigin(<js>"anonymous"</js>);
 *
 * 	<jc>// Module script</jc>
 * 	Script <jv>script5</jv> = <jsm>script</jsm>()
 * 		.src(<js>"https://example.com/module.js"</js>)
 * 		.type(<js>"module"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#script() script()}
 * 		<li class='jm'>{@link HtmlBuilder#script(String, String...) script(String, String...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "script")
public class Script extends HtmlElementRawText<Script> {

	/**
	 * Creates an empty {@link Script} element.
	 */
	public Script() {}

	/**
	 * Creates a {@link Script} element with the specified {@link Script#type(String)} attribute and
	 * {@link Script#text(Object)} node.
	 *
	 * @param type The {@link Script#type(String)} attribute. Can be <jk>null</jk> to unset the attribute.
	 * @param text The child text node. Can be <jk>null</jk> to leave the text unset.
	 */
	public Script(String type, String...text) {
		type(type).text(joinnl(text));
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-async">async</a> attribute.
	 *
	 * <p>
	 * Execute script asynchronously.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"async"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Script async(Object value) {
		attr("async", deminimize(value, "async"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-charset">charset</a> attribute.
	 *
	 * <p>
	 * Specifies the character encoding of the external script resource. This is used when
	 * the script is loaded from an external source.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"utf-8"</js> - UTF-8 encoding (default)</li>
	 * 	<li><js>"iso-8859-1"</js> - Latin-1 encoding</li>
	 * 	<li><js>"windows-1252"</js> - Windows-1252 encoding</li>
	 * </ul>
	 *
	 * @param value The character encoding of the external script resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Script charset(String value) {
		attr("charset", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-crossorigin">crossorigin</a>
	 * attribute.
	 *
	 * <p>
	 * How the element handles cross-origin requests.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Script crossorigin(String value) {
		attr("crossorigin", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-defer">defer</a> attribute.
	 *
	 * <p>
	 * Defer script execution.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"defer"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Script defer(Object value) {
		attr("defer", deminimize(value, "defer"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-src">src</a> attribute.
	 *
	 * <p>
	 * Address of the resource.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * 	Can be <jk>null</jk>, in which case the attribute is stored with a <jk>null</jk> value (not removed).
	 * @return This object.
	 */
	public Script src(Object value) {
		attrUri("src", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#attr-script-type">type</a> attribute.
	 *
	 * <p>
	 * Type of embedded resource.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Script type(String value) {
		attr("type", value);
		return this;
	}
}
