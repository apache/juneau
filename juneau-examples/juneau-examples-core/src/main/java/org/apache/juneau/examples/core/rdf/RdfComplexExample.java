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
package org.apache.juneau.examples.core.rdf;

import org.apache.juneau.examples.core.pojo.*;
import org.apache.juneau.jena.RdfSerializer;
import org.apache.juneau.jena.RdfXmlAbbrevSerializer;
import org.apache.juneau.jena.RdfXmlSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Sample class which shows the complex usage of RdfXmlSerializer.
 */
public class RdfComplexExample {

    /**
     * Serializing PojoComplex bean into RDF XML format.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Fill some data to a PojoComplex bean
        HashMap<String, List<Pojo>> values = new HashMap<>();
        ArrayList<Pojo> setOne = new ArrayList<>();
        setOne.add(new Pojo("1.1", "name1"));
        setOne.add(new Pojo("1.1", "name2"));
        ArrayList<Pojo> setTwo = new ArrayList<>();
        setTwo.add(new Pojo("1.2", "name1"));
        setTwo.add(new Pojo("1.2", "name2"));
        values.put("setOne", setOne);
        values.put("setTwo", setTwo);
        PojoComplex pojoc = new PojoComplex("pojo", new Pojo("1.0", "name0"), values);

        // this creates an RDF serializer with the default XML structure
        /**Produces
         * <rdf:RDF
         * xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         * xmlns:j="http://www.apache.org/juneau/"
         * xmlns:jp="http://www.apache.org/juneaubp/" >
         * <rdf:Description rdf:nodeID="A0">
         * <jp:name>name1</jp:name>
         * <jp:id>1.1</jp:id>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A1">
         * <jp:innerPojo rdf:nodeID="A2"/>
         * <jp:values rdf:nodeID="A3"/>
         * <jp:id>pojo</jp:id>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A3">
         * <jp:setOne rdf:nodeID="A4"/>
         * <jp:setTwo rdf:nodeID="A5"/>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A6">
         * <jp:name>name2</jp:name>
         * <jp:id>1.1</jp:id>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A2">
         * <jp:name>name0</jp:name>
         * <jp:id>1.0</jp:id>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A7">
         * <jp:name>name2</jp:name>
         * <jp:id>1.2</jp:id>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A4">
         * <rdf:_2 rdf:nodeID="A6"/>
         * <rdf:_1 rdf:nodeID="A0"/>
         * <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq"/>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A5">
         * <rdf:_2 rdf:nodeID="A7"/>
         * <rdf:_1 rdf:nodeID="A8"/>
         * <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq"/>
         * </rdf:Description>
         * <rdf:Description rdf:nodeID="A8">
         * <jp:name>name1</jp:name>
         * <jp:id>1.2</jp:id>
         * </rdf:Description>
         * </rdf:RDF>
         */
        RdfSerializer rdfSerializer = RdfXmlSerializer.DEFAULT;
        // This will show the final output from the bean
        System.out.println(rdfSerializer.serialize(pojoc));

        //Usage of RdfXmlAbbrevSerializer.
        String rdfXml = RdfXmlAbbrevSerializer.DEFAULT.serialize(pojoc);
        System.out.println(rdfXml);
    }
}
