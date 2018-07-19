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
package org.apache.juneau.html;

import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Allows custom rendering of bean property values when serialized as HTML.
 *
 * <p>
 * Associated with bean properties using the {@link Html#render() @Html.render()} annotation.
 *
 * <p>
 * Using this class, you can alter the CSS style and HTML content of the bean property.
 *
 * <p>
 * The following example shows two render classes that customize the appearance of the <code>pctFull</code> and
 * <code>status</code> columns shown below:
 *
 * <p>
 * <img class='bordered' src='doc-files/HtmlRender_1.png'>
 *
 * <p class='bcode w800'>
 * 	<jc>// Our bean class</jc>
 * 	<jk>public class</jk> FileSpace {
 *
 * 		<jk>private final</jk> String <jf>drive</jf>;
 * 		<jk>private final long</jk> <jf>total</jf>, <jf>available</jf>;
 *
 * 		<jk>public</jk> FileSpace(String drive, <jk>long</jk> total, <jk>long</jk> available) {
 * 			<jk>this</jk>.<jf>drive</jf> = drive;
 * 			<jk>this</jk>.<jf>total</jf> = total;
 * 			<jk>this</jk>.<jf>available</jf> = available;
 * 		}
 *
 * 		<ja>@Html</ja>(link=<js>"drive/{drive}"</js>)
 * 		<jk>public</jk> String getDrive() {
 * 			<jk>return</jk> <jf>drive</jf>;
 * 		}
 *
 * 		<jk>public long</jk> getTotal() {
 * 			<jk>return</jk> <jf>total</jf>;
 * 		}
 *
 * 		<jk>public long</jk> getAvailable() {
 * 			<jk>return</jk> <jf>available</jf>;
 * 		}
 *
 * 		<ja>@Html</ja>(render=FileSpacePctRender.<jk>class</jk>)
 * 		<jk>public float</jk> getPctFull() {
 * 			<jk>return</jk> ((100 * <jf>available</jf>) / <jf>total</jf>);
 * 		}
 *
 * 		<ja>@Html</ja>(render=FileSpaceStatusRender.<jk>class</jk>)
 * 		<jk>public</jk> FileSpaceStatus getStatus() {
 * 			<jk>float</jk> pf = getPctFull();
 * 			<jk>if</jk> (pf &lt; 80)
 * 				<jk>return</jk> FileSpaceStatus.<jsf>OK</jsf>;
 * 			<jk>if</jk> (pf &lt; 90)
 * 				<jk>return</jk> FileSpaceStatus.<jsf>WARNING</jsf>;
 * 			<jk>return</jk> FileSpaceStatus.<jsf>SEVERE</jsf>;
 * 		}
 * 	}
 *
 * 	<jc>// Possible values for the getStatus() method</jc>
 * 	<jk>public static enum</jk> FileSpaceStatus {
 * 		<jsf>OK</jsf>, <jsf>WARNING</jsf>, <jsf>SEVERE</jsf>;
 * 	}
 *
 * 	<jc>// Custom render for getPctFull() method</jc>
 * 	<jk>public static class</jk> FileSpacePctRender <jk>extends</jk> HtmlRender&lt;Float&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getStyle(SerializerSession session, Float value) {
 * 			<jk>if</jk> (value &lt; 80)
 * 				<jk>return</jk> <js>"background-color:lightgreen;text-align:center"</js>;
 * 			<jk>if</jk> (value &lt; 90)
 * 				<jk>return</jk> <js>"background-color:yellow;text-align:center"</js>;
 * 			<jk>return</jk> <js>"background-color:red;text-align:center;border:;animation:color_change 0.5s infinite alternate"</js>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Object getContent(SerializerSession session, Float value) {
 * 			<jk>if</jk> (value >= 90)
 * 				<jk>return</jk> <jsm>div</jsm>(
 * 					String.<jsm>format</jsm>(<js>"%.0f%%"</js>, value),
 * 					<jsm>style</jsm>(<js>"@keyframes color_change { from { background-color: red; } to { background-color: yellow; }"</js>)
 * 				);
 * 			<jk>return</jk> String.<jsm>format</jsm>(<js>"%.0f%%"</js>, value);
 * 		}
 * 	}
 *
 * 	<jc>// Custom render for getStatus() method</jc>
 * 	<jk>public static class</jk> FileSpaceStatusRender <jk>extends</jk> HtmlRender&lt;FileSpaceStatus&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String getStyle(SerializerSession session, FileSpaceStatus value) {
 * 			<jk>return</jk> <js>"text-align:center"</js>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Object getContent(SerializerSession session, FileSpaceStatus value) {
 * 			<jk>switch</jk> (value) {
 * 				<jk>case</jk> <jsf>OK</jsf>:  <jk>return</jk> <jsm>img</jsm>().src(URI.<jsm>create</jsm>(<js>"servlet:/htdocs/ok.png"</js>));
 * 				<jk>case</jk> <jsf>WARNING</jsf>:  <jk>return</jk> <jsm>img</jsm>().src(URI.<jsm>create</jsm>(<js>"servlet:/htdocs/warning.png"</js>));
 * 				<jk>default</jk>: <jk>return</jk> <jsm>img</jsm>().src(URI.<jsm>create</jsm>(<js>"servlet:/htdocs/severe.png"</js>));
 * 			}
 * 		}
 * 	}
 * </p>
 *
 * @param <T> The bean property type.
 */
public abstract class HtmlRender<T> {

	/**
	 * Returns the CSS style of the element containing the bean property value.
	 *
	 * @param session
	 * 	The current serializer session.
	 * 	Can be used to retrieve properties and session-level information.
	 * @param value The bean property value.
	 * @return The CSS style string, or <jk>null</jk> if no style should be added.
	 */
	public String getStyle(SerializerSession session, T value) {
		return null;
	}

	/**
	 * Returns the delegate value for the specified bean property value.
	 *
	 * <p>
	 * The default implementation simply returns the same value.
	 * A typical use is to return an HTML element using one of the HTML5 DOM beans.
	 *
	 * @param session
	 * 	The current serializer session.
	 * 	Can be used to retrieve properties and session-level information.
	 * @param value The bean property value.
	 * @return The new bean property value.
	 */
	public Object getContent(SerializerSession session, T value) {
		return value;
	}
}
