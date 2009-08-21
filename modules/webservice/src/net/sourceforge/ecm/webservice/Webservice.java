package net.sourceforge.ecm.webservice;

import net.sourceforge.ecm.exceptions.EcmException;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.repository.PidList;
import net.sourceforge.ecm.repository.ValidationResult;
import net.sourceforge.ecm.repository.Repository;
import net.sourceforge.ecm.templates.TemplateSubsystem;
import net.sourceforge.ecm.utils.DocumentUtils;
import net.sourceforge.ecm.validator.ValidatorSubsystem;
import net.sourceforge.ecm.view.ViewSubsystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import javax.ws.rs.*;
import javax.xml.bind.JAXB;
import javax.xml.transform.TransformerException;

import java.io.StringWriter;

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
    private ValidatorSubsystem validator;


    public Webservice() {
        view = new ViewSubsystem();
        temps = new TemplateSubsystem();
        validator = new ValidatorSubsystem();

    }

    /*----------VALIDATE METHODS -------------------*/
    @GET
    @Path("validate/{objectpid}")
    @Produces("text/xml")
    public ValidationResult validate(
            @PathParam("objectpid") String objpid
    ) throws EcmException {
        log.trace("Entering validate with objpid='"+objpid+"'");
        return validator.validate(objpid);
    }

    @GET
    @Path("validate/{objectpid}/against/{ecmpid}")
    @Produces("text/xml")
    public ValidationResult validateAgainst(
            @PathParam("objectpid") String objpid,
            @PathParam("ecmpid") String cmpid
    ) throws EcmException {
        log.trace("Entering validate with objpid='"
                  +objpid+"' and cmpid='"+cmpid+"'");
        return validator.validateAgainst(objpid,cmpid);
    }


    /*------------TEMPLATE METHODS---------------------*/

    @POST
    @Path("mark/{objectpid}/asTemplateFor/{cmpid}")
    public void markObjectAsTemplate(
            @PathParam("objectpid") String objpid,
            @PathParam("cmpid") String cmpid) throws EcmException {
        temps.markObjectAsTemplate(objpid,cmpid);
    }

    @GET
    @Path("findTemplatesFor/{cmpid}")
    @Produces("text/xml")
    public PidList findTemplatesFor(
            @PathParam("cmpid") String cmpid) throws EcmException {
        log.trace("Entering findTemplatesFor with cmpid='"+cmpid+"'");

        return temps.findTemplatesFor(cmpid);
    }

    @POST
    @Path("clone/{templatepid}")
    @Produces("text/plain")
    public String cloneTemplate(
            @PathParam("templatepid") String templatepid) throws EcmException {
        return temps.cloneTemplate(templatepid);
    }



    /*-----------VIEW METHODS ------------------------*/
    @GET
    @Path("getEntryContentModelsForAngle/{viewAngle}")
    @Produces("text/xml")
    public PidList getEntryCMsForAngle(
            @PathParam("viewAngle") String viewAngle) throws EcmException {
        return view.getEntryCMsForAngle(viewAngle);
    }


    @GET
    @Path("getObjectsForContentModel/{cmpid}")
    @Produces("text/xml")
    public PidList getObjectsForContentModel(
            @PathParam("cmpid") String cmpid,
            @DefaultValue("Active") @QueryParam("state") String status)
            throws EcmException {
        return view.getObjectsForContentModel(cmpid,status);
    }

    @GET
    @Path("getEntryObjectsForViewAngle/{viewAngle}")
    @Produces("text/xml")
    public PidList getEntriesForAngle(@PathParam("viewAngle") String viewAngle,
                                      @DefaultValue("Active")
                                      @QueryParam("state") String state)
            throws EcmException {
        return view.getEntriesForAngle(viewAngle,state);
    }

    @GET
    @Path("getViewObjectsForObject/{objpid}/forAngle/{viewAngle}")
    @Produces("text/xml")
    public String getViewObjectsBundleForObject(
            @PathParam("objpid") String objectpid,
            @PathParam("viewAngle") String viewAngle,
            @QueryParam("bundle") @DefaultValue("false") boolean bundle)
            throws EcmException {
        if (bundle){
            //Return a string, as the two different return formats
            //confuse java
            Document dobundle = view.getViewObjectBundleForObject(
                    objectpid,
                    viewAngle);
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
                    viewAngle);
            StringWriter writer = new StringWriter();
            JAXB.marshal(list,writer);
            return  writer.toString();

        }
    }

    @GET
    @Path("getContentModelsForObject/{objpid}")
    @Produces("text/xml")
    public PidList getContentModels(@PathParam("objpid") String objpid)
            throws EcmException{
        return new PidList(Repository.getContentModels(Repository.ensurePID(objpid)));
    }

}
