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
package org.apache.juneau.examples.rest.petstore;

import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;

@Html(render=UserStatus.UserStatusRender.class)
public enum UserStatus {
	ACTIVE, INACTIVE;
	
	public static class UserStatusRender extends HtmlRender<UserStatus> {
		@Override /* HtmlRender */
		public String getStyle(SerializerSession session, UserStatus value) {
			switch(value) {
				case ACTIVE:  return "background-color:#5cb85c;text-align:center;vertical-align:middle;"; 
				case INACTIVE:  return "background-color:#888;text-align:center;vertical-align:middle;"; 
				default:  return ""; 
			}
		}
	}
}
