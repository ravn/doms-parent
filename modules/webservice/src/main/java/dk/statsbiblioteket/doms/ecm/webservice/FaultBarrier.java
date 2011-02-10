package dk.statsbiblioteket.doms.ecm.webservice;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.core.Response;

/**
 * TODO abr forgot to document this class
 */
public class FaultBarrier implements ExceptionMapper<Exception>{



    //TODO
    public Response toResponse(Exception e) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
