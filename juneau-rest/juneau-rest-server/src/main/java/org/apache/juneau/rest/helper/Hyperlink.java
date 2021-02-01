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
package org.apache.juneau.rest.helper;

import org.apache.juneau.dto.html5.*;

/**
 * Defines a simple hyperlink class.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyRest <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Produces &lt;a href=&quot;/foo&quot;>bar&lt;/a></jc>
 * 		<ja>@RestOp</ja>
 * 		<jk>public</jk> Hyperlink a01() {
 * 			<jk>return new</jk> Hyperlink(<js>"foo"</js>, <js>"bar"</js>);
 * 		}
 *
 * 		<jc>// Produces &lt;ul>&lt;li>&lt;a href=&quot;/foo&quot;>bar&lt;/a>&lt;/li>&lt;/ul></jc>
 * 		<ja>@RestOp</ja>
 * 		<jk>public</jk> Hyperlink[] a02() {
 * 			<jk>return new</jk> Hyperlink[]{a01()};
 * 		}
 *
 * 		<jc>// Produces &lt;ul>&lt;li>&lt;a href=&quot;/foo&quot;>bar&lt;/a>&lt;/li>&lt;/ul></jc>
 * 		<ja>@RestOp</ja>
 * 		<jk>public</jk> Collection&lt;Hyperlink> a03() {
 * 			<jk>return</jk> Arrays.<jsm>asList</jsm>(a02());
 * 		}
 * 	}
 * </p>
 */
public class Hyperlink extends A {
	/**
	 * Creates an empty {@link A} element.
	 */
	public Hyperlink() {}

	/**
	 * Creates an {@link A} element with the specified {@link A#href(Object)} attribute and {@link A#children(Object[])}
	 * nodes.
	 *
	 * @param href The {@link A#href(Object)} attribute.
	 * @param children The {@link A#children(Object[])} nodes.
	 */
	public Hyperlink(Object href, Object...children) {
		super(href, children);
	}
}
