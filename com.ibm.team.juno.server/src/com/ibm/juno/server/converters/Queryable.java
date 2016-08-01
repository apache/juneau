/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.converters;

import static javax.servlet.http.HttpServletResponse.*;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;

/**
 * Converter for enablement of {@link PojoQuery} support on response objects returned by a <code>@RestMethod</code> method.
 * <p>
 * 	When enabled, objects in a POJO tree can be filtered using the functionality described in the {@link PojoQuery}
 * 	class.
 * <p>
 * 	The following HTTP request parameters are available for tabular data (e.g. {@code Collections} of {@code Maps}, arrays of beans, etc...):
 * <ul>
 * 	<li><b>&amp;q=<i>JSON-object</i></b> - Query parameter.  Only return rows that match the specified search string. <br>
 * 			The JSON object keys are column names, and the values are search parameter strings.<br>
 * 			Example:  <code>&amp;s=(name=Bill*,birthDate=&gt;2000)</code><br>
 * 			<br>
 * 	<li><b>&amp;v=<i>JSON-array or comma-delimited list</i></b> - View parameter.  Only return the specified columns.<br>
 * 			Example:  <code>&amp;v=(name,birthDate)</code><br>
 * 			<br>
 * 	<li><b>&amp;s=<i>JSON-object</i></b> - Sort parameter.  Sort the results by the specified columns.<br>
 * 			The JSON object keys are the column names, and the values are either {@code 'A'} for ascending or {@code 'D'} for descending.
 * 			Example:  <code>&amp;s=(name=A,birthDate=D)</code><br>
 * 			<br>
 * 	<li><b>&amp;i=<i>true/false</i></b> - Case-insensitive parameter.  Specify <jk>true</jk> for case-insensitive matching on the {@code &amp;q} parameter. <br>
 * 			<br>
 * 	<li><b>&amp;p=<i>number</i></b> - Position parameter.  Only return rows starting at the specified index position (zero-indexed).  Default is {@code 0}.<br>
 * 			<br>
 * 	<li><b>&amp;q=<i>number</i></b> - Limit parameter.  Only return the specified number of rows. Default is {@code 0} (meaning return all rows).
 * </ul>
 *
 * <p>
 * 	The <b>&amp;v</b> parameter can also be used on {@code Maps} and beans.
 *
 * <p>
 * 	See {@link PojoQuery} for additional information on filtering POJO models.
 */
public final class Queryable implements RestConverter {

	@Override /* RestConverter */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object convert(RestRequest req, Object o, ClassMeta cm) {
		if (o == null)
			return null;

		try {

			// If no actual filtering parameters have been passed in, and there is no map augmenter specified,
			// then just pass the original object back.
			if (req.hasAnyQueryParameters("q","v","s","g","i","p","l")) {
				BeanContext bc = req.getBeanContext();

				if (cm.getPojoFilter() != null)
					o = cm.getPojoFilter().filter(o);

				PojoQuery f = new PojoQuery(o, bc);

				if (o instanceof Collection || o.getClass().isArray()) {
					ObjectMap query = req.getQueryParameter("q", ObjectMap.class);
					ClassMeta<List<String>> cm1 = bc.getCollectionClassMeta(List.class, String.class);
					List<String> view = req.getQueryParameter("v", cm1);
					ClassMeta<List<Object>> cm2 = bc.getCollectionClassMeta(List.class, String.class);
					List sort = req.getQueryParameter("s", cm2);
					boolean ignoreCase = req.getQueryParameter("i", Boolean.class, false);
					int pos = req.getQueryParameter("p", Integer.class, 0);
					int limit = req.getQueryParameter("l", Integer.class, 0);
					o = f.filterCollection(query, view, sort, pos, limit, ignoreCase);

				} else {
					ClassMeta<List<String>> cm2 = bc.getCollectionClassMeta(List.class, String.class);
					List<String> view = req.getQueryParameter("v", cm2);
					o = f.filterMap(view);
				}
			}
			return o;
		} catch (SerializeException e) {
			throw new RestException(SC_BAD_REQUEST, e);
		} catch (ParseException e) {
			throw new RestException(SC_BAD_REQUEST, e);
		}
	}
}
