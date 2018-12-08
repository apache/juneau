package org.apache.juneau.examples.core.rdf;

import org.apache.juneau.examples.core.pojo.*;
import org.apache.juneau.jena.RdfSerializer;
import org.apache.juneau.jena.RdfParser;
import org.apache.juneau.jena.RdfXmlSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RdfComplexExample {

    /**
     * TODO
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
        RdfSerializer rdfSerializer = RdfXmlSerializer.DEFAULT;
        // This will show the final output from the bean
        System.out.println(rdfSerializer.serialize(pojoc));
    }
}
