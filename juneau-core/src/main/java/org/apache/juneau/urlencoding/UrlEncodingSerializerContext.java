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

/**
 * Configurable properties on the {@link UrlEncodingSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link UrlEncodingSerializer#setProperty(String,Object)}
 * 	<li>{@link UrlEncodingSerializer#setProperties(ObjectMap)}
 * 	<li>{@link UrlEncodingSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link UrlEncodingSerializer#addBeanFilters(Class[])}
 * 	<li>{@link UrlEncodingSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link UrlEncodingSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class UrlEncodingSerializerContext extends UonSerializerContext {

	/**
	 * Serialize bean property collections/arrays as separate key/value pairs ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>false</jk>, serializing the array <code>[1,2,3]</code> results in <code>?key=$a(1,2,3)</code>.
	 * If <jk>true</jk>, serializing the same array results in <code>?key=1&key=2&key=3</code>.
	 * <p>
	 * Example:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> String[] f1 = {<js>"a"</js>,<js>"b"</js>};
	 * 		<jk>public</jk> List&lt;String&gt; f2 = <jk>new</jk> LinkedList&lt;String&gt;(Arrays.<jsm>asList</jsm>(<jk>new</jk> String[]{<js>"c"</js>,<js>"d"</js>}));
	 * 	}
	 *
	 * 	UrlEncodingSerializer s1 = <jk>new</jk> UrlEncodingParser();
	 * 	UrlEncodingSerializer s2 = <jk>new</jk> UrlEncodingParser().setProperty(UrlEncodingContext.<jsf>URLENC_expandedParams</jsf>, <jk>true</jk>);
	 *
	 * 	String s1 = p1.serialize(<jk>new</jk> A()); <jc>// Produces "f1=(a,b)&f2=(c,d)"</jc>
	 * 	String s2 = p2.serialize(<jk>new</jk> A()); <jc>// Produces "f1=a&f1=b&f2=c&f2=d"</jc>
	 * </p>
	 * <p>
	 * <b>Important note:</b>  If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * is added to it.
	 * <p>
	 * This option only applies to beans.
	 */
	public static final String URLENC_expandedParams = "UrlEncoding.expandedParams";


	final boolean
		expandedParams;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public UrlEncodingSerializerContext(ContextFactory cf) {
		super(cf);
		this.expandedParams = cf.getProperty(URLENC_expandedParams, boolean.class, false);
	}
}
