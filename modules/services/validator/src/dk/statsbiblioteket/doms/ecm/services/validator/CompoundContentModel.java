package dk.statsbiblioteket.doms.ecm.services.validator;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectIsWrongTypeException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class describing a compund content model, that is the result of combining
 * information in all content models for an object.
 */
public class CompoundContentModel {
    /**
     * A list of content model pids used to generate the compound content
     * model, in the order they were resolved.
     */
    List<String> pids;

    /** A list of datastreams defined by the model. */
    List<Datastream> datastreams;

    /** The ontology defined by the model. */
    Document ontology;


    /**
     * Factory method. Create a Compound Content Model object from the content
     * models in a data object
     * @param pid the data object
     * @return the compound object with all the content models for the data object
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     * @throws ObjectIsWrongTypeException
     */
    public static CompoundContentModel loadFromDataObject(String pid, FedoraConnector fedoraConnector)
            throws FedoraConnectionException, FedoraIllegalContentException,
                   ObjectNotFoundException, ObjectIsWrongTypeException {
        return ContentModelUtils.getCompoundContentModel(pid, fedoraConnector);
    }

    /**
     * Factory method. Create a compound object from a content model, with
     * all it's ancestors
     * @param pid the content model to begin with
     * @return the compound object of this content model
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     * @throws ObjectIsWrongTypeException
     */
    public static CompoundContentModel loadFromContentModel(String pid, FedoraConnector fedoraConnector)
            throws FedoraConnectionException, FedoraIllegalContentException,
                   ObjectNotFoundException, ObjectIsWrongTypeException {
        return ContentModelUtils.getAsCompoundContentModel(pid,fedoraConnector);
    }
    

    /**
     * Initialise a content model.
     *
     * @throws RuntimeException on trouble generating document.
     */
    CompoundContentModel() {
        pids = new ArrayList<String>();
        datastreams = new ArrayList<Datastream>();
        try {
            ontology = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new Error(
                    "Error initialising default document builder",
                    e);
        }
        ontology.appendChild(ontology.createElementNS(
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF"));

    }

    /**
     * Get the list of content model pids used to generate the compound content
     * model, in the order they were resolved.
     * @return A list of pids.
     */
    public List<String> getPids() {
        return pids;
    }

    /**
     * Set the list of content model pids used to generate the compound content
     * model, in the order they were resolved.
     * @param pids A list of pids.
     */
    public void setPids(List<String> pids) {
        this.pids = pids;
    }

    /**
     * Get the list of datastreams defined by the model.
     * @return The list of datastreams defined by the model.
     */
    public List<Datastream> getDatastreams() {
        return datastreams;
    }

    /**
     * Set the list of datastreams defined by the model.
     * @param datastreams The list of datastreams defined by the model.
     */
    public void setDatastreams(List<Datastream> datastreams) {
        this.datastreams = datastreams;
    }

    /**
     * Get the ontology defined by the model.
     * @return The ontology defined by the model.
     */
    public Document getOntology() {
        return ontology;
    }

    /**
     * Set the ontology defined by the model.
     * @param ontology The ontology defined by the model.
     */
    public void setOntology(Document ontology) {
        this.ontology = ontology;
    }

}
