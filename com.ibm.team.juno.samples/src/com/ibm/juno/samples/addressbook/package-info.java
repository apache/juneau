/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
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
package com.ibm.juno.samples.addressbook;
import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.annotation.*;

