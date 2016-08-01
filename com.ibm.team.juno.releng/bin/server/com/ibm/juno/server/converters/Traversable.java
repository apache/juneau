/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.converters;

import javax.servlet.http.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;

/**
 * Converter for enablement of {@link PojoRest} support on response objects returned by a <code>@RestMethod</code> method.
 * <p>
 * 	When enabled, objects in a POJO tree returned by the REST method can be addressed through additional URL path information.
 *
 * <p class='bcode'>
 * 	<jc>// Resource method on resource "http://localhost:8080/sample/addressBook"</jc>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, converters=Traversable.<jk>class</jk>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		<jk>return new</jk> AddressBook();
 * 	}
 *
 * 	<jc>// Sample usage</jc>
 * 	<jk>public static void</jk> main(String[] args) {
 * 		<jc>// Get the zip code of the 2nd address of the first person in the address book.</jc>
 * 		RestCall r = <jk>new</jk> RestClient().doGet(<js>"http://localhost:8080/sample/addressBook/0/addresses/1/zip"</js>);
 * 		<jk>int</jk> zip = r.getResponse(Integer.<jk>class</jk>);
 * 	}
 * </p>
 * <p>
 * 	See {@link PojoRest} for additional information on addressing elements in a POJO tree using URL notation.
 */
public final class Traversable implements RestConverter {

	@Override /* RestConverter */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object convert(RestRequest req, Object o, ClassMeta cm) throws RestException {
		if (o == null)
			return null;

		if (req.getPathRemainder() != null) {
			try {
				if (cm.getPojoFilter() != null)
					o = cm.getPojoFilter().filter(o);
				PojoRest p = new PojoRest(o, req.getReaderParser());
				o = p.get(req.getPathRemainder());
			} catch (SerializeException e) {
				throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			} catch (PojoRestException e) {
				throw new RestException(e.getStatus(), e);
			}
		}

		return o;
	}
}
