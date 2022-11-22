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
package org.apache.juneau.uon;

/**
 * Identifies the possible values for the {@link UonSerializer.Builder#paramFormat(ParamFormat)} setting.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
public enum ParamFormat {

	/**
	 * Use UON notation for values.
	 *
	 * <p>
	 * String values such as <js>"(foo='bar')"</js> will end up being quoted and escaped to <js>"'(foo=bar~'baz~')'"</js>.
	 *
	 * <p>
	 * Boolean strings (<js>"true"</js>/<js>"false"</js>) and numeric values (<js>"123"</js>) will also end up quoted
	 * (<js>"'true'"</js>, <js>"'false'"</js>, <js>"'123'"</js>.
	 */
	UON,

	/**
	 * Serialize as plain text.
	 *
	 * <p>
	 * Strings will never be quoted or escaped.
	 * <br>Maps and array constructs (<js>"(...)"</js>, <js>"@(...)"</js>) will never be used.
	 *
	 * <p>
	 * Note that this can cause errors during parsing if you're using the URL-encoding parser to parse the results since
	 * UON constructs won't be differentiable.
	 * <br>However, this is not an issue if you're simply creating queries or form posts against 3rd-party interfaces.
	 */
	PLAINTEXT;
}
