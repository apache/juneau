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
 * 	<li>{@link UonSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class UonSerializerContext extends SerializerContext {

	/**
	 * Use simplified output ({@link Boolean}, default=<jk>false</jk>).
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
	 * Use whitespace in output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String UON_useWhitespace = "UonSerializer.useWhitespace";

	/**
	 * Encode non-valid URI characters to <js>"%xx"</js> constructs. ({@link Boolean}, default=<jk>false</jk> for {@link UonSerializer}, <jk>true</jk> for {@link UrlEncodingSerializer}).
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
}
