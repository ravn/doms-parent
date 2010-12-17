package dk.statsbiblioteket.doms.ecm.webservice;

import dk.statsbiblioteket.doms.ecm.repository.*;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.EcmException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.InitialisationException;
import dk.statsbiblioteket.doms.ecm.repository.utils.DocumentUtils;
import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import dk.statsbiblioteket.doms.ecm.services.templates.TemplateSubsystem;
import dk.statsbiblioteket.doms.ecm.services.view.ViewSubsystem;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.xml.bind.JAXB;
import javax.xml.transform.TransformerException;
import java.io.StringWriter;
import java.util.List;

                    
/**
 * This is the Class serving as entry point for the webservice api for ECM. This
 * class contain the JAX-RS annotations to make a REST interface.
 */
@Path("/")
public class Webservice {

    private static final Log log
            = LogFactory.getLog(Webservice.class);


    private ViewSubsystem view;
    private TemplateSubsystem temps;
    private FedoraConnector fedoraConnector;
    private PidGenerator pidGenerator;

    private boolean initialised = false;

    @Context
    private HttpServletRequest request;

    public Webservice() {
        view = new ViewSubsystem();
        temps = new TemplateSubsystem();

        //read the config protoperties from ConfigCollection and initialise pidGenerator
        String pidgeneratorclassString = ConfigCollection.getProperties()
                .getProperty("dk.statsbiblioteket.doms.ecm.pidGenerator.client");
        try {
            Class<?> pidgeneratorClass = Class.forName(pidgeneratorclassString);
            if (PidGenerator.class.isAssignableFrom(pidgeneratorClass)) {
                try {
                    pidGenerator = (PidGenerator) pidgeneratorClass.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else {//Class not implementing the correct interface

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //TODO bomb if pidgenerator cannot be made
    }

    private synchronized void initialise() throws InitialisationException {
/*
        if (initialised){
            return;
        }
*/
        String fedoraserverurl = ConfigCollection.getProperties().getProperty("dk.statsbiblioteket.doms.ecm.fedora.location");
        String fedoraconnectorclassstring = ConfigCollection.getProperties().getProperty("dk.statsbiblioteket.doms.ecm.fedora.connector");
        Credentials creds = null;
        creds = (Credentials) request.getAttribute("Credentials");
        if (creds == null) {
            log.warn("No credentials found, using empty creds");
            creds = new Credentials("", "");
        }
        try {
            Class<?> fedoraconnectorclass = Class.forName(fedoraconnectorclassstring);
            if (FedoraConnector.class.isAssignableFrom(fedoraconnectorclass)) {
                try {
                    fedoraConnector = (FedoraConnector) fedoraconnectorclass.newInstance();
                    fedoraConnector = new CachingConnector(fedoraConnector);
                    FedoraUserToken token = new FedoraUserToken(fedoraserverurl, creds.getUsername(), creds.getPassword());
                    fedoraConnector.initialise(token);
                } catch (InstantiationException e) {//TODO
                    throw new InitialisationException("Initialise failed", e);
                } catch (IllegalAccessException e) {//TODO
                    throw new InitialisationException("Initialise failed", e);
                }
            }
        } catch (ClassNotFoundException e) {//TODO
            throw new InitialisationException("Initialise failed", e);
        }
        initialised = true;
    }

    /*------------TEMPLATE METHODS---------------------*/

    @POST
    @Path("mark/{objectpid}/asTemplateFor/{cmpid}")
    public void markObjectAsTemplate(
            @PathParam("objectpid") String objpid,
            @PathParam("cmpid") String cmpid) throws EcmException {
        initialise();
        temps.markObjectAsTemplate(objpid, cmpid, fedoraConnector);
    }

    @GET
    @Path("findTemplatesFor/{cmpid}")
    @Produces("text/xml")
    public PidList findTemplatesFor(
            @PathParam("cmpid") String cmpid) throws EcmException {
        log.trace("Entering findTemplatesFor with cmpid='" + cmpid + "'");
        initialise();
        return temps.findTemplatesFor(cmpid, fedoraConnector);
    }

    @POST
    @Path("clone/{templatepid}")
    @Produces("text/plain")
    public String cloneTemplate(
            @PathParam("templatepid") String templatepid,
            @QueryParam("oldID") List<String> oldIDs) throws EcmException {
        initialise();
        return temps.cloneTemplate(templatepid, oldIDs, fedoraConnector, pidGenerator);
    }


    /*-----------VIEW METHODS ------------------------*/
    @GET
    @Path("getEntryContentModelsForAngle/{viewAngle}")
    @Produces("text/xml")
    public PidList getEntryCMsForAngle(
            @PathParam("viewAngle") String viewAngle) throws EcmException {
        initialise();
        return view.getEntryCMsForAngle(viewAngle, fedoraConnector);
    }


    @GET
    @Path("getObjectsForContentModel/{cmpid}")
    @Produces("text/xml")
    public PidList getObjectsForContentModel(
            @PathParam("cmpid") String cmpid,
            @DefaultValue("Active") @QueryParam("state") String status)
            throws EcmException {
        initialise();
        return view.getObjectsForContentModel(cmpid, status, fedoraConnector);
    }

    @GET
    @Path("getEntryObjectsForViewAngle/{viewAngle}")
    @Produces("text/xml")
    public PidList getEntriesForAngle(@PathParam("viewAngle") String viewAngle,
                                      @DefaultValue("Active")
                                      @QueryParam("state") String state)
            throws EcmException {
        initialise();
        return view.getEntriesForAngle(viewAngle, state, fedoraConnector);
    }

    @GET
    @Path("getViewObjectsForObject/{objpid}/forAngle/{viewAngle}")
    @Produces("text/xml")
    public String getViewObjectsBundleForObject(
            @PathParam("objpid") String objectpid,
            @PathParam("viewAngle") String viewAngle,
            @QueryParam("bundle") @DefaultValue("false") boolean bundle)
            throws EcmException {
        initialise();
        if (bundle) {
            //Return a string, as the two different return formats
            //confuse java
            Document dobundle = view.getViewObjectBundleForObject(
                    objectpid,
                    viewAngle,
                    fedoraConnector);
            try {
                return DocumentUtils.documentToString(dobundle);
            } catch (TransformerException e) {
                throw new FedoraIllegalContentException(
                        "Failed to bundle the objects",
                        e);
            }

        } else {
            PidList list = view.getViewObjectsListForObject(
                    objectpid,
                    viewAngle,
                    fedoraConnector);
            StringWriter writer = new StringWriter();
            JAXB.marshal(list, writer);
            return writer.toString();

        }
    }

    @GET
    @Path("getContentModelsForObject/{objpid}")
    @Produces("text/xml")
    public PidList getContentModels(@PathParam("objpid") String objpid)
            throws EcmException {
        initialise();
        return new PidList(fedoraConnector.getContentModels(FedoraUtil.ensurePID(objpid)));
    }

    @GET
    @Path("getEntryContentModelsForObject/{objpid}/forAngle/{viewAngle}")
    @Produces("text/xml")
    public PidList getEntryContentModelsForObjectForAngle(@PathParam("objpid") String objpid,
                                                          @PathParam("viewAngle") String viewAngle)
            throws EcmException {
        initialise();

        return new PidList(view.getEntryContentModelsForObjectForViewAngle(FedoraUtil.ensurePID(objpid), viewAngle, fedoraConnector));
    }

    /*@GET
    @Path("getAllEntryObjectsForCollection/{collectionpid}/forAngle/"
            + "{viewAngle}")
    @Produces("text/xml")
    public PidList getAllEntryObjectsForCollection(
            @PathParam("collectionpid") String collectionPid,
            @PathParam("viewAngle") String viewAngle,
            @QueryParam("state") String state,
            @QueryParam("offset") int offset,
            @QueryParam("limit") int limit)
            throws EcmException {
        initialise();

        return new PidList(view.getAllEntryObjectsForCollection(collectionPid,
                entryCMpid, viewAngle, state, fedoraConnector));
    }
*/

}
