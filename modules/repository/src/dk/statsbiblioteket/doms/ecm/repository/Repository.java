package dk.statsbiblioteket.doms.ecm.repository;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.DatastreamNotFoundException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectIsWrongTypeException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException;
import org.w3c.dom.Document;

import java.util.List;

/**
 * This is the connection independent way for the webservice to talk to Fedora.
 *
 * This class is "static", ie it is never supposed to be instantiated. Instead,
 * before the class is used it must be "initialised" by a call to the initialise
 * method. In this call, you provide the fedora connector object, which this
 * class will use to delegate all fedora communication.
 *
 * @see #initialise(FedoraUserToken, FedoraConnector)
 */
public class Repository {


    /**
     * The connector object through which all communication will be made
     */
    private static FedoraConnector connector;

    private static String pidGenerator = "dk.statsbiblioteket.doms.ecm.repository.PidGeneratorImpl";


    /**
     * The static fedora uri prefix, to convert between pids and uris
     */
    private static final String FEDORA_URI_PREFIX = "info:fedora/";


    //TODO javadoc or remove
    public static String getUser(){
        return connector.getUsername();
    }

    /**
     * Initialise this static class. This method must be called before any other
     * or IllegalStateExceptions will be thrown.
     * If any of these arguments are null, you risk getting nullpointerexceptions
     * later on.
     * @param usertoken The usertoken, containing the username, password and
     * server url
     * @param connectorObject the Connector object which will handle the
     * communication with fedora
     */
    public static void initialise(
            FedoraUserToken usertoken,
            FedoraConnector connectorObject) {
        connector = connectorObject;
        connector.initialise(usertoken);
    }


    public static PidList query(String query)
            throws IllegalStateException,
                   FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.query(query);
    }


    public static boolean addRelation(String from, String relation, String to)
            throws IllegalStateException, ObjectNotFoundException,
                   FedoraConnectionException, FedoraIllegalContentException {
        return connector.addRelation(from, relation, to);
    }

    public static boolean addLiteralRelation(
            String from,
            String relation,
            String value,
            String datatype)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.addLiteralRelation(from, relation, value, datatype);
    }

    public static Document getObjectXml(String pid)
            throws FedoraConnectionException,
                   FedoraIllegalContentException,
                   ObjectNotFoundException {
        return connector.getObjectXml(pid);
    }

    public static String ingestDocument(
            Document newobject,
            String logmessage)
            throws FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.ingestDocument(newobject, logmessage);
    }

    public static List<FedoraConnector.Relation> getRelations(String pid)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.getRelations(pid);
    }

    public static List<FedoraConnector.Relation> getRelations(
            String pid,
            String relation)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.getRelations(pid, relation);
    }

    public static Document getDatastream(String pid, String dsid)
            throws DatastreamNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException,
                   ObjectNotFoundException {
        return connector.getDatastream(pid, dsid);
    }

    public static List<String> getContentModels(String pid)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.getContentModels(pid);
    }

    public static List<String> getInheritingContentModels(String cmpid)
            throws FedoraConnectionException, ObjectNotFoundException,
                   FedoraIllegalContentException, ObjectIsWrongTypeException {
        return connector.getInheritingContentModels(cmpid);
    }


    public static List<String> getInheritedContentModels(String cmpid)
            throws FedoraConnectionException, ObjectNotFoundException,
                   FedoraIllegalContentException, ObjectIsWrongTypeException {
        return connector.getInheritedContentModels(cmpid);
    }



    public static List<String> listDatastreams(String pid)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        return connector.listDatastreams(pid);
    }

    public static boolean exist(String pid) throws FedoraConnectionException,
                                                   FedoraIllegalContentException {
        return connector.exists(pid);
    }

    public static boolean isDataObject(String pid) throws
                                                   FedoraConnectionException,
                                                   FedoraIllegalContentException {
        return connector.isDataObject(pid);
    }

    public static boolean isTemplate(String pid) throws ObjectNotFoundException,
                                                        FedoraIllegalContentException,
                                                        FedoraConnectionException {
        return connector.isTemplate(pid);
    }

    public static boolean isContentModel(String pid) throws
                                                     FedoraConnectionException,
                                                     FedoraIllegalContentException {
        return connector.isContentModel(pid);
    }

    /**
     * If the given string starts with "info:fedora/", remove it.
     *
     * @param pid A pid, possibly as a URI
     * @return The pid, with the possible URI prefix removed.
     */
    public static String ensurePID(String pid) {
        if (pid.startsWith(FEDORA_URI_PREFIX)) {
            pid = pid.substring(FEDORA_URI_PREFIX.length());
        }
        return pid;
    }

    /**
     * If the given string does not start with "info:fedora/", add it.
     *
     * @param uri An URI, possibly as a PID
     * @return The uri, with the possible URI prefix prepended.
     */
    public static String ensureURI(String uri) {
        if (!uri.startsWith(FEDORA_URI_PREFIX)) {
            uri = FEDORA_URI_PREFIX + uri;
        }
        return uri;
    }

    public static String getPidGenerator() {
        return pidGenerator;
    }

    public static void setPidGenerator(String pidGenerator) {
        Repository.pidGenerator = pidGenerator;
    }

    public static boolean authenticate() throws FedoraConnectionException{

        //FIXME: Call the interfaces....
        return false;  //To change body of created methods use File | Settings | File Templates.
    }
}
