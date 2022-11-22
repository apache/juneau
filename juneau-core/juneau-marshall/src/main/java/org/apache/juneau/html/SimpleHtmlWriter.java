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

import java.io.*;

/**
 * Utility class for creating custom HTML.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjson'>
 * 	String <jv>table</jv> = <jk>new</jk> SimpleHtmlWriter().sTag(<js>"table"</js>).sTag(<js>"tr"</js>).sTag(<js>"td"</js>)
 * 	.append(<js>"hello"</js>).eTag(<js>"td"</js>).eTag(<js>"tr"</js>).eTag(<js>"table"</js>).toString();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class SimpleHtmlWriter extends HtmlWriter {

	/**
	 * Constructor.
	 */
	public SimpleHtmlWriter() {
		super(new StringWriter(), true, 100, false, '\'', null);
	}

	@Override /* Object */
	public String toString() {
		return out.toString();
	}
}
