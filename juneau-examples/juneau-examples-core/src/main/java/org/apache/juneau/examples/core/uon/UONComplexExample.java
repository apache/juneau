package org.apache.juneau.examples.core.uon;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.examples.core.pojo.PojoComplex;
import org.apache.juneau.uon.UonParser;
import org.apache.juneau.uon.UonSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UONComplexExample {
    /**
     * Serializing PojoComplex bean into UON format.
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
         * (innerPojo=(name=name0,id='1.0'),
         * values=(setOne=@((name=name1,id='1.1'),(name=name2,id='1.1')),
         * setTwo=@((name=name1,id='1.2'),(name=name2,id='1.2'))),id=pojo)
         */
        UonSerializer uonSerializer = UonSerializer.DEFAULT;
        // This will show the final output from the bean
        System.out.println(uonSerializer.serialize(pojoc));

        PojoComplex obj = UonParser.DEFAULT.parse(uonSerializer.serialize(pojoc), PojoComplex.class);

        assert obj.getId().equals(pojoc.getId());

    }
}
