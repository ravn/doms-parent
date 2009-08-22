package net.sourceforge.ecm.repository;

import net.sourceforge.ecm.utils.Constants;
import net.sourceforge.ecm.utils.DocumentUtils;
import net.sourceforge.ecm.utils.XpathUtils;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.transform.TransformerException;
import java.util.*;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * TODO abr forgot to document this class
 */
public class TestObject {

    private Map<String,String> datastreams;

    private String state, pid;

    public TestObject(String state, String pid) {
        this.state = state;
        this.pid = pid;
        datastreams = new HashMap<String, String>();
        relations = new ArrayList<FedoraConnector.Relation>();
    }


    private List<FedoraConnector.Relation> relations;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPid() {
        return pid;
    }

    public boolean add(FedoraConnector.Relation relation) {
        return relations.add(relation);
    }

    public boolean remove(Object o) {
        return relations.remove(o);
    }

    public List<FedoraConnector.Relation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    public String get(Object key) {
        return datastreams.get(key);
    }

    public String put(String key, String value) {
        return datastreams.put(key, value);
    }

    public Map<String, String> getDatastreams() {
        return Collections.unmodifiableMap(datastreams);
    }

    private String objectheader  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<foxml:digitalObject\n" +
            "\n" +
            "        PID=\"REPLACEPIDHERE\" VERSION=\"1.1\"\n" +
            "\n" +
            "        xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n" +
            "\n" +
            "\n" +
            "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "\txsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">\n" +
            "\n" +
            "\n" +
            "    <foxml:objectProperties>\n" +
            "\t\t<foxml:property NAME=\"info:fedora/fedora-system:def/model#state\"\n" +
            "\t\t\tVALUE=\"REPLACESTATEHERE\" />\n" +
            "\t</foxml:objectProperties>";

    private String datastream = "<foxml:datastream CONTROL_GROUP=\"X\" ID=\"REPLACEDSIDHERE\">\n" +
            "\t\t<foxml:datastreamVersion ID=\"REPLACEDSIDHERE1.0\"\n" +
            "\t\t                         MIMETYPE=\"text/xml\">\n" +
            "\t\t\t<foxml:xmlContent>\n";

    private String relsextdatastream = "\t<foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\">\n" +
            "\t\t<foxml:datastreamVersion ID=\"RELS-EXT1.0\"\n" +
            "\t\t                         FORMAT_URI=\"info:fedora/fedora-system:FedoraRELSExt-1.0\"\n" +
            "\t\t\t                     MIMETYPE=\"application/rdf+xml\">\n" +
            "\t\t\t<foxml:xmlContent>\n" +
            "\t\t\t\t<rdf:RDF\n" +
            "\n" +
            "\t\t\t\t\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
            "\t\t\t\t\t<rdf:Description rdf:about=\"info:fedora/REPLACEPIDHERE\">";

    private String relsextend = "\t\t\t\t\t</rdf:Description>\n" +
            "\t\t\t\t</rdf:RDF>\n" +
            "\t\t\t</foxml:xmlContent>\n" +
            "\t\t</foxml:datastreamVersion>\n" +
            "\t</foxml:datastream>";


    private String datastreamend =
            "\t\t\t</foxml:xmlContent>\n" +
                    "\t\t</foxml:datastreamVersion>\n" +
                    "\t</foxml:datastream>";

    private String objectend = "\n" +
            "</foxml:digitalObject>";

    public Document dumpAsDocument() throws FedoraIllegalContentException {

        StringWriter st = new StringWriter();
        st.append(objectheader.replace("REPLACEPIDHERE", pid).replace("REPLACESTATEHERE", state));

        //And now datastreams
        for (String dsid : datastreams.keySet()) {
            if (dsid.equals("RELS-EXT")) {
                continue;
            }
            st.append(datastream.replace("REPLACEDSIDHERE", dsid));
            st.append(datastreams.get(dsid));
            st.append(datastreamend);
        }

        //and last relations
        st.append(relsextdatastream.replace("REPLACEPIDHERE", pid));
        for (FedoraConnector.Relation rel : relations) {
            String relationname = rel.getRelation();
            int index1 = relationname.lastIndexOf(":");
            int index2 = relationname.lastIndexOf("#");
            int index3 = relationname.lastIndexOf("/");
            int li = Math.max(index3, Math.max(index1, index2));
            String ns = relationname.substring(0, li+1);
            String ln = relationname.substring(li+1);
            String relation = "<" + ln + " xmlns=\"" + ns + "\" rdf:resource=\"" + Repository.ensureURI(rel.getTo()) + "\" />\n";
            st.append(relation);
        }
        st.append(relsextend);

        st.append(objectend);

        Document doc = null;
        try {
            doc = DocumentUtils.DOCUMENT_BUILDER.parse(new ByteArrayInputStream(st.toString().getBytes()));
        } catch (SAXException e) {
            throw new FedoraIllegalContentException(e);
        } catch (IOException e) {
            throw new Error(e);
        }

        return doc;

    }

    public static TestObject parseFromDocument(Document source){
        //TODO
        //schema validate

        //xpath extract properties
        Element docelement = source.getDocumentElement();
        String state;

        try {
            NodeList states = XpathUtils.xpathQuery(docelement,
                    "/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state']/@VALUE");
            if (states.getLength() == 1){
                state = states.item(0).getNodeValue();

            } else {
                state = "A";
            }
        } catch (XPathExpressionException e) {
            return null;
        }

        String pid = "newpid";
        try {
            NodeList pids = XpathUtils.xpathQuery(docelement,
                    "/foxml:digitalObject/@PID");
            if (pids.getLength() == 1){
                pid = pids.item(0).getNodeValue();

            } else {
                pid = "newpid";
            }
        } catch (XPathExpressionException e) {
            return null;
        }

        

        TestObject that = new TestObject(state,pid);

        try {
            NodeList dsids = XpathUtils.xpathQuery(docelement,
                    "/foxml:digitalObject/foxml:datastream");
            for (int i=0;i<dsids.getLength();i++){//For each datastream
                Node ds = dsids.item(i);
                String dsid = ds.getAttributes().getNamedItem("ID").getNodeValue();
                if (dsid.equals("RELS-EXT")){
                    NodeList rels = XpathUtils.xpathQuery(
                            ds,
                            "foxml:datastreamVersion[last()]/foxml:xmlContent/rdf:RDF/rdf:Description/*");
                    for (int j=0;j<rels.getLength();j++){
                        Node rel = rels.item(j);
                        String namespace = rel.getNamespaceURI();
                        String localname = rel.getLocalName();
                        Node about = rel.getAttributes().getNamedItemNS(Constants.NAMESPACE_RDF, "resource");
                        String to = about.getNodeValue();
                        that.add(new FedoraConnector.Relation(pid,Repository.ensurePID(to),namespace+localname));
                    }
                }
                NodeList contents = XpathUtils.xpathQuery(ds,
                        "foxml:datastreamVersion[last()]/foxml:xmlContent");
                if (contents.getLength() != 1){
                    //several root elements, WTF TODO
                } else{
                    Document doc = DocumentUtils.DOCUMENT_BUILDER.newDocument();
                    Node contentsnode = contents.item(0).getFirstChild();
                    while (contentsnode.getNodeType() != Node.ELEMENT_NODE){
                        contentsnode = contentsnode.getNextSibling();
                    }
                    Node importedNode = doc.importNode(contentsnode, true);
                    doc.appendChild(importedNode);
                    String content = DocumentUtils.documentToString(doc);
                    content = content.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>","");
                    that.put(dsid,content);//Put the datastream in the new object
                }
            }
        } catch (XPathExpressionException e) {
            return null;
        } catch (TransformerException e) {
            //TODO FIX THIS!!!
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        return that;


    }
}
