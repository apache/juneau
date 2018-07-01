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
package org.apache.juneau.examples.addressbook;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.xml.annotation.*;

/**
 * POJO for creating a new person
 */
@Xml(prefix="per")
@Rdf(prefix="per")
@Bean(typeName="person")
public class CreatePerson {

	// Bean properties
	public String name;

	@Swap(CalendarSwap.DateMedium.class)
	public Calendar birthDate;

	public LinkedList<CreateAddress> addresses = new LinkedList<>();

	/** Bean constructor - Needed for instantiating on server side */
	public CreatePerson() {}

	/** Normal constructor - Needed for instantiating on client side */
	public CreatePerson(String name, Calendar birthDate, CreateAddress...addresses) {
		this.name = name;
		this.birthDate = birthDate;
		this.addresses.addAll(Arrays.asList(addresses));
	}
}

