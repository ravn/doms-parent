package net.sourceforge.ecm.repository;

import junit.framework.TestCase;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.utils.DocumentUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * TODO abr forgot to document this class
 */
public class TestObjectTest
        extends TestCase {

    String object = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<foxml:digitalObject\n" + "\n" + "        PID=\"ThisIsMyPid\" VERSION=\"1.1\"\n" + "\n" + "        xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n" + "\n" + "\n" + "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "\txsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">\n" + "\n" + "\n" + "    <foxml:objectProperties>\n" + "\t\t<foxml:property NAME=\"info:fedora/fedora-system:def/model#state\"\n" + "\t\t\tVALUE=\"A\" />\n" + "\t</foxml:objectProperties>\n" + "\n" + "\n" + "\t<foxml:datastream CONTROL_GROUP=\"X\" ID=\"DC\">\n" + "\t\t<foxml:datastreamVersion ID=\"DC1.0\"\n" + "\t\t                         FORMAT_URI=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + "\t\t                         MIMETYPE=\"text/xml\">\n" + "\t\t\t<foxml:xmlContent>\n" + "\t\t\t\t<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + "\t\t\t\t\txmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n" + "\t\t\t\t\t<dc:title>Sample object</dc:title>\n" + "\t\t\t\t\t<dc:description>This describes the object</dc:description>\n" + "\t\t\t\t\t<dc:creator>Edwin Shin</dc:creator>\n" + "\t\t\t\t</oai_dc:dc>\n" + "\t\t\t</foxml:xmlContent>\n" + "\t\t</foxml:datastreamVersion>\n" + "\t</foxml:datastream>\n" + "\n" + "\n" + "\t<foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\">\n" + "\t\t<foxml:datastreamVersion ID=\"RELS-EXT1.0\"\n" + "\t\t                         FORMAT_URI=\"info:fedora/fedora-system:FedoraRELSExt-1.0\"\n" + "\t\t\t                     MIMETYPE=\"application/rdf+xml\">\n" + "\t\t\t<foxml:xmlContent>\n" + "\t\t\t\t<rdf:RDF\n" + "\t\t\t\t\txmlns:fedora-model=\"info:fedora/fedora-system:def/model#\"\n" + "\t\t\t\t\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" + "\t\t\t\t\t<rdf:Description rdf:about=\"info:fedora/ThisIsMyPid\">\n" + "\t\t\t\t\t\t<fedora-model:hasModel\n" + "\t\t\t\t\t\t\trdf:resource=\"info:fedora/demo:dc2mods.cmodel\" />\n" + "\t\t\t\t\t</rdf:Description>\n" + "\t\t\t\t</rdf:RDF>\n" + "\t\t\t</foxml:xmlContent>\n" + "\t\t</foxml:datastreamVersion>\n" + "\t</foxml:datastream>\n" + "\n" + "\n" + "</foxml:digitalObject>";


    public void testDumpAsDocument()
            throws IOException, SAXException, FedoraIllegalContentException {
        InputStream in = new ByteArrayInputStream(object.getBytes());
        Document doc = DocumentUtils.DOCUMENT_BUILDER.parse(in);

        TestObject testobject = TestObject.parseFromDocument(doc);

        Document it1 = testobject.dumpAsDocument();
        TestObject it2 = TestObject.parseFromDocument(it1);

        assertEquals("invalid pid", it2.getPid(), "ThisIsMyPid");
        assertEquals("wrong state", it2.getState(), "A");
        assertEquals("Changed number of relations",
                     it2.getRelations().size(),
                     1);
        assertEquals("relation name",
                     it2.getRelations().get(0).getRelation(),
                     "info:fedora/fedora-system:def/model#hasModel");
        assertEquals("relation target",
                     Repository.ensureURI(it2.getRelations().get(0).getTo()),
                     "info:fedora/demo:dc2mods.cmodel");

    }


    public void testParseFromDocument() throws IOException, SAXException {

        InputStream in = new ByteArrayInputStream(object.getBytes());
        Document doc = DocumentUtils.DOCUMENT_BUILDER.parse(in);

        TestObject testobject = TestObject.parseFromDocument(doc);

        assertEquals("invalid pid", testobject.getPid(), "ThisIsMyPid");
        assertEquals("wrong state", testobject.getState(), "A");
        assertTrue("datastream missing",
                   testobject.get("DC").trim().startsWith(
                           "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" + "\t\t\t\t\t<dc:title>Sample object</dc:title>\n" + "\t\t\t\t\t<dc:description>This describes the object</dc:description>\n" + "\t\t\t\t\t<dc:creator>Edwin Shin</dc:creator>\n" + "\t\t\t\t</oai_dc:dc>"));


    }
}
