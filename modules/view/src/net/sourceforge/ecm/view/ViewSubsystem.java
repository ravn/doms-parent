package net.sourceforge.ecm.view;

import net.sourceforge.ecm.exceptions.FedoraConnectionException;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.exceptions.ObjectIsWrongTypeException;
import net.sourceforge.ecm.exceptions.ObjectNotFoundException;
import net.sourceforge.ecm.repository.FedoraConnector;
import net.sourceforge.ecm.repository.PidList;
import net.sourceforge.ecm.repository.Repository;
import net.sourceforge.ecm.utils.Constants;
import net.sourceforge.ecm.utils.DocumentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The interface to the ViewSubsystem
 */
public class ViewSubsystem {

    private static final Log LOG = LogFactory.getLog(ViewSubsystem.class);


    /**
     * Nessesary no-args constructor
     */
    public ViewSubsystem() {
    }


    /**
     * Get the data objects which are marked (in their content models) as entries
     * for the given angle
     * @param viewAngle The given view angle
     * @return a lists of the data objects pids
     * @throws FedoraConnectionException if there
     * was a problem in communication with Fedora
     * @throws FedoraIllegalContentException if the return value from fedora
     * could not be parsed
     */
    public PidList getEntryCMsForAngle(
            String viewAngle) throws FedoraIllegalContentException,
                                     FedoraConnectionException {
        viewAngle = sanitizeLiteral(viewAngle);

        //TODO Inheritance?
        String query = "select $object\n" +
                       "from <#ri> \n" +
                       "where\n" +
                       "$object <"+ Constants.HAS_MODEL + "> <" +
                       Constants.CONTENT_MODEL_3_0 +"> \n" +
                       "and $object <"+ Constants.ENTRY_RELATION+"> '" +
                       viewAngle + "' \n";

        return Repository.query(query);
    }

    /**
     * Simple utility method for removing illegal characters in the viewAngle
     * name
     * @param viewAngle the viewAngle to sanitize
     * @return the sanitized viewAngle
     */
    private String sanitizeLiteral(String viewAngle) {
        viewAngle = viewAngle.replaceAll("'","");
        viewAngle = viewAngle.replaceAll("<","");
        return viewAngle;
    }


    /**
     * Simple utility method for removing illegal characters in a pid.
     * @param pid the pid to sanitize
     * @return the sanitized pid
     */
    private String sanitizePid(String pid){
        return pid;
    }

    /**
     * Get all data objects subscribing to a given content model, and with the
     * given status
     * @param cmpid the content model
     * @param status the given status, A, I or D
     * @return a list of data objects
     * @throws ObjectNotFoundException if cmpid could not be found
     * @throws ObjectIsWrongTypeException if cmpid is not a content model
     * @throws FedoraIllegalContentException If the fedora response could not be
     * parsed
     * @throws FedoraConnectionException if there was a problem in communicating
     * with Fedora
     */
    public PidList getObjectsForContentModel(
            String cmpid,
            String status) throws ObjectNotFoundException,
                                  ObjectIsWrongTypeException,
                                  FedoraIllegalContentException,
                                  FedoraConnectionException {
        LOG.trace("Entering getObjectsForContentModel with params '" +
                  cmpid + "' and '" + status + "'");
        status = sanitizeLiteral(status);

        cmpid = sanitizePid(cmpid);
        //TODO why do we sanitize?



        if (!Repository.exist(cmpid)){
            throw new ObjectNotFoundException("The pid '" + cmpid +
                                              "' is not in the repository");
        }
        if (!Repository.isContentModel(cmpid)){
            throw new ObjectIsWrongTypeException("The pid '" + cmpid +
                                                 "' is not a content model");
        }
        String contentModel = "<" + Repository.ensureURI(cmpid) + ">";

        List<String> childcms = Repository.getInheritingContentModels(cmpid);

        String query = "select $object\n" +
                       "from <#ri>\n" +
                       "where\n" +
                       " $object <" + Constants.STATEREL + "> <" +
                       Constants.NAMESPACE_FEDORA_MODEL +status+">\n"+
                       " and (\n";

        query = query +
                "$object <" + Constants.HAS_MODEL+"> " +contentModel+"\n ";
        for (String childCm:childcms){
            query = query +
                    " or $object <" + Constants.HAS_MODEL + "> <" +
                    Repository.ensureURI(childCm) + ">\n ";
        }
        query = query + ")";

        LOG.debug("Using query \n'" + query + "'\n");
        return Repository.query(query);

    }


    /**
     * Get all entry data objects for the given angle, but only entry objects
     * with the given state
     * @param viewAngle the viewangle
     * @param state the required state
     * @return a list of dataobjects
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     */
    public PidList getEntriesForAngle(String viewAngle,
                                      String state) throws
                                                    FedoraConnectionException,
                                                    FedoraIllegalContentException{

        Set<String> collector = new HashSet<String>();
        PidList list = getEntryCMsForAngle(viewAngle);
        for (String pid: list){
            try {
                collector.addAll(getObjectsForContentModel(pid,state));
            } catch (ObjectNotFoundException e) {
                throw new FedoraIllegalContentException(
                        "Content model '" +
                        pid + "' which was just" +
                        "found is not found any more",e);
            } catch (ObjectIsWrongTypeException e) {
                throw new FedoraIllegalContentException(
                        "Content model '" +
                        pid + "' which was just" +
                        "found is not a content model any more",e);

            }
        }

        PidList result = new PidList();
        result.addAll(collector);
        return result;

    }

    /**
     * Get a list of the objects in the view of a given object
     * @param objpid the object whose view we examine
     * @param viewAngle The view angle
     * @return the list of the pids in the view of objpid
     * @throws ObjectNotFoundException
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     */
    public PidList getViewObjectsListForObject(
            String objpid,
            String viewAngle)
            throws ObjectNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException {

        LOG.trace("Entering getViewObjectsListForObject with params '" +
                  objpid + "' and '" + viewAngle + "'");

        if (!Repository.exist(objpid)){
            throw new ObjectNotFoundException("The data object '" + objpid +
                                              "' does not exist");
        }
        if (!Repository.isDataObject(objpid)){
            throw new ObjectNotFoundException("The data object '" + objpid +
                                              "' is not a data object");
        }

        PidList includedPids = new PidList();

        appendPids(viewAngle,includedPids,objpid);

        return includedPids;
    }

    /**
     * Get a bundle of the xml dump of the objects in the view of objpid. The
     * objects will be bundled under the supertag
     * dobundle:digitalObjectBundle, where dobundle is defined in Constants
     * @param objpid the object whose view we examine
     * @param viewAngle The view angle
     * @return The objects bundled under the supertag
     * @throws ObjectNotFoundException
     * @throws FedoraIllegalContentException
     * @throws FedoraConnectionException
     * @see #getViewObjectsListForObject(String, String)
     * @see Constants#NAMESPACE_DIGITAL_OBJECT_BUNDLE
     */
    public Document getViewObjectBundleForObject(
            String objpid,
            String viewAngle) throws
                              ObjectNotFoundException,
                              FedoraIllegalContentException,
                              FedoraConnectionException {


        PidList pidlist = getViewObjectsListForObject(objpid,viewAngle);

        Document doc = DocumentUtils.DOCUMENT_BUILDER.newDocument();

        //There is no document element per default, so we make one
        doc.appendChild(
                doc.createElementNS(
                        Constants.NAMESPACE_DIGITAL_OBJECT_BUNDLE,
                        "dobundle:digitalObjectBundle"
                )
        );

        //And we get the new document element back
        Element docelement = doc.getDocumentElement();

        for (String pid: pidlist){
            //Get the object as a document
            Document objectdoc = Repository.getObjectXml(pid);

            //add it to the bundle we are creating
            Element objectdocelement = objectdoc.getDocumentElement();
            Node importobjectdocelement = doc.importNode(objectdocelement, true);
            docelement.appendChild(importobjectdocelement);
        }

        //return the bundle
        return doc;
    }


    private void appendPids(String viewname,
                            List<String> includedPids, String pid
    )
            throws ObjectNotFoundException,
                   FedoraIllegalContentException,
                   FedoraConnectionException {

        LOG.trace("Entering appendPids with params "+viewname+" and " + pid);
        pid = sanitizePid(pid);
        viewname = sanitizeLiteral(viewname);

        // Check if PIDs is there
        // This is the reason why we need to thread the list through the
        // recursion. Without it we would end in cycles
        pid = Repository.ensurePID(pid);
        if (includedPids.contains(pid)) {
            return;
        }
        includedPids.add(pid);
        LOG.trace("Pid '" + pid + "' added to includedPids");

        // Find relations to follow
        // Get content model
        CompoundView cm = CompoundView.getView(pid);
        View view = cm.getView().get(viewname);
        if (view == null) {
            LOG.debug("View null, returning");
            return;
        }

        // Outgoing relations
        List<String> properties = view.getProperties();
        for (String property : properties) {
            // Find relations
            List<FedoraConnector.Relation> relations;

            relations = Repository.
                    getRelations(pid, property);



            // Recursively add
            for (FedoraConnector.Relation relation: relations){
                String newpid = relation.getTo();
                appendPids(
                        viewname, includedPids,
                        newpid);

            }

        }

        // Incoming relations
        List<String> inverseProperties = view.getProperties();
        for (String inverseProperty : inverseProperties) {
            String query = "select $object\n" + "from <#ri>\n"
                           + "where $object <" + inverseProperty + "> <"
                           + Repository.ensureURI(pid) + ">";
            // Find relations


            PidList objects = Repository.query(query);
            // Recursively add
            for (String newpid: objects){
                appendPids(
                        viewname, includedPids,
                        newpid);
            }


        }
    }


}
