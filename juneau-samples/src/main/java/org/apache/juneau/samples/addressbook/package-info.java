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
// XML namespaces used in this package
@XmlSchema(
	prefix="ab",
	xmlNs={
		@XmlNs(prefix="ab", namespaceURI="http://www.ibm.com/addressBook/"),
		@XmlNs(prefix="per", namespaceURI="http://www.ibm.com/person/"),
		@XmlNs(prefix="addr", namespaceURI="http://www.ibm.com/address/"),
		@XmlNs(prefix="mail", namespaceURI="http://www.ibm.com/mail/")
	}
)
@RdfSchema(
	prefix="ab",
	rdfNs={
		@RdfNs(prefix="ab", namespaceURI="http://www.ibm.com/addressBook/"),
		@RdfNs(prefix="per", namespaceURI="http://www.ibm.com/person/"),
		@RdfNs(prefix="addr", namespaceURI="http://www.ibm.com/address/"),
		@RdfNs(prefix="mail", namespaceURI="http://www.ibm.com/mail/")
	}
)
package org.apache.juneau.samples.addressbook;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.annotation.*;

