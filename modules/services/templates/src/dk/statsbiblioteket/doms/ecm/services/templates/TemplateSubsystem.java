package dk.statsbiblioteket.doms.ecm.services.templates;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.*;
import dk.statsbiblioteket.doms.ecm.repository.PidList;
import dk.statsbiblioteket.doms.ecm.repository.PidGenerator;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.utils.Constants;
import dk.statsbiblioteket.doms.ecm.repository.utils.XpathUtils;
import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;


/**
 * This class deals with the template subsystem.
 */
public class TemplateSubsystem {

    private static final Log LOG = LogFactory.getLog(TemplateSubsystem.class);
    private static final String FOXML_DIGITAL_OBJECT_PID = "/foxml:digitalObject/@PID";
    private static final String RELSEXT_ABOUT = "/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/"
                                                + "foxml:datastreamVersion[position()=last()]/"
                                                + "foxml:xmlContent/rdf:RDF/"
                                                + "rdf:Description/@rdf:about";
    private static final String DCIDENTIFIER = "/foxml:digitalObject/foxml:datastream[@ID='DC']/"
                                               + "foxml:datastreamVersion[position()=last()]/"
                                               + "foxml:xmlContent/oai_dc:dc/dc:identifier";
    private static final String OAIDC = "/foxml:digitalObject/foxml:datastream[@ID='DC']/"
                                        + "foxml:datastreamVersion[position()=last()]/"
                                        + "foxml:xmlContent/oai_dc:dc";
    private static final String ISTEMPLATEFOR = "/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/"
                                                + "foxml:datastreamVersion[position()=last()]/"
                                                + "foxml:xmlContent/rdf:RDF/"
                                                + "rdf:Description/doms:isTemplateFor";
    private static final String DATASTREAM_AUDIT = "/foxml:digitalObject/foxml:datastream[@ID='AUDIT']";
    private static final String DATASTREAM_NEWEST = "/foxml:digitalObject/foxml:datastream/"
                                                    + "foxml:datastreamVersion[position()=last()]";
    private static final String DATASTREAM_CREATED = "/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion";
    private static final String OBJECTPROPERTY_CREATED = "/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#createdDate']";
    private static final String OBJECTPROPERTIES_LSTMODIFIED = "/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/view#lastModifiedDate']";


    /**
     * Mark the objpid object as a template for the cmpid object
     * @param objpid the object to mark
     * @param cmpid the content model to make objpid a template for
     * @param fedoraConnector
     * @throws ObjectNotFoundException if either of the objects do not exist
     * @throws FedoraConnectionException if anything went wrong in the communication
     * @throws ObjectIsWrongTypeException if the object is not a data object or the content model is not a content model
     */
    public void markObjectAsTemplate(
            String objpid,
            String cmpid, FedoraConnector fedoraConnector)
            throws ObjectNotFoundException, FedoraConnectionException,
                   ObjectIsWrongTypeException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException {
        LOG.trace("Entering markObjectAsTemplate with params: " + objpid + " and "+cmpid );
        //Working


        if (!fedoraConnector.isContentModel(cmpid)){
            throw new ObjectIsWrongTypeException("The content model '"+cmpid+
                                                 "' is not a content model");
        }
        if (!fedoraConnector.isDataObject(objpid)){
            throw new ObjectIsWrongTypeException("The data object '"+objpid+
                                                 "' is not a data object");
        }


        boolean added = fedoraConnector.addRelation(objpid, Constants.TEMPLATE_REL, cmpid);
        LOG.info("Marked object '"+objpid+"' as template for '"+cmpid+"'");
        if (!added){
            //The object is already a template. Note this in the log, and do no more
            LOG.info("Object '"+objpid+"' was already a template for '"+cmpid+"' so no change was performed");
        }
    }



    public PidList findTemplatesFor(String cmpid, FedoraConnector fedoraConnector)
            throws FedoraConnectionException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException,
                   ObjectNotFoundException,
                   ObjectIsWrongTypeException {
        //Working
        LOG.trace("Entering findTemplatesFor with param '"+cmpid+"'");

        List<String> childcms
                = fedoraConnector.getInheritingContentModels(cmpid);

        String contentModel
                = "<"+
                  FedoraUtil.ensureURI(cmpid)+
                  ">\n";

        String query = "select $object\n" +
                       "from <#ri>\n" +
                       "where\n" +
                       " $object <" + Constants.TEMPLATE_REL + "> " +
                       contentModel;

        for (String childcm: childcms){
            String cm = "<" +
                        FedoraUtil.ensureURI(childcm) +
                        ">\n";

            query = query +
                    "or $object <" +
                    Constants.TEMPLATE_REL +
                    "> " + cm;
        }

        return fedoraConnector.query(query);

    }




    public String cloneTemplate(String templatepid, FedoraConnector fedoraConnector, PidGenerator pidGenerator)
            throws FedoraIllegalContentException,
                   FedoraConnectionException, PIDGeneratorException,
                   ObjectNotFoundException,
                   ObjectIsWrongTypeException, InvalidCredentialsException {

        //working
        templatepid = FedoraUtil.ensurePID(templatepid);
        LOG.trace("Entering cloneTemplate with param '" + templatepid + "'");


        if (!fedoraConnector.isTemplate(templatepid)){
            throw new ObjectIsWrongTypeException("The pid (" + templatepid +
                                                 ") is not a pid of a template");
        }

        // Get the document
        Document document = fedoraConnector.getObjectXml(templatepid);


        String newPid = pidGenerator.generateNextAvailablePID("clone_");
        LOG.debug("Generated new pid '" + newPid + "'");

        try {
            removeAudit(document);
            LOG.trace("Audit removed");
            removeDatastreamVersions(document);
            LOG.trace("Datastreamsversions removed");

            // Replace PID
            replacePid(document, templatepid, newPid);
            LOG.trace("Pids replaced");


            removeDCidentifier(document);
            LOG.trace("DC identifier removed");

            removeXSI_DC(document);
            LOG.trace("XSI stuff removed from DC");


            removeCreated(document);
            LOG.trace("CREATED removed");

            removeLastModified(document);
            LOG.trace("Last Modified removed");

            removeTemplateRelation(document);
            LOG.trace("Template relation removed");
        } catch (XPathExpressionException e){
            throw new FedoraIllegalContentException(
                    "Template object did not contain the correct structure",e);
        }

        //reingest the object
        return fedoraConnector.ingestDocument(
                document,
                "Cloned from template '" +
                templatepid +
                "' by user '" +
                fedoraConnector.getUser() +
                "'");

    }

    private void removeXSI_DC(Document document) throws
                                                 XPathExpressionException {
/*        removeExpathList(document, XSI_TAGS1);*/
        removeAttribute(document,OAIDC,"xsi:schemaLocation");
    }

    /** Private helper method for cloneTemplate. In a document, replaces the
     * mention of oldpid with newpid
     * @param doc the document to work on
     * @param oldpid the old pid
     * @param newpid the new pid
     * @throws FedoraIllegalContentException If there is a problem understanding
     * the document
     * @throws javax.xml.xpath.XPathExpressionException if there was 
     */
    private void replacePid(Document doc, String oldpid, String newpid)
            throws FedoraIllegalContentException, XPathExpressionException {

        LOG.trace("Entering replacepid");
        replateAttribute(doc, FOXML_DIGITAL_OBJECT_PID,
                         FedoraUtil.ensurePID(newpid));


        replateAttribute(doc, RELSEXT_ABOUT,FedoraUtil.ensureURI(newpid));


    }

    /**
     * Utility method for removing all nodes from a query. Does not work
     * for attributes
     * @param doc the object
     * @param query the adress of the nodes
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeExpathList(Document doc, String query)
            throws XPathExpressionException {
        NodeList nodes = XpathUtils.
                xpathQuery(doc,
                           query);
        if (nodes != null){
            for (int i=0;i<nodes.getLength();i++){
                Node node = nodes.item(i);
                node.getParentNode().removeChild(node);

            }
        }
    }

    /**
     * Utility method for changing the value of an attribute
     * @param doc the object
     * @param query the location of the Attribute
     * @param value the new value
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void replateAttribute(Document doc, String query, String value)
            throws XPathExpressionException {
        NodeList nodes = XpathUtils.
                xpathQuery(doc,
                           query);
        for (int i=0;i<nodes.getLength();i++){
            Node node = nodes.item(i);
            node.setNodeValue(value);
        }

    }

    /**
     * Utility method for removing an attribute
     * @param doc the object
     * @param query the adress of the node element
     * @param attribute the name of the attribute
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeAttribute(Document doc, String query, String attribute)
            throws XPathExpressionException {
        NodeList nodes;

        nodes = XpathUtils.xpathQuery(
                doc,
                query);

        for (int i=0;i<nodes.getLength();i++){
            Node node = nodes.item(i);

            NamedNodeMap attrs = node.getAttributes();

            if (attrs.getNamedItem(attribute) != null){
                attrs.removeNamedItem(attribute);
            }

        }
    }

    /**
     * Removes the DC identifier from the DC datastream
     * @param doc the object
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeDCidentifier(Document doc)
            throws  XPathExpressionException {
        //Then remove the pid in dc identifier
        removeExpathList(doc, DCIDENTIFIER);
    }



    /**
     * Removes all template relations
     * @param doc the object
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeTemplateRelation(Document doc) throws
                                                      XPathExpressionException {
        // Remove template relation

        //TODO Constant for template relation
        removeExpathList(doc, ISTEMPLATEFOR);


    }

    /**
     * Removes the AUDIT datastream
     * @param doc the object
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeAudit(Document doc) throws
                                           XPathExpressionException {

        removeExpathList(doc, DATASTREAM_AUDIT);

    }

    /**
     * Removes all datastream versions, except the newest
     * @param doc the object
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeDatastreamVersions(Document doc) throws
                                                        XPathExpressionException {
        NodeList relationNodes;

        relationNodes = XpathUtils.xpathQuery(
                doc, DATASTREAM_NEWEST);

        Node node = relationNodes.item(0);
        Node datastreamnode = node.getParentNode();

        //Remove all of the datastream node children
        while (datastreamnode.getFirstChild() != null) {
            datastreamnode.removeChild(
                    datastreamnode.getFirstChild());
        }

        datastreamnode.appendChild(node);


    }

    /**
     * Removes the CREATED attribute on datastreamVersion and the createdDate objectProperty
     * @param doc the object
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeCreated(Document doc) throws XPathExpressionException {
        LOG.trace("Entering removeCreated");
        removeAttribute(doc, DATASTREAM_CREATED,"CREATED");

        removeExpathList(doc, OBJECTPROPERTY_CREATED);


    }

    /**
     * Removes the lastModifiedDate objectDate
     * @param doc the object
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeLastModified(Document doc) throws
                                                  XPathExpressionException {

        removeExpathList(doc, OBJECTPROPERTIES_LSTMODIFIED);

    }
}
