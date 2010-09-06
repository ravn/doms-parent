package dk.statsbiblioteket.doms.ecm.repository;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.*;
import org.w3c.dom.Document;

import java.util.List;

/**
 * This is the interface to the underlying repository system. The purpose
 * of this system is to allow for multiple access models to be implemented.
 * <br/>
 * Implementing classes should provide a no-args constructor.
 * <br/>
 * The method initialise must be called, before any other methods are used. The
 * purpose of this method if to provide the user credentials for connecting
 * to Fedora. Without this information, the connector obvoiusly cannot connect.
 * Methods called before initialise will throw IllegalStateException
 * <br/>
 * About pids: Fedora pids come in two forms, with and without the
 * "info:fedora/" prefix. The implementations of this interface must be guaranteed
 * to be agnostic towards this. Ie. they will understand both forms. They are
 * free to choose which of these forms they will for returned pids, but this
 * should be noted in the javadoc.  
 *
 * @see #initialise(FedoraUserToken)
 */
public interface FedoraConnector {


    /**
     * This is the pseudo constructor. This method must be called before
     * anything else is attempted. 
     * @param token A token contain the login credentials and the server url of
     * the fedora instance
     */
    public void initialise(FedoraUserToken token);

    //Most of the methods inhere should be directly assesible from the
    //webservice, except the ones marked not public

    /**
     * Checks if the object with the given pid exists in the repository
     * @param pid the pid of the object
     * @return true if the object exists
     * @throws IllegalStateException if the connector have not been initialised
     */
    public boolean exists(String pid)
            throws IllegalStateException, FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException;


    /**
     * Checks if the object with the given pid exists and is a data object
     * @param pid the pid of the object
     * @return true if the object exits and is a data object
     * @throws IllegalStateException if the connector have not been initialised
     */
    public boolean isDataObject(String pid)
            throws IllegalStateException, FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException;


    /**
     * Checks if the object with the given pid exists and is a template object
     * @param pid the pid of the object
     * @return true if the object exits and is a template object
     * @throws IllegalStateException if the connector have not been initialised
     */
    public boolean isTemplate(String pid)
            throws IllegalStateException, ObjectNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;


    /**
     * Checks if the object with the given pid exists and is a content model
     *  object
     * @param pid the pid of the object
     * @return true if the object exits and is a content model object
     * @throws IllegalStateException if the connector have not been initialised
     */
    public boolean isContentModel(String pid)
            throws IllegalStateException, FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException;


    /**
     * Not public.
     *
     * Queries must start with
     * "select $object
     * from <#ri>
     * where"
     *
     * @param query the query to execute.
     * @return a list of objects
     * @throws FedoraConnectionException if something went wrong with the
     * fedora connection
     * @throws FedoraIllegalContentException if the fedora reply could not be
     * parsed
     * @throws IllegalStateException if the connector have not been initialised
     */
    public PidList query(String query)
            throws IllegalStateException,
                   FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException;


    /**
     * Adds a relation to the given object.
     * @param from the object to add the relation to
     * @param relation the name of the relation
     * @param to the target of the relation
     * @return true, if the relation was added. False, if the relation is
     * already there.
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)
     */
    public boolean addRelation(String from, String relation, String to)
            throws IllegalStateException, ObjectNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;

    /**
     * Add a literal value as a relation
     * @param from the object to add the relation to
     * @param relation the name of the literal
     * @param value the value
     * @param datatype the datatype of the literal
     * //TODO what to put here
     * @return true, if added
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)

     */
    public boolean addLiteralRelation(String from,
                                      String relation,
                                      String value,
                                      String datatype)
            throws IllegalStateException, ObjectNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;

    /**
     * Gets the entire object as a Document
     * @param pid the pid of the object
     * @return the object as a Document
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws FedoraIllegalContentException if
     * the object cannot be parsed to a document
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)

     */
    public Document getObjectXml(String pid)
            throws IllegalStateException,
                   ObjectNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException;


    /**
     * //TODO this method should not be public
     * Ingest the object into the repository
     * @param newobject the Document to ingest
     * @param logmessage the message that should appear in the audit trail about
     * who and why the object was created
     * @return The pid of the new object
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws FedoraIllegalContentException if
     * the object cannot be parsed to a document
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)

     */
    public String ingestDocument(Document newobject, String logmessage)
            throws IllegalStateException,
                   FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException;

    /**
     * Get all relations in the object
     * @param pid the pid of the object
     * @return List of relations
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)

     */
    public List<Relation> getRelations(String pid)
            throws IllegalStateException, FedoraConnectionException,
                   ObjectNotFoundException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;

    /**
     * Get all relations in the object, with the given name
     * @param pid the pid of the object
     * @param relation the name of the relation
     * @return List of relations
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)
     */
    public List<Relation> getRelations(String pid, String relation)
            throws IllegalStateException, FedoraConnectionException,
                   ObjectNotFoundException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;

    /**
     * Get the datastream as a Document
     * @param pid the pid of the object
     * @param dsid the id of the datastream
     * @return Datastream as Document
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws DatastreamNotFoundException if the
     * object is found, but does not have a datastream with this id
     * @throws FedoraIllegalContentException if
     * the datastream cannot be parsed to a document
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)
     */
    public Document getDatastream(String pid, String dsid)
            throws IllegalStateException,
                   DatastreamNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException,
                   ObjectNotFoundException, InvalidCredentialsException;

    /**
     * Get the content models of the object. This method returns all the
     * content models of the object in the order of precedence. As such, the
     * first content model in the list is the most specific content model for
     * the object.<br>
     * The list is achieved by going through the inheritance tree breadth-first.
     * @param pid the pid of the object
     * @return List of pids of content models
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)
     */
    public PidList getContentModels(String pid)
            throws IllegalStateException, FedoraConnectionException,
                   ObjectNotFoundException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;


    /**
     * To implement inheritance, modify this method. All ECM functions use this
     * method when resolving inheritance.
     *
     * This method shall return the entire list of descendants of the specified
     * content model, not just the first
     * level. It will only be called once by an application that want the
     * inheritance
     * @param cmpid the content model pid
     * @return List of content models
     * if there is a problem with the communication with Fedora
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)
     */
    public PidList getInheritingContentModels(String cmpid)
            throws IllegalStateException, FedoraConnectionException,
                   ObjectNotFoundException, ObjectIsWrongTypeException,
                   FedoraIllegalContentException, InvalidCredentialsException;

    /**
     * Get the list of datastreams in the object
     * @param pid the pid of the object
     * @return List of IDs of the datastreams in the object
     * @throws FedoraConnectionException
     * if there is a problem with the communication with Fedora
     * @throws ObjectNotFoundException if the object
     * is not found in the repository
     * @throws IllegalStateException If initialise have not been called.
     * @see #initialise(FedoraUserToken)
     */
    public List<String> listDatastreams(String pid)
            throws IllegalStateException, FedoraConnectionException,
                   ObjectNotFoundException,
                   FedoraIllegalContentException,
                   InvalidCredentialsException;




    //TODO should this be here
    public String getUsername();

    public PidList getInheritedContentModels(String cmpid) throws
                                                           FedoraConnectionException,
                                                           ObjectNotFoundException,
                                                           ObjectIsWrongTypeException,
                                                           FedoraIllegalContentException,
                                                           InvalidCredentialsException;

    /**
     * Test if the credentials given in the constructor is sufficient for the
     * connector to work
     * @return true if the credentials are good
     */
    public boolean authenticate()
            throws FedoraConnectionException;

    public String getUser();

    /**
     * The standard way to represent a relation
     */
    public static class Relation {

        private String from,to,relation;

        /**
         * Create a new Relation object. This constructor should not be used
         * except in classes implementing FedoraConnector
         * @param from The object, ie the object wherein the relation originates
         * @param to the Subject, ie the target of the relation
         * @param relation the full name of the relation
         */
        public Relation(String from, String to, String relation) {
            this.from = from;
            this.to = to;
            this.relation = relation;
        }

        /**
         * From is the pid of the object that holds the relation.
         * Called object in rdf terminology
         * @return from
         */
        public String getFrom() {
            return from;
        }

        /**
         * To is the pid of the object pointed to, the target. Called subject
         * in rdf terminology
         *
         * @return to
         */
        public String getTo() {
            return to;
        }

        /**
         * The fully qualified name of the relation. Called Property in
         * rdf terminology
         * @return the relation
         */
        public String getRelation() {
            return relation;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Relation) {
                Relation relation1 = (Relation) obj;
                return relation1.getTo().equals(this.getTo())
                       && relation1.getFrom().equals(this.getFrom())
                       && relation1.getRelation().equals(this.getRelation());
            } else{
                return false;
            }
        }
    }
}
