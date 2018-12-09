package org.apache.juneau.examples.core.xml;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.SerializeException;
import org.apache.juneau.xml.XmlParser;
import org.apache.juneau.xml.XmlSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Sample class which shows the simple usage of XmlSerializer.
 */
public class XmlSimpleExample {
    /**
     * Serializing SimplePojo bean into human readable XML
     * and Deserialize back to Pojo instance type.
     * @param args
     * @throws SerializeException
     * @throws ParseException
     */
    public static void main(String[] args) throws SerializeException, ParseException {

        // Fill some data to a Pojo bean
        Pojo pojo = new Pojo("id","name");

        // Serialize to human readable XML and print
        String serial = XmlSerializer.DEFAULT_SQ_READABLE.serialize(pojo);
        System.out.println(serial);

        // Deserialize back to Pojo instance
        Pojo obj = XmlParser.DEFAULT.parse(serial, Pojo.class);

        assert obj.getId().equals(pojo.getId());
        assert obj.getName().equals(pojo.getName());

        // The object above can be parsed thanks to the @BeanConstructor annotation on PojoComplex
        // Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.

    }
}
