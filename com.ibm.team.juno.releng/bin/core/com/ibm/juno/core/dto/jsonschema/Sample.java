/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.jsonschema;

import java.io.*;
import java.net.*;

import com.ibm.juno.core.json.*;
import com.ibm.juno.core.utils.*;

@SuppressWarnings("serial")
class Sample {

	public static void main(String[] args) {

		// Create a SchemaMap for looking up schemas.
		SchemaMap schemaMap = new SchemaMap() {

			@Override /* SchemaMap */
			public Schema load(URI uri) {
				Reader r = null;
				try {
					r = new InputStreamReader(uri.toURL().openStream(), IOUtils.UTF8);
					Schema s = JsonParser.DEFAULT.parse(r, -1, Schema.class);
					return s;
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					if (r != null) {
						try {
							r.close();
						} catch (IOException e) {
						}
					}
				}
			}
		};

		// Get schema from the schema map.
		Schema purchaseOrderSchema = schemaMap.get("http://www.ibm.com/purchase-order/PurchaseOrder#");

		JsonType streetType = purchaseOrderSchema
			.getProperty("address",true)                         // Get "address" property, resolved to Address schema.
			.getProperty("street")                               // Get "street" property.
			.getTypeAsJsonType();                                // Get data type.
		System.err.println("streetType=" + streetType);         // Prints "streetType=string"

		JsonType productIdType = purchaseOrderSchema
			.getProperty("product")                              // Get "product" property
			.getItemsAsSchemaArray()                             // Get "items".
			.get(0)                                              // Get first entry.
			.resolve()                                           // Resolve to Product schema.
			.getProperty("productId")                            // Get "productId" property.
			.getTypeAsJsonType();                                // Get data type.
		System.err.println("productIdType=" + productIdType);   // Prints "productIdType=number"
	}
}
