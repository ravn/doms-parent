package dk.statsbiblioteket.doms.ecm.services.validator;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.DatastreamNotFoundException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectIsWrongTypeException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException;
import dk.statsbiblioteket.doms.ecm.repository.utils.Constants;
import dk.statsbiblioteket.doms.ecm.repository.utils.XpathUtils;
import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Utility methods for working with content models.
 */
public class ContentModelUtils {

    private static final Log LOG = LogFactory.getLog(ContentModelUtils.class);
    private static final String DSCOMPOSITE_MODEL_TYPE_MODEL = "ds:dsCompositeModel/ds:dsTypeModel";
    private static final String DS_FORM_MIME = "ds:form/@MIME";
    private static final String DS_FORM_FORMAT_URI = "ds:form/@FORMAT_URI";
    private static final String DS_EXTENSIONS_NAME_SCHEMA_SCHEMA_SCHEMA_TYPE_XSD = "ds:extensions[@name='SCHEMA']/schema:schema[@type='xsd']";

    /**
     * Utility class, do not instantiate.
     */
    private ContentModelUtils() {
    }

    /**
     * Make a single content model into a compound content model.
     * @param cmpid the content model to wrap thus
     * @return the compound content model
     * @throws FedoraIllegalContentException
     * @throws FedoraConnectionException
     * @throws ObjectNotFoundException
     */
    public static CompoundContentModel getAsCompoundContentModel(String cmpid, FedoraConnector fedoraConnector)
            throws FedoraIllegalContentException, FedoraConnectionException,
                   ObjectNotFoundException, ObjectIsWrongTypeException {
        LOG.trace("Entering");
        CompoundContentModel model = new CompoundContentModel();


        List<String> inherited = fedoraConnector.getInheritedContentModels(cmpid);

        for (String oldcm:inherited){
            updateModel(oldcm,model,fedoraConnector);
        }

        updateModel(cmpid, model,fedoraConnector);
        return model;
    }


    /**
     * Get the compound content model for an object. This method will generate
     * an abstract representation of the Compund Content Model for an object,
     * including datastreams (with SCHEMA extensions), ontology, and
     * view information.
     *
     *
     * @param pid The pid for an object
     * @param fedoraConnector
     * @return The compound content model for the object or content model.
     * @throws FedoraConnectionException     on trouble communicating with
     *                                       Fedora.
     * @throws FedoraIllegalContentException if content model contains illegal
     *                                       information (i.e. non-existing PIDs
     *                                       referred, illegal XML, etc.)
     */
    public static CompoundContentModel getCompoundContentModel(String pid, FedoraConnector fedoraConnector)
            throws FedoraIllegalContentException, FedoraConnectionException,
                   ObjectNotFoundException, ObjectIsWrongTypeException {

        LOG.trace("Entering");
        CompoundContentModel model = new CompoundContentModel();


        //Get pids of all content models
        List<String> pids = fedoraConnector.getContentModels(pid);

        LOG.trace("Got base list of pids");

        // Update content model with info from all models.
        for (String p: pids){
            updateModel(p,model,fedoraConnector);
        }

        return model;
    }

    /**
     * Update the compound model with the information in the content model p.
     * No inheritance rules are followed here.
     * @param p pid of a content model
     * @param model the compound model to update
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     */
    private static void updateModel(String p,
                                    CompoundContentModel model,
                                    FedoraConnector fedoraConnector
    )
            throws FedoraConnectionException,
                   FedoraIllegalContentException, ObjectNotFoundException {

        if (model.getPids().contains(p)){
            return;
        } else{
            List<String> pids = model.getPids();
            pids.add(p);
            model.setPids(pids);
        }

        List<String> datastreams;
        datastreams = fedoraConnector.listDatastreams(p);

        for (String def : datastreams) {
            if (def.equals(
                    Constants.ONTOLOGY_DATASTREAM)) {
                // Merge datastream ontology into compound model ontology
                Document newOntology = null;

                try {
                    newOntology = fedoraConnector.getDatastream(p,def);
                } catch (DatastreamNotFoundException e) {
                    throw new FedoraIllegalContentException(
                            "Fedora object does not have datastream that it " +
                            "just said it had",e);
                }
                updateOntology(model.getOntology(), newOntology);
            } else if (def.equals(
                    Constants.DS_COMPOSITE_MODEL_DATASTREAM)) {
                // Add DS-COMPOSITE-MODEL datastream information to compound
                // model datastreams
                Document dsCompositeXml;

                try {
                    dsCompositeXml = fedoraConnector.getDatastream(
                            p,def);
                } catch (DatastreamNotFoundException e) {
                    throw new FedoraIllegalContentException(
                            "Fedora object does not have datastream that it " +
                            "just said it had",e);
                }


                try {
                    addDatastream(model.getDatastreams(), dsCompositeXml, p,fedoraConnector);
                } catch (DatastreamNotFoundException e) {
                    throw new FedoraIllegalContentException("Illegal content" +
                                                            "model",e);
                }
            }
        }
    }


    /**
     * Add a pid to the list, if it is not already in the list.
     *
     * @param pids   The pid to add.
     * @param newpid The list to add to.
     */
    private static void addPid(List<String> pids, String newpid) {
        newpid = FedoraUtil.ensurePID(newpid);
        if (!pids.contains(newpid)) {
            pids.add(newpid);
        }
    }

    /**
     * Update an ontology with all content found in another ontology.
     *
     * @param ontology    The ontology to update.
     * @param newOntology The ontology with rules to add.
     */
    private static void updateOntology(Document ontology,
                                       Document newOntology) {
        NodeList nodes = newOntology.getDocumentElement()
                .getChildNodes();
        for (int n = 0; n < nodes.getLength(); n++) {
            Node node = nodes.item(n);
            ontology.getDocumentElement()
                    .appendChild(ontology.importNode(node, true));
        }
    }

    /**
     * Parse a DS-COMPOSITE-MODEL structure, and add the result to a list of
     * datastreams, if a datastream with that ID is not already in it. The
     * extension SCHEMA are parsed, and updated in Datastream if not
     * already present.
     *
     * @param datastreams    The list to add datastream to.
     * @param dsCompositeXml The datastream to parse.
     * @param pid            The pid of the object containing the datastream.
     */
    private static void addDatastream(List<Datastream> datastreams,
                                      Document dsCompositeXml,
                                      String pid,
                                      FedoraConnector fedoraConnector)
            throws FedoraIllegalContentException,
                   FedoraConnectionException,
                   DatastreamNotFoundException,
                   ObjectNotFoundException {
        NodeList xpathResult;
        try {
            xpathResult = XpathUtils.xpathQuery(
                    dsCompositeXml, DSCOMPOSITE_MODEL_TYPE_MODEL);
        } catch (XPathExpressionException e) {
            throw new FedoraIllegalContentException("Invalid DSCOMPOSITEMODEL " +
                                                    "datastream in '" + pid +
                                                    "'");
        }

        // Run through all defined datastreams
        for (int i = 0; i < xpathResult.getLength(); i++) {
            // Get the ID of the datastream
            Node dsTypeModelNode = xpathResult.item(i);
            if (dsTypeModelNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }
            Element dsTypeModel = (Element) dsTypeModelNode;
            String id = dsTypeModel.getAttribute("ID");
            if (id == null || id.equals("")) {
                continue;
            }

            Datastream datastream = null;
            // Find datastream if already defined
            for (Datastream d : datastreams) {
                if (d.getName().equals(id)) {
                    // already in list, skip
                    datastream = d;
                    break;
                }
            }

            if (datastream == null) {
                // Prepare new datastream description.
                datastream = new Datastream();
                datastream.setName(id);
                // Add the defined datastream
                datastreams.add(datastream);
            }

            // Get mimetype, if not already set, and add it to the datastream
            if (datastream.getMimetypes().size() == 0) {
                addMimetypesToDatastream(datastream, dsTypeModel);
            }

            // Get formaturi, if not already set, and add it to the datastream
            if (datastream.getFormatUris().size() == 0) {
                addFormatUrisToDatastream(datastream, dsTypeModel);
            }

            // Get schema, if not already set, and add it to the datastream
            if (datastream.getXmlSchema() == null) {
                addSchemaInformationToDatastream(datastream, dsTypeModel, pid, fedoraConnector);
            }
        }
    }

    /**
     * Given a datastream description from DS-COMPOSITE-MODEL, add information
     * about the mimetypes to the datastream model.
     *
     * @param datastream  The datastream to add mimetype information to.
     * @param dsTypeModel The dsTypeModel element to read the information from.
     */
    private static void addMimetypesToDatastream(Datastream datastream,
                                                 Element dsTypeModel) {
        NodeList mimetypeattrs;
        try {
            mimetypeattrs = XpathUtils
                    .xpathQuery(dsTypeModel, DS_FORM_MIME);
        } catch (XPathExpressionException e1) {
            //MIME not defined, so ignore
            LOG.debug("Caught exception ",e1);
            return;
        }
        if (mimetypeattrs.getLength() > 0) {
            for (int a = 0; a < mimetypeattrs.getLength(); a++) {
                if (mimetypeattrs.item(a).getNodeType() == Document
                        .ATTRIBUTE_NODE) {
                    datastream.getMimetypes().add(
                            mimetypeattrs.item(a).getNodeValue());
                }
            }
        }
    }

    /**
     * Given a datastream description from DS-COMPOSITE-MODEL, add information
     * about the format uris to the datastream model.
     *
     * @param datastream  The datastream to add format uri information to.
     * @param dsTypeModel The dsTypeModel element to read the information from.
     * @throws FedoraIllegalContentException
     */
    private static void addFormatUrisToDatastream(Datastream datastream,
                                                  Element dsTypeModel)
            throws FedoraIllegalContentException {
        NodeList formaturis;
        try {
            formaturis = XpathUtils
                    .xpathQuery(dsTypeModel, DS_FORM_FORMAT_URI);
        } catch (XPathExpressionException e1) {
            //Format URI not defined, so ignore
            LOG.debug("Caught exception ",e1);
            return;
        }
        if (formaturis.getLength() > 0) {
            for (int a = 0; a < formaturis.getLength(); a++) {
                if (formaturis.item(a).getNodeType() == Document
                        .ATTRIBUTE_NODE) {
                    String uri = formaturis.item(a).getNodeValue();
                    try {
                        datastream.getFormatUris().add(new URI(uri));
                    } catch (URISyntaxException e1) {
                        throw new FedoraIllegalContentException(
                                "URI '" + uri
                                + "' is invalid FORMAT_URI",
                                e1);
                    }
                }
            }
        }
    }


    /**
     * Given a datastream description from DS-COMPOSITE-MODEL, add information
     * about schema to the datastream model.
     *
     * @param datastream  The datastream to add schema information to.
     * @param dsTypeModel The dsTypeModel element to read the information from.
     * @param pid the pid of the current content model
     * @param fedoraConnector
     * @throws DatastreamNotFoundException
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     */
    private static void addSchemaInformationToDatastream(Datastream datastream,
                                                         Element dsTypeModel,
                                                         String pid, FedoraConnector fedoraConnector)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            DatastreamNotFoundException, ObjectNotFoundException {
        NodeList schemas;
        try {
            schemas = XpathUtils.xpathQuery(
                    dsTypeModel,
                    DS_EXTENSIONS_NAME_SCHEMA_SCHEMA_SCHEMA_TYPE_XSD);
        } catch (XPathExpressionException e1) {
            throw new Error("XPath expression did not evaluate", e1);
        }
        for (int elm = 0; elm < schemas.getLength(); elm++) {
            if (schemas.item(elm).getNodeType() == Document
                    .ELEMENT_NODE) {
                //TODO: Fail on more than one?
                String schemaDatastream = ((Element) schemas.item(elm))
                        .getAttribute("datastream");
                String schemaObject = ((Element) schemas.item(elm))
                        .getAttribute("object");
                if (schemaObject == null || schemaObject.equals("")) {
                    schemaObject = pid;
                }
                schemaObject = FedoraUtil.ensurePID(schemaObject);
                Document schema = fedoraConnector.getDatastream(
                        schemaObject, schemaDatastream);

                datastream.setXmlSchema(schema);
            }
        }
    }

}
