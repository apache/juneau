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
package org.apache.juneau.petstore.dto;

import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Enum of all possible pet statuses.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Html(render=PetStatus.PetStatusRender.class)
@SuppressWarnings("javadoc")
public enum PetStatus {
	AVAILABLE, PENDING, SOLD, UNKNOWN;

	/**
	 * Used to control how this enum is rendered in HTML view.
	 */
	public static class PetStatusRender extends HtmlRender<PetStatus> {
		@Override /* HtmlRender */
		public String getStyle(SerializerSession session, PetStatus value) {
			switch(value) {
				case AVAILABLE:  return "background-color:#5cb85c;text-align:center;vertical-align:middle;";
				case PENDING:  return "background-color:#f0ad4e;text-align:center;vertical-align:middle;";
				case SOLD:  return "background-color:#888;text-align:center;vertical-align:middle;";
				default:  return "background-color:#777;text-align:center;vertical-align:middle;";
			}
		}
	}
}
