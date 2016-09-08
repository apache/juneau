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
package org.apache.juneau.dto.jsonschema;

import java.io.*;
import java.net.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

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
					Schema s = JsonParser.DEFAULT.parse(r, Schema.class);
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
