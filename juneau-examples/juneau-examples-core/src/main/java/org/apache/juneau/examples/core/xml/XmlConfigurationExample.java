/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.examples.core.xml;

import java.util.*;

import org.apache.juneau.examples.core.pojo.*;
import org.apache.juneau.xml.*;

/**
 * Xml configuration example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class XmlConfigurationExample {

	/**
	 * Examples on XML Serializers configured using properties
	 * defined in XmlSerializer class.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {

		var aPojo = new Pojo("a", "<pojo>");

		/**
		 * Xml Serializers can be configured using properties defined in XmlSerializer.
		 * Produces
		 * <object>
		 * <name>&lt;pojo&gt;</name>
		 * <id>a</id>
		 * </object>
		 */
		var withWhitespace = XmlSerializer.create().ws().build().serialize(aPojo);
		// the output will be padded with spaces after format characters.
		System.out.println(withWhitespace);

		var values = new HashMap<String,List<Pojo>>();
		var pojoc = new PojoComplex("pojo", new Pojo("1.0", "name0"), values);

		//Produces
		//<object><innerPojo><name>name0</name><id>1.0</id></innerPojo><id>pojo</id></object>
		var mapescaped = XmlSerializer.create().trimEmptyMaps().build().serialize(pojoc);
		// the output will have trimmed Empty maps.
		System.out.println(mapescaped);

		//Produces
		//<object xmlns="http://www.apache.org/2013/Juneau"><name>&lt;pojo&gt;</name><id>a</id></object>
		var nspaceToRoot = XmlSerializer.create().ns().addNamespaceUrisToRoot().build().serialize(aPojo);
		// the output will add default name space to the xml document root.
		System.out.println(nspaceToRoot);

		var nPojo = new Pojo("a", null);

		//Produces
		//<object><id>a</id></object>
		var nullescaped = XmlSerializer.create().build().serialize(nPojo);
		// the output will have trimmed null properties.
		System.out.println(nullescaped);

		//Produces
		//<object xmlns="http://www.pierobon.org/iis/review1.htm.html#one"><name>&lt;pojo&gt;</name><id>a</id></object>
		var dNamsSpace = XmlSerializer.create().enableNamespaces().defaultNamespace(Namespace.create("http://www.pierobon.org" + "/iis/review1.htm.html#one")).addNamespaceUrisToRoot().build()
			.serialize(aPojo);
		// the output will have new default namespace added.
		System.out.println(dNamsSpace);

	}
}