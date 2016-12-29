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
package org.apache.juneau.urlencoding;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link UonSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link UonSerializer#setProperty(String,Object)}
 * 	<li>{@link UonSerializer#setProperties(ObjectMap)}
 * 	<li>{@link UonSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link UonSerializer#addBeanFilters(Class[])}
 * 	<li>{@link UonSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link UonSerializer#addToDictionary(Class[])}
 * 	<li>{@link UonSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the URL-Encoding and UON serializers</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th><th>Session overridable</th></tr>
 * 	<tr>
 * 		<td>{@link #UON_simpleMode}</td>
 * 		<td>Use simplified output.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #UON_useWhitespace}</td>
 * 		<td>Use whitespace.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #UON_encodeChars}</td>
 * 		<td>Encode non-valid URI characters.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk> for {@link UonSerializer}<br><jk>true</jk> for {@link UrlEncodingSerializer}</td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Configurable properties inherited from parent classes</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../serializer/SerializerContext.html#ConfigProperties'>SerializerContext</a> - Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
 */
public class UonSerializerContext extends SerializerContext {

	/**
	 * <b>Configuration property:</b>  Use simplified output.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonSerializer.simpleMode"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, type flags will not be prepended to values in most cases.
	 * <p>
	 * Use this setting if the data types of the values (e.g. object/array/boolean/number/string)
	 * 	is known on the receiving end.
	 * <p>
	 * It should be noted that the default behavior produces a data structure that can
	 * 	be losslessly converted into JSON, and any JSON can be losslessly represented
	 * 	in a URL-encoded value.  However, this strict equivalency does not exist
	 * 	when simple mode is used.
	 * <p>
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>Input (in JSON)</th>
	 * 		<th>Normal mode output</th>
	 * 		<th>Simple mode output</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>{foo:'bar',baz:'bing'}</td>
	 * 		<td class='code'>$o(foo=bar,baz=bing)</td>
	 * 		<td class='code'>(foo=bar,baz=bing)</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>{foo:{bar:'baz'}}</td>
	 * 		<td class='code'>$o(foo=$o(bar=baz))</td>
	 * 		<td class='code'>(foo=(bar=baz))</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>['foo','bar']</td>
	 * 		<td class='code'>$a(foo,bar)</td>
	 * 		<td class='code'>(foo,bar)</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>['foo',['bar','baz']]</td>
	 * 		<td class='code'>$a(foo,$a(bar,baz))</td>
	 * 		<td class='code'>(foo,(bar,baz))</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>true</td>
	 * 		<td class='code'>$b(true)</td>
	 * 		<td class='code'>true</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>123</td>
	 * 		<td class='code'>$n(123)</td>
	 * 		<td class='code'>123</td>
	 * 	</tr>
	 * </table>
	 */
	public static final String UON_simpleMode = "UonSerializer.simpleMode";

	/**
	 * <b>Configuration property:</b>  Use whitespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonSerializer.useWhitespace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String UON_useWhitespace = "UonSerializer.useWhitespace";

	/**
	 * <b>Configuration property:</b>  Encode non-valid URI characters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonSerializer.encodeChars"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk> for {@link UonSerializer}, <jk>true</jk> for {@link UrlEncodingSerializer}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Encode non-valid URI characters with <js>"%xx"</js> constructs.
	 * <p>
	 * If <jk>true</jk>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * Set to <jk>false</jk> if parameter value is being passed to some other code that will already
	 * 	perform URL-encoding of non-valid URI characters.
	 */
	public static final String UON_encodeChars = "UonSerializer.encodeChars";


	final boolean
		simpleMode,
		useWhitespace,
		encodeChars;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public UonSerializerContext(ContextFactory cf) {
		super(cf);
		simpleMode = cf.getProperty(UON_simpleMode, boolean.class, false);
		useWhitespace = cf.getProperty(UON_useWhitespace, boolean.class, false);
		encodeChars = cf.getProperty(UON_encodeChars, boolean.class, false);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UonSerializerContext", new ObjectMap()
				.append("simpleMode", simpleMode)
				.append("useWhitespace", useWhitespace)
				.append("encodeChars", encodeChars)
			);
	}
}
