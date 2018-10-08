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
package org.apache.juneau.examples.rest.petstore.dto;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.Html;
import org.apache.juneau.serializer.*;

@Html(render=Species.SpeciesRender.class)
public enum Species {

	BIRD, CAT, DOG, FISH, MOUSE, RABBIT, SNAKE;

	public static class SpeciesRender extends HtmlRender<Species> {
		@Override
		public Object getContent(SerializerSession session, Species value) {
			return new Img().src("servlet:/htdocs/"+value.name().toLowerCase()+".png");
		}
		@Override
		public String getStyle(SerializerSession session, Species value) {
			return "background-color:#FDF2E9";
		}
	}
}
