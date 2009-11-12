package dk.statsbiblioteket.doms.ecm.webservice;

import org.springframework.stereotype.Service;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.PidGenerator;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Nov 12, 2009
 * Time: 4:06:04 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class TransportBean {

    private FedoraConnector fedoraConnector;


    private PidGenerator pidGenerator;


    public FedoraConnector getFedoraConnector() {
        return fedoraConnector;
    }

    public void setFedoraConnector(FedoraConnector fedoraConnector) {
        this.fedoraConnector = fedoraConnector;
    }

    public PidGenerator getPidGenerator() {
        return pidGenerator;
    }

    public void setPidGenerator(PidGenerator pidGenerator) {
        this.pidGenerator = pidGenerator;
    }
}
