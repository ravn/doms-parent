package dk.statsbiblioteket.doms.ecm.webservice;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.EcmException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;

/**
 * Simple class to map EcmExceptions to webservice Responce's.
 *
 * The @Provider annotation means that this class is picked up as a
 * exceptionmapper. 
 */
@Provider
public class EcmFaultBarrier
        implements ExceptionMapper<EcmException> {

    private static final Log LOG = LogFactory.getLog(EcmFaultBarrier.class);

    /**
     * Maps a EcmException to a Responce
     * @param e the exception
     * @return the responce
     */
    public Response toResponse(EcmException e) {
        StringWriter strw = new StringWriter();

        //TODO Fault barrier
        if (e instanceof FedoraConnectionException){
            LOG.warn("caught exception:",e);
        } else {
            LOG.debug("caught exception",e);
        }


        JAXB.marshal(new Cleaned(e), strw);
        return Response.serverError().entity(
               strw.toString()).build();
    }

    /**
     * Simple class, that provides JAXB annotations to a EcmException
     */
    @XmlRootElement(name="failure")
    private static class Cleaned {
        @XmlElement
        public String errorType;

        @XmlElement
        public String message;

        private Cleaned() {
        }

        private Cleaned(EcmException e) {
            this.message = e.getMessage();
            errorType = e.getClass().getSimpleName();
        }
    }
}
