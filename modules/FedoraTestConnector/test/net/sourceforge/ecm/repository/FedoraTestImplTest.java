package net.sourceforge.ecm.repository;

import junit.framework.TestCase;
import net.sourceforge.ecm.exceptions.DatastreamNotFoundException;
import net.sourceforge.ecm.exceptions.FedoraConnectionException;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.exceptions.ObjectNotFoundException;
import net.sourceforge.ecm.utils.Constants;
import net.sourceforge.ecm.utils.DocumentUtils;
import org.w3c.dom.Document;

import java.util.List;

/**
 * TODO abr forgot to document this class
 */
public class FedoraTestImplTest
        extends TestCase {

    FedoraConnector connector;

    TestObject o1, o2;

    TestObject cm1;

    public void setUp() {
        FedoraTestImpl mycon = new FedoraTestImpl();


        o1 = new TestObject("A", "o1");
        o2 = new TestObject("A", "o2");
        cm1 = new TestObject("A", "cm1");

        mycon.addObject(o1);
        mycon.addObject(cm1);


        o1.add(new FedoraConnector.Relation(o1.getPid(),
                                            cm1.getPid(),
                                            Constants.HAS_MODEL));
        o2.add(new FedoraConnector.Relation(o2.getPid(),
                                            cm1.getPid(),
                                            Constants.HAS_MODEL));
        o1.add(new FedoraConnector.Relation(o1.getPid(),
                                            cm1.getPid(),
                                            Constants.TEMPLATE_REL));
        cm1.add(new FedoraConnector.Relation(cm1.getPid(),
                                             Constants.CONTENT_MODEL_3_0,
                                             Constants.HAS_MODEL));


        connector = mycon;

    }

    public void testExist() {
        assertTrue("data object not found", connector.exists(o1.getPid()));
        assertTrue("content model not found", connector.exists(cm1.getPid()));
        assertTrue("remember to normalize pids",
                   connector.exists("info:fedora/" + o1.getPid()));
        assertTrue("found spurious object", !connector.exists("shjkfsd"));

    }

    public void testIsDataObject() {
        assertTrue("data object not found",
                   connector.isDataObject(o1.getPid()));
        assertTrue("content model found as data object",
                   !connector.isDataObject(cm1.getPid()));
        assertTrue("remember to normalize pids",
                   connector.isDataObject("info:fedora/" + o1.getPid()));
        assertTrue("found spurious object", !connector.isDataObject("shjkfsd"));
    }

    public void testIsTemplate() {
        assertTrue("data object not found", connector.isTemplate(o1.getPid()));
        assertTrue("content model not found",
                   !connector.isTemplate(cm1.getPid()));
        assertTrue("remember to normalize pids",
                   connector.isTemplate("info:fedora/" + o1.getPid()));
        assertTrue("found spurious object", !connector.isTemplate("shjkfsd"));
    }

    public void testIsContentModel() {
        assertTrue("data object not found",
                   !connector.isContentModel(o1.getPid()));
        assertTrue("content model not found",
                   connector.isContentModel(cm1.getPid()));
        assertTrue("remember to normalize pids",
                   connector.isContentModel("info:fedora/" + cm1.getPid()));
        assertTrue("found spurious object",
                   !connector.isContentModel("shjkfsd"));
    }


    public void testQuery() {
        fail("Not implemented yet");
    }

    public void testRelations1()
            throws ObjectNotFoundException, FedoraConnectionException {

        connector.addRelation(o1.getPid(),
                              Constants.NAMESPACE_RELATIONS + "TestRelation",
                              o2.getPid());

        List<FedoraConnector.Relation> foundrel = connector.getRelations(o1.getPid(),
                                                                         Constants.NAMESPACE_RELATIONS + "TestRelation");
        assertTrue("Found wrong number of relations", foundrel.size() == 1);
        assertEquals("Found relation wrong target",
                     o2.getPid(),
                     foundrel.get(0).getTo());

        // Add your code here
    }


    public void testRelations2()
            throws ObjectNotFoundException, FedoraConnectionException {

        connector.addRelation(o1.getPid(),
                              Constants.NAMESPACE_RELATIONS + "TestRelation",
                              Repository.ensureURI(o2.getPid()));
        List<FedoraConnector.Relation> foundrel = connector.getRelations(o1.getPid(),
                                                                         Constants.NAMESPACE_RELATIONS + "TestRelation");
        assertTrue("Found wrong number of relations", foundrel.size() == 1);
        assertEquals("Found relation wrong target",
                     o2.getPid(),
                     foundrel.get(0).getTo());

        // Add your code here
    }


    public void testRelations3()
            throws ObjectNotFoundException, FedoraConnectionException {


        List<FedoraConnector.Relation> foundrel = connector.getRelations(o1.getPid());
        assertEquals("Found wrong number of relations", 2, foundrel.size());


        // Add your code here
    }

    public void testObjectXml()
            throws ObjectNotFoundException, FedoraIllegalContentException,
                   FedoraConnectionException {

        connector.getObjectXml(o1.getPid());
        Document o1doc = connector.getObjectXml(Repository.ensureURI(o1.getPid()));
        TestObject rereado1 = TestObject.parseFromDocument(o1doc);
        assertEquals("Same pid", o1.getPid(), rereado1.getPid());
        assertEquals("same state", o1.getState(), rereado1.getState());
        assertEquals("relation preserved",
                     o1.getRelations().get(0),
                     rereado1.getRelations().get(0));


    }


    public void testDatastreams()
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, DatastreamNotFoundException {
        String defaultds = "<oai_dc:dc " + "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" " + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" + ">\n" + "<dc:title>Sample object</dc:title>\n" + "<dc:description>This describes the object</dc:description>\n" + "<dc:creator>Edwin Shin</dc:creator>\n" + "</oai_dc:dc>";


        o1.put("DC", defaultds);


        List<String> datastreams = connector.listDatastreams(o1.getPid());
        assertEquals("wrong number of datastreams", 1, datastreams.size());

        Document datastream = connector.getDatastream(o1.getPid(),
                                                      datastreams.get(0));
        String ds = DocumentUtils.documentToString(datastream);
        ds = ds.replace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
                "").trim();
        ds = ds.replaceAll("\\s+", " ");
        defaultds = defaultds.replaceAll("\\s+", " ").trim();
        defaultds = defaultds.replaceAll("\\s+", " ").trim();
        assertEquals("Datastream changed", defaultds, ds);

    }


    public void testGetContentModels()
            throws ObjectNotFoundException, FedoraConnectionException {

        PidList cms = connector.getContentModels(o1.getPid());
        assertEquals("Wrong number of content models", 1, cms.size());
        assertEquals("Wrong content model", cm1.getPid(), cms.get(0));
        // Add your code here
    }

    public void testGetInheritedContentModels() {
        fail("Not implemented yet");
    }

    public void testGetInheritingContentModels() {
        fail("Not implemented yet");
    }

}
