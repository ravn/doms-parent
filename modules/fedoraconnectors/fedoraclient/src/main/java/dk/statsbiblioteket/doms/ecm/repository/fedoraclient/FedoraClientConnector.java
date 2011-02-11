package dk.statsbiblioteket.doms.ecm.repository.fedoraclient;

import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.FedoraUserToken;
import dk.statsbiblioteket.doms.ecm.repository.PidList;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.*;
import dk.statsbiblioteket.doms.ecm.repository.utils.Constants;
import dk.statsbiblioteket.doms.ecm.repository.utils.DocumentUtils;
import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import org.apache.axis.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.DatastreamDef;
import org.fcrepo.server.types.gen.MIMETypedStream;
import org.fcrepo.server.types.gen.ObjectProfile;
import org.fcrepo.server.types.gen.RelationshipTuple;
import org.jrdf.graph.Node;
import org.trippi.TrippiException;
import org.trippi.TupleIterator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.*;


/**
 * This is a implementation of the Fedora connector, based on the old
 * FedoraClient This FedoraClient is really a wrapping of the soap api.
 */
public class FedoraClientConnector
        implements FedoraConnector {


    private static final Log LOG = LogFactory.getLog(FedoraClientConnector.class);
    private FedoraUserToken token;

    //Do not get this directly, use the accessor
    private FedoraClient client;
    private FedoraAPIM apiM;
    private FedoraAPIA apiA;

    //noargs constructor, as required
    public FedoraClientConnector() {

    }


    public void initialise(FedoraUserToken token) {
        this.token = token;
        apiA = null;
        apiM = null;
        client = null;
    }


    public boolean addRelation(String from, String relation, String to)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {
        from = FedoraUtil.ensureURI(from);
        to = FedoraUtil.ensureURI(to);
        try {
            return getAPIM().addRelationship(from, relation, to, false, null);
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+from+"' was not found",axisFault.getCause());
                }

            }
            throw new FedoraConnectionException(
                    "Something went wrong in the connection with fedora",
                    e);
        }
    }

    public boolean addLiteralRelation(String from,
                                      String relation,
                                      String value,
                                      String datatype)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {
        from = FedoraUtil.ensureURI(from);

     
        try {
            return getAPIM().addRelationship(from,
                                             relation,
                                             value,
                                             true,
                                             datatype);
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+from+"' was not found",axisFault.getCause());
                }
            }
            throw new FedoraConnectionException(
                    "Something went wrong in the connection with fedora",
                    e);
        }
    }

    public List<Relation> getRelations(String pid)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {
        return getRelations(pid, null);
    }

    public List<Relation> getRelations(String pid, String relation)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {
        pid = FedoraUtil.ensureURI(pid);
        try {
            RelationshipTuple[] relations = getAPIM().getRelationships(pid,
                                                                       relation);
            List<Relation> result = new ArrayList<Relation>();
            if (relations != null) {

                for (RelationshipTuple rel : relations) {
                    result.add(toRelation(rel));
                }

            }
            return result;
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+pid+"' was not found",axisFault.getCause());
                }
            }
            throw new FedoraConnectionException(
                    "Something failed in the communication with Fedora",
                    e);
        }

    }

    private Relation toRelation(RelationshipTuple rel) {
        return new Relation(rel.getSubject(),
                            rel.getObject(),
                            rel.getPredicate());
    }

    public PidList getContentModels(String pid)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {

        pid = FedoraUtil.ensurePID(pid);

        ObjectProfile profile;
        try {
            profile = getAPIA().getObjectProfile(pid, null);
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+pid+"' was not found",lowlevelStorageException);
                }

            }
            throw new FedoraConnectionException(
                    "Failed in communication with Fedora",
                    e);
        }

        String[] models = profile.getObjModels();
        PidList localmodels = new PidList(Arrays.asList(models));
        return localmodels;
        //TODO Do we really want to perform this breath first search? I mean, we have all the inherited content models in the objects, per definition already. 
        //return getInheritedContentModelsBreadthFirst(localmodels);
    }


    private PidList getInheritedContentModelsBreadthFirst(PidList contentmodels)
            throws FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException {


        /*
        bfs (Graph G) {
	        all vertices of G are first painted white

	        the graph root is painted gray and put in a queue

	        while the queue is not empty {
	            a vertex u is removed from the queue

	            for all white successors v of u {
	                v is painted gray
		            v is added to the queue
	            }

	            u is painted black
	        }
        }
        */

        //all vertices of G are first painted white
        //all content models are white if not in one of the sets grey or black
        Set<String> grey = new HashSet<String>();
        PidList black = new PidList();
        Queue<String> queue = new LinkedList<String>();

        //the graph root is painted gray and put in a queue
        for (String startingcontentmodel : contentmodels) {
            queue.add(startingcontentmodel);
            grey.add(startingcontentmodel);
        }

        //while the queue is not empty {
        while (queue.size() > 0) {
            //a vertex u is removed from the queue
            String u = queue.poll();

            //    for all white successors v of u {
            List<String> successor_of_u = getAncestors(u);
            for (String v : successor_of_u) {
                if (grey.contains(v) || black.contains(v)) {
                    continue;
                }

                //v is painted gray
                grey.add(v);
                //v is added to the queue
                queue.add(v);
            }
            //u is painted black
            black.add(u);
        }
        return black;
    }

    private List<String> getAncestors(String s)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        PidList temp = new PidList();

        List<Relation> ancestors = null;
        try {
            ancestors = getRelations(s, Constants.RELATION_EXTENDS_MODEL);
        } catch (ObjectNotFoundException e) {
            //Content model does not exist, but that is not a problem. It just
            //does not have ancestors
            return temp;
        }
        for (Relation ancestor : ancestors) {
            temp.add(ancestor.getTo());
        }
        return temp;
    }

    /**
     * @param cmpid the content model pid
     * @return an empty list
     */
    public PidList getInheritingContentModels(String cmpid)
            throws FedoraConnectionException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException {
        cmpid = FedoraUtil.ensureURI(cmpid);
/*
        if (!exists(cmpid)) {
            throw new ObjectNotFoundException("Object '" + cmpid + "' does not exist in the Repository");
        }
        if (!isContentModel(cmpid)) {
            throw new ObjectIsWrongTypeException("Object '" + cmpid + "' is not a content model");
        }
*/

        PidList descendants = query("select $object \n" + "from <#ri>\n" + "where \n" + "walk(\n" + "$object <" + Constants.RELATION_EXTENDS_MODEL + "> <" + cmpid + ">\n" + "and\n" + "$object <" + Constants.RELATION_EXTENDS_MODEL + "> $temp\n" + ");");
        return descendants;

    }


    public PidList getInheritedContentModels(String cmpid)
            throws FedoraConnectionException,
                   ObjectIsWrongTypeException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException {
        cmpid = FedoraUtil.ensurePID(cmpid);
        return getInheritedContentModelsBreadthFirst(new PidList(cmpid));
    }

    public boolean authenticate() throws FedoraConnectionException {

        try {
            getFedoraClient().getAPIA().describeRepository();
        } catch (ServiceException e) {
            return false;
        } catch (IOException e) {
            throw new FedoraConnectionException("Fedora exception encountered",e);
        }
        return true;
    }

    public String getUser() {
        return token.getUsername();
    }


    public List<String> listDatastreams(String pid)
            throws FedoraConnectionException, ObjectNotFoundException,
                   FedoraIllegalContentException, InvalidCredentialsException {
        pid = FedoraUtil.ensurePID(pid);

        try {
            DatastreamDef[] datastreams = getAPIA().listDatastreams(pid, null);
            List<String> result = new ArrayList<String>();
            for (DatastreamDef def : datastreams) {
                result.add(def.getID());
            }
            return result;
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+pid+"' was not found",axisFault.getCause());
                }

            }
            throw new FedoraConnectionException("Something failed in the " + "communication with Fedora",
                                                e);
        }
    }

    public String getUsername() {
        return token.getUsername();
    }

    public String ingestDocument(Document document, String logmessage)
            throws
            FedoraConnectionException,
            FedoraIllegalContentException,
            InvalidCredentialsException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            DocumentUtils.DOCUMENT_TRANSFORMER.transform(new DOMSource(document),
                                                         new StreamResult(
                                                                 byteArrayOutputStream));
        } catch (TransformerException e) {
            throw new FedoraIllegalContentException("The new object could not" + "be transformed to a stream",
                                                    e);
        }

        //TODO check the document for all things that make the Fedora system fail
        try {
            return getAPIM().ingest(byteArrayOutputStream.toByteArray(),
                                    FedoraClient.FOXML1_1.uri,
                                    logmessage);
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
            }
            throw new FedoraConnectionException(
                    "The object could not be ingested",
                    e);
        }
    }

    /**
     * Get object XML from Fedora, and return it as a DOM document
     *
     * @param pid The PID of the document to retrieve. May be represented as a
     *            PID, or as a Fedora URI.
     * @return The object parsed in a DOM.
     */
    public String getObjectXml(String pid)
            throws FedoraConnectionException, FedoraIllegalContentException,
                   ObjectNotFoundException, InvalidCredentialsException {
        pid = FedoraUtil.ensurePID(pid);


        byte[] objectXML;
        try {
            objectXML = getAPIM().getObjectXML(pid);

        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+pid+"' was not found",lowlevelStorageException);
                }

            }
            throw new FedoraConnectionException("Error getting XML for '" + pid + "' from Fedora",
                                                e);
        } catch (IOException e) {
            throw new FedoraConnectionException("Error getting XML for '" + pid + "' from Fedora",
                                                e);
        }
        String result = new String(objectXML);
        return result;
    }

    /**
     * Retrieve a datastream from Fedora, and parse it as document.
     *
     * @param pid        The ID of the object to get the datastream from.
     * @param datastream The ID of the datastream.
     * @return The datastream parsed as a DOM document.
     */
    public Document getDatastream(String pid, String datastream)
            throws DatastreamNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException,
                   ObjectNotFoundException,
                   InvalidCredentialsException {

        pid = FedoraUtil.ensurePID(pid);

        MIMETypedStream dsCompositeDatastream;
        byte[] buf;
        try {
            dsCompositeDatastream = getAPIA()
                    .getDatastreamDissemination(pid, datastream, null);
            buf = dsCompositeDatastream.getStream();
        } catch (RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                if (axisFault.getCause() instanceof AuthzException) {
                    AuthzException authzException
                            = (AuthzException) axisFault.getCause();
                    throw new InvalidCredentialsException(
                            "The supplied credentials were insufficient for the"
                            + " task at hand",authzException);
                }
                if (axisFault.getCause() instanceof LowlevelStorageException) {
                    LowlevelStorageException lowlevelStorageException
                            = (LowlevelStorageException) axisFault.getCause();
                    throw new ObjectNotFoundException("The object '"+pid+"' was not found",axisFault.getCause());
                }

            }
            if (e.getMessage().contains(
                    "DatastreamNotFoundException")) {

                throw new DatastreamNotFoundException(
                        "Error getting datastream'" + datastream + "' from '" + pid + "'",
                        e);

            }
            throw new FedoraConnectionException("Error getting datastream'" + datastream + "' from '" + pid + "'",
                                                e);
        }

        Document dsCompositeXml;
        try {
            dsCompositeXml = DocumentUtils.DOCUMENT_BUILDER.parse(new ByteArrayInputStream(
                    buf));
        } catch (SAXException e) {
            throw new FedoraIllegalContentException("Error parsing datastream '" + datastream + "'  from '" + pid + "' as XML",
                                                    e);
        } catch (IOException e) {
            throw new Error("IOTrouble reading from byte array stream, " + "this should never happen",
                            e);
        }
        return dsCompositeXml;
    }


    public boolean exists(String pid)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        return hasContentModel(pid, Constants.FEDORA_OBJECT_3_0);
    }

    public boolean isDataObject(String pid)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        boolean cm = hasContentModel(pid, Constants.CONTENT_MODEL_3_0);
        boolean sdef = hasContentModel(pid, Constants.SERVICE_DEFINITION_3_0);
        boolean sdep = hasContentModel(pid, Constants.SERVICE_DEPLOYMENT_3_0);
        return !cm && !sdef && !sdep;
    }


    public boolean isTemplate(String pid)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {

        List<Relation> templaterels = getRelations(pid, Constants.TEMPLATE_REL);
        return templaterels.size() > 0;
    }

    public boolean isContentModel(String pid)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        return hasContentModel(pid, Constants.CONTENT_MODEL_3_0);
    }

    public boolean hasContentModel(String pid, String cmpid)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        PidList contentmodels = query("select $object\n" + "from <#ri>\n" + "where\n <" + FedoraUtil.ensureURI(
                pid) + "> <" + Constants.HAS_MODEL + "> " + "$object\n");
        return contentmodels.contains(FedoraUtil.ensurePID(cmpid));

    }


    public PidList query(String query)
            throws FedoraConnectionException, FedoraIllegalContentException, InvalidCredentialsException {


        PidList pidlist = new PidList();

        LOG.debug("Entering query with this string \n'" + query + "'\n");
        Map<String, String> map = new HashMap<String, String>();
        map.put("lang", "itql");
        map.put("query", query);
        map.put("stream","on");
        map.put("flush", "true");

        final TupleIterator tupleIterator;
        try {
            tupleIterator = getFedoraClient().getTuples(map);
        } catch (IOException e) {
            if (e.getMessage().startsWith("Request failed [401 Unauthorized]")){
                throw new FedoraConnectionException(
                    "IO exception when communication with fedora",
                    e);
                //TODO here put proper error because the credentials was not valid
            } else{
            throw new FedoraConnectionException(
                    "IO exception when communication with fedora",
                    e);
            }
        }

        try {
            while (tupleIterator.hasNext()) {
                final Map<String, Node> tuple = tupleIterator.next();
                String subject = FedoraUtil.ensurePID(tuple.get("object").toString());
                pidlist.add(subject);
            }
        } catch (TrippiException e) {
            throw new FedoraIllegalContentException(
                    "Incorrect data was returned",
                    e);
        }

        return pidlist;
    }

    /**
     * Gets a Fedora client. If this is the first connect, or if the client has
     * been reset, the client is initialised, and connection to Fedora
     * initialised. Otherwise, the existing client is reused.
     *
     * @return The fedora client instance.
     * @throws FedoraConnectionException
     *          on trouble connectng to Fedora.
     */
    private synchronized FedoraClient getFedoraClient()
            throws FedoraConnectionException {

        if (token == null) {
            throw new IllegalStateException("Connector not initialised");
        }
        try {
            FedoraClient client = this.client;
            if (client == null) {

                client = new FedoraClient(token.getServerurl(),
                                          token.getUsername(),
                                          token.getPassword());
                this.client = client;
            }
            return client;
        } catch (MalformedURLException e) {
            throw new FedoraConnectionException("Error connecting to Fedora",
                                                e);
        } catch (IOException e) {
            throw new FedoraConnectionException("Error connecting to Fedora",
                                                e);
        }

    }

    /**
     * Get the API-M interface to Fedora.
     *
     * @return The API-M interface to Fedora.
     * @throws FedoraConnectionException
     *          on trouble connecting to Fedora.
     */
    private FedoraAPIM getAPIM() throws FedoraConnectionException {
        try {
            if (apiM == null){
                apiM = getFedoraClient().getAPIM();
            }
            return apiM;

        } catch (ServiceException e) {
            throw new FedoraConnectionException("Error connecting to Fedora",
                                                e);
        } catch (IOException e) {
            throw new FedoraConnectionException("Error connecting to Fedora",
                                                e);
        }
    }

    /**
     * Get the API-A interface to Fedora.
     *
     * @return The API-A interface to Fedora.
     * @throws FedoraConnectionException
     *          on trouble connecting to Fedora.
     */
    private FedoraAPIA getAPIA() throws FedoraConnectionException {

        try {
            if (apiA == null){
                apiA = getFedoraClient().getAPIA();
            }
            return apiA;
        } catch (IOException e) {
            throw new FedoraConnectionException("Error connecting to Fedora",
                                                e);
        } catch (ServiceException e) {
            throw new FedoraConnectionException("Error connecting to Fedora",
                                                e);
        }
    }
}
