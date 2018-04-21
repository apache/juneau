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

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.html5.Img;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;

@Bean(typeName="Species", fluentSetters=true)
@Html(render=Species.SpeciesRender.class)
public class Species {
	private long id;
	private String name;
	
	public long getId() {
		return id;
	}
	
	public Species id(long id) {
		this.id = id;
		return this;
	}
	
	public String getName() {
		return name;
	}
	
	public Species name(String name) {
		this.name = name;
		return this;
	}
	
	@Example
	public static Species example() {
		return new Species()
			.id(123)
			.name("Dog");
	}

	public static class SpeciesRender extends HtmlRender<Species> {
		@Override
		public Object getContent(SerializerSession session, Species value) {
			return new Img().src("servlet:/htdocs/"+value.getName().toLowerCase()+".png");
		}
		@Override
		public String getStyle(SerializerSession session, Species value) {
			return "background-color:#FDF2E9";
		}
	}
	
	@Override /* Object */
	public String toString() {
		return name;
	}
}
