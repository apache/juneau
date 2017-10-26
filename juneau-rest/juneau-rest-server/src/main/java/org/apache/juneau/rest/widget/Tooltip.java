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
package org.apache.juneau.rest.widget;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.html5.*;

/**
 * Simple template for adding tooltips to HTML5 bean constructs, typically in menu item widgets.
 *
 * <p>
 * Tooltips depend on the existence of the <code>tooltip</code> and <code>tooltiptext</code> styles that should be
 * present in the stylesheet for the document.
 *
 * <p>
 * The following examples shows how tooltips can be added to a menu item widget.
 *
 * <p class='bcode'>
 * <jk>public class</jk> MyFormMenuItem <jk>extends</jk> MenuItemWidget {
 *
 * 	<ja>@Override</ja>
 * 	<jk>public</jk> String getLabel(RestRequest req) <jk>throws</jk> Exception {
 * 		<jk>return</jk> <js>"myform"</js>;
 * 	}
 *
 * 	<ja>@Override</ja>
 * 	<jk>public</jk> Object getContent(RestRequest req) <jk>throws</jk> Exception {
 * 		<jk>return</jk> div(
 * 			<jsm>form</jsm>().id(<js>"form"</js>).action(<js>"servlet:/form"</js>).method(<jsf>POST</jsf>).children(
 * 				<jsm>table</jsm>(
 * 					<jsm>tr</jsm>(
 * 						<jsm>th</jsm>(<js>"Field 1:"</js>),
 * 						<jsm>td</jsm>(<jsm>input</jsm>().name(<js>"field1"</js>).type(<js>"text"</js>)),
 * 						<jsm>td</jsm>(<jk>new</jk> Tooltip(<js>"(?)"</js>, <js>"This is field #1!"</js>, br(), <js>"(e.g. '"</js>, code(<js>"Foo"</js>), <js>"')"</js>))
 * 					),
 * 					<jsm>tr</jsm>(
 * 						<jsm>th</jsm>(<js>"Field 2:"</js>),
 * 						<jsm>td</jsm>(<jsm>input</jsm>().name(<js>"field2"</js>).type(<js>"text"</js>)),
 * 						<jsm>td</jsm>(<jk>new</jk> Tooltip(<js>"(?)"</js>, <js>"This is field #2!"</js>, br(), <js>"(e.g. '"</js>, code(<js>"Bar"</js>), <js>"')"</js>))
 * 					)
 * 				)
 * 			)
 * 		);
 * 	}
 * }
 * </p>
 */
public class Tooltip {

	private final String display;
	private final List<Object> content;

   /**
    * Constructor.
    *
    * @param display
    * 	The normal display text.
    * 	This is what gets rendered normally.
    * @param content
    * 	The hover contents.
    * 	Typically a list of strings, but can also include any HTML5 beans as well.
    */
   public Tooltip(String display, Object...content) {
   	this.display = display;
   	this.content = new ArrayList<>(Arrays.asList(content));
   }

   /**
    * The swap method.
    *
    * <p>
    * Converts this bean into a div tag with contents.
    *
    * @param session The bean session.
    * @return The swapped contents of this bean.
    */
   public Div swap(BeanSession session) {
      return div(
      	small(display),
      	span()._class("tooltiptext").children(content)
      )._class("tooltip");
   }
}
