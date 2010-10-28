package dk.statsbiblioteket.doms.ecm.services.view;


import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.PidList;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.*;
import dk.statsbiblioteket.doms.ecm.repository.utils.Constants;
import dk.statsbiblioteket.doms.ecm.repository.utils.DocumentUtils;
import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import dk.statsbiblioteket.util.xml.DOM;
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
     * @param fedoraConnector
     * @return a lists of the data objects pids
     * @throws FedoraConnectionException if there
     * was a problem in communication with Fedora
     * @throws FedoraIllegalContentException if the return value from fedora
     * could not be parsed
     */
    public PidList getEntryCMsForAngle(
            String viewAngle, FedoraConnector fedoraConnector)
            throws FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException {
        viewAngle = sanitizeLiteral(viewAngle);

        //TODO Inheritance?
        String query = "select $object\n" +
                       "from <#ri> \n" +
                       "where\n" +
                       "$object <"+ Constants.HAS_MODEL + "> <" +
                       Constants.CONTENT_MODEL_3_0 +"> \n" +
                       "and $object <"+ Constants.ENTRY_RELATION+"> '" +
                       viewAngle + "' \n";

        return fedoraConnector.query(query);
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
     * @param fedoraConnector
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
            String status, FedoraConnector fedoraConnector)
            throws ObjectNotFoundException,
                   ObjectIsWrongTypeException,
                   FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException {
        LOG.trace("Entering getObjectsForContentModel with params '" +
                  cmpid + "' and '" + status + "'");
        status = sanitizeLiteral(status);

        cmpid = sanitizePid(cmpid);
        //TODO why do we sanitize?


        String contentModel = "<" + FedoraUtil.ensureURI(cmpid) + ">";

        List<String> childcms = fedoraConnector.getInheritingContentModels(cmpid);

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
                    FedoraUtil.ensureURI(childCm) + ">\n ";
        }
        query = query + ")";

        LOG.debug("Using query \n'" + query + "'\n");
        return fedoraConnector.query(query);

    }


    /**
     * Get all entry data objects for the given angle, but only entry objects
     * with the given state
     * @param viewAngle the viewangle
     * @param state the required state
     * @param fedoraConnector
     * @return a list of dataobjects
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     */
    public PidList getEntriesForAngle(String viewAngle,
                                      String state, FedoraConnector fedoraConnector)
            throws
            FedoraConnectionException,
            FedoraIllegalContentException,
            InvalidCredentialsException {

        Set<String> collector = new HashSet<String>();
        PidList list = getEntryCMsForAngle(viewAngle, fedoraConnector);
        for (String pid: list){
            try {
                collector.addAll(getObjectsForContentModel(pid,state, fedoraConnector));
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
     * @param fedoraConnector
     * @return the list of the pids in the view of objpid
     * @throws ObjectNotFoundException
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     */
    public PidList getViewObjectsListForObject(
            String objpid,
            String viewAngle, FedoraConnector fedoraConnector)
            throws ObjectNotFoundException,
                   FedoraConnectionException,
                   FedoraIllegalContentException, InvalidCredentialsException {

        LOG.trace("Entering getViewObjectsListForObject with params '" +
                  objpid + "' and '" + viewAngle + "'");
/*

        if (!fedoraConnector.exists(objpid)){
            throw new ObjectNotFoundException("The data object '" + objpid +
                                              "' does not exist");
        }
        if (!fedoraConnector.isDataObject(objpid)){
            throw new ObjectNotFoundException("The data object '" + objpid +
                                              "' is not a data object");
        }
*/

        PidList includedPids = new PidList();

        appendPids(viewAngle,includedPids,objpid, fedoraConnector);

        return includedPids;
    }

    /**
     * Get a bundle of the xml dump of the objects in the view of objpid. The
     * objects will be bundled under the supertag
     * dobundle:digitalObjectBundle, where dobundle is defined in Constants
     * @param objpid the object whose view we examine
     * @param viewAngle The view angle
     * @param fedoraConnector
     * @return The objects bundled under the supertag
     * @throws ObjectNotFoundException
     * @throws FedoraIllegalContentException
     * @throws FedoraConnectionException
     * @see #getViewObjectsListForObject(String, String,dk.statsbiblioteket.doms.ecm.repository.FedoraConnector)
     * @see Constants#NAMESPACE_DIGITAL_OBJECT_BUNDLE
     */
    public Document getViewObjectBundleForObject(
            String objpid,
            String viewAngle, FedoraConnector fedoraConnector) throws
                                                               ObjectNotFoundException,
                                                               FedoraIllegalContentException,
                                                               FedoraConnectionException,
                                                               InvalidCredentialsException {


        PidList pidlist = getViewObjectsListForObject(objpid,viewAngle, fedoraConnector);

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
            Document objectdoc = DOM.stringToDOM(fedoraConnector.getObjectXml(pid),true);

            //add it to the bundle we are creating
            Element objectdocelement = objectdoc.getDocumentElement();
            Node importobjectdocelement = doc.importNode(objectdocelement, true);
            docelement.appendChild(importobjectdocelement);
        }

        //return the bundle
        return doc;
    }


    private void appendPids(String viewname,
                            List<String> includedPids, String pid,
                            FedoraConnector fedoraConnector)
            throws ObjectNotFoundException,
                   FedoraIllegalContentException,
                   FedoraConnectionException, InvalidCredentialsException {

        LOG.trace("Entering appendPids with params "+viewname+" and " + pid);
        pid = sanitizePid(pid);
        viewname = sanitizeLiteral(viewname);

        // Check if PIDs is there
        // This is the reason why we need to thread the list through the
        // recursion. Without it we would end in cycles
        pid = FedoraUtil.ensurePID(pid);
        if (includedPids.contains(pid)) {
            return;
        }
        includedPids.add(pid);
        LOG.trace("Pid '" + pid + "' added to includedPids");

        // Find relations to follow
        // Get content model
        CompoundView cm = CompoundView.getView(pid, fedoraConnector);
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

            relations = fedoraConnector.
                    getRelations(pid, property);



            // Recursively add
            for (FedoraConnector.Relation relation: relations){
                String newpid = relation.getTo();
                appendPids(
                        viewname, includedPids,
                        newpid, fedoraConnector);

            }

        }

        // Incoming relations
        List<String> inverseProperties = view.getProperties();
        for (String inverseProperty : inverseProperties) {
            String query = "select $object\n" + "from <#ri>\n"
                           + "where $object <" + inverseProperty + "> <"
                           + FedoraUtil.ensureURI(pid) + ">";
            // Find relations


            PidList objects = fedoraConnector.query(query);
            // Recursively add
            for (String newpid: objects){
                appendPids(
                        viewname, includedPids,
                        newpid, fedoraConnector);
            }


        }
    }

    public PidList getEntryContentModelsForObjectForViewAngle(String pid,
                                                              String angle,
                                                              FedoraConnector fedoraConnector)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        LOG.trace("Entering getEntryContentModelsForObjectForViewAngle with params '" +
                  pid + "' and '" + angle + "'");


        pid = sanitizePid(pid);
        angle = sanitizeLiteral(angle);

        String query = "select $object\n"
                       + "from <#ri>\n"
                       + "where\n"
                       + "$object2 <fedora-model:hasModel> $object\n"
                       + "and\n"
                       + "$object2 <mulgara:is> <info:fedora/"+pid+">\n"
                       + "and\n"
                       + "$object <"+Constants.ENTRY_RELATION+"> '"+angle+"'";



        LOG.debug("Using query \n'" + query + "'\n");
        return fedoraConnector.query(query);

    }

  /*  public PidList getAllEntryObjectsForCollection(String collectionPid,
                                                   String angle,
                                                   String state,
                                                   int offset,
                                                   int limit,
                                                   FedoraConnector fedoraConnector)
            throws
            FedoraIllegalContentException,
            FedoraConnectionException,
            InvalidCredentialsException {
        LOG.trace("Entering getAllEntryObjectsForCollection with params '" +
                  collectionPid + "' and '" + angle+"' and '"+offset+"' and '"+limit+"'");


        collectionPid = sanitizePid(collectionPid);

        angle = sanitizeLiteral(angle);

        if (state == null){
            state = "<fedora-model:Active>";
        } else if (state.equals("I")){
            state = "<fedora-model:Inactive>";
        } else {
            state = "<fedora-model:Active>";
        }

        String domsnamespace
                = "http://doms.statsbiblioteket.dk/relations/default/0/1/#";

        String query = "select $object $cm $date\n"
                       + "from <#ri>\n"
                       + "where\n"
                       + "$object <fedora-model:hasModel> $cm\n"
                       + "and\n"
                       + "$cm <"+Constants.ENTRY_RELATION+"> '"+angle+"'\n"
                       + "and\n"
                       + "$object <"+domsnamespace+"isPartOfCollection> <info:fedora/"+collectionPid+">\n"
                       + "and\n"
                       + "$object <fedora-model:state> "+state+"\n"
                       + "and\n"
                       + "$object <fedora-view:lastModifiedDate> $date \n"
                       + "order by $date";
        if (offset != 0){
            query = query+"\n offset "+offset;
        }
        if (limit != 0){
            query = query + "\n limit "+limit;
        }


        LOG.debug("Using query \n'" + query + "'\n");
        return fedoraConnector.query(query);

    }

*/

}
