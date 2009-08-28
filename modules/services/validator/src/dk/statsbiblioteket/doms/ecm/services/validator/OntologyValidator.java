package dk.statsbiblioteket.doms.ecm.services.validator;

import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.Repository;
import dk.statsbiblioteket.doms.ecm.repository.ValidationResult;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.InvalidOntologyException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException;
import dk.statsbiblioteket.doms.ecm.repository.utils.DocumentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.StringInputSource;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.util.OWLDescriptionVisitorAdapter;

import javax.xml.transform.TransformerException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 Validates data objects against the ontology defined in content model;s
 */
public class OntologyValidator implements Validator{

    private static final Log LOG = LogFactory.getLog(OntologyValidator.class);

    /**
     * Validate the pid object against the ontology from the cm object
     * @param pid the object to validate
     * @param cm the content model with the specification to validate against
     * @return a validation result
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     */
    public ValidationResult validate(
            String pid,
            CompoundContentModel cm)
            throws FedoraConnectionException, FedoraIllegalContentException, ObjectNotFoundException {

        //TODO how about a general check for relations to non-existant objects?
        LOG.trace("Validating ontology for object '" + pid + "'");
        List<String> problems = new ArrayList<String>();
        boolean valid = true;

        //parse compound ontology into java
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        String ontostring;

        try {
            ontostring = DocumentUtils.documentToString(cm.getOntology());
            LOG.debug(ontostring);
        } catch (TransformerException e) {
            throw new FedoraIllegalContentException("Invalid xml content in ontology",e);
        }

        // Load the combined ontology from the Compound content model
        OWLOntology ont;
        try {
            ont = man.loadOntology(
                    new StringInputSource(ontostring)
                    );
        } catch (OWLOntologyCreationException e) {
            throw new InvalidOntologyException(
                    "The combined ontology for this object is not valid", e);
        }

        //Make a new restrition visitor.
        RestrictionVisitor restrictionVisitor =
                new RestrictionVisitor(Collections.singleton(ont));

        //Get the classes of this object
        List<String> contentmodels = cm.getPids();

        //For each class
        for (String contentmodel: contentmodels){
            LOG.trace("Visiting the content model '" + contentmodel + "'");

            URI classUri = URI.create(Repository.ensureURI(contentmodel));
            OWLClass owlClass = man.getOWLDataFactory().getOWLClass(classUri);
            for(OWLSubClassAxiom ax : ont.getSubClassAxiomsForLHS(owlClass)) {
                //for all the subclass restrictions
                OWLDescription superCls = ax.getSuperClass();
                //get the class (ie the content model that defined this restriction
                superCls.accept(restrictionVisitor);//collect all the restrictions in this content model
            }

        }
        // Our RestrictionVisitor has now collected all of the properties that have been restricted


        boolean valid1 = checkMinCardinality(pid, problems, restrictionVisitor);
        boolean valid2 = checkMaxCardinality(pid, problems, restrictionVisitor);
        boolean valid3 = checkCardinality(pid, problems, restrictionVisitor);
        boolean valid4 = checkAllValuesFrom(pid, problems, restrictionVisitor);
        boolean valid5 = checkSomeValuesFrom(pid, problems, restrictionVisitor);
        valid = valid1 && valid2 && valid3 && valid4 && valid5;

        return new ValidationResult(valid, problems);

    }

    private boolean checkSomeValuesFrom(String pid, List<String> problems,
                                        RestrictionVisitor restrictionVisitor)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        boolean valid = true;
        Map<OWLObjectProperty,OWLClass> valuerestrictedproperties;
        valuerestrictedproperties = restrictionVisitor.getSomeValuesFrom();
        for(Map.Entry<OWLObjectProperty,OWLClass> prop :
                valuerestrictedproperties.entrySet()) {
            String relation = prop.getKey().getURI().toString();

            List<FedoraConnector.Relation> relations = Repository.getRelations(
                    Repository.ensurePID(pid),
                    relation);

            if (relations.size() < 1) {
                valid = false;
                problems.add("Object '"+pid+"' have 0 of " + relation +
                             "' relations but must have more than 1");
                continue;
            }

            String reqcm = Repository.ensureURI(prop.getValue().toString());
            boolean foundObject = false;
            for (FedoraConnector.Relation rel: relations){

                String target = rel.getTo();
                List<String> targetType;
                try {
                    targetType = Repository.getContentModels(target);
                } catch (ObjectNotFoundException e) {
                    valid = false;
                    problems.add("Object '" + pid + "' has a relation '" +
                                 relation + "' to object '" + target +
                                 "' which does not exist");
                    LOG.debug("Caught exception and discarded it",e);
                    continue;

                }

                if (targetType.contains(reqcm)){
                    foundObject = true;
                    break;
                }
            }
            if (!foundObject){
                valid=false;
                problems.add("Object '" + pid + "' must have at least one " +
                             "relation '" + relation + "'to an object with " +
                             "content model '" + reqcm + "'");
            }

        }
        return valid;
    }

    private boolean checkAllValuesFrom(String pid, List<String> problems,
                                       RestrictionVisitor restrictionVisitor)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        boolean valid = true;
        Map<OWLObjectProperty,OWLClass> valuerestrictedproperties;
        valuerestrictedproperties = restrictionVisitor.getAllValuesFrom();
        for(Map.Entry<OWLObjectProperty,OWLClass> prop :
                valuerestrictedproperties.entrySet()) {
            String relation = prop.getKey().getURI().toString();

            List<FedoraConnector.Relation> relations = Repository.getRelations(
                    Repository.ensurePID(pid), relation);

            String reqcm = Repository.ensureURI(prop.getValue().toString());

            for (FedoraConnector.Relation relation_: relations){
                String target = relation_.getTo();
                List<String> targetType;
                try {
                    targetType = Repository.getContentModels(target);
                } catch (ObjectNotFoundException e) {
                    valid = false;
                    problems.add("Object '" + pid + "' has a relation '" +
                                 relation + "'to object '" + target +
                                 "' which does not exist");
                    LOG.debug("Caught exception and discarded it", e);
                    continue;
                }

                if (!targetType.contains(reqcm)){
                    valid = false;
                    problems.add("Object '" + pid + "' have a relation to " +
                                 "object '" + target + "', which must have the" +
                                 " content model '" + reqcm + "'");
                }
            }
        }
        return valid;
    }

    private boolean checkCardinality(String pid, List<String> problems,
                                     RestrictionVisitor restrictionVisitor)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        boolean valid = true;
        Map<OWLObjectProperty, Integer> restrictedProperties;
        restrictedProperties = restrictionVisitor.getCardinality();
        for(Map.Entry<OWLObjectProperty,Integer> prop :
                restrictedProperties.entrySet()) {
            String relation = prop.getKey().getURI().toString();

            List<FedoraConnector.Relation> relations = Repository.getRelations(
                    Repository.ensurePID(pid), relation);

            if (relations.size() < prop.getValue()) {
                int number = relations.size();
                valid = false;
                problems.add("Object '" + pid + "' have  '" + number +
                             " of " + relation + "' relations but must have " +
                             "exactly " + prop.getValue());
            }
        }
        return valid;
    }

    private boolean checkMaxCardinality(String pid, List<String> problems,
                                        RestrictionVisitor restrictionVisitor)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        boolean valid = true;
        Map<OWLObjectProperty, Integer> restrictedProperties;
        restrictedProperties = restrictionVisitor.getMaxCardinality();
        for(Map.Entry<OWLObjectProperty,Integer> prop :
                restrictedProperties.entrySet()) {
            String relation = prop.getKey().getURI().toString();

            List<FedoraConnector.Relation> relations
                    = Repository.getRelations(
                    Repository.ensurePID(pid),
                    relation);

            if (relations.size() < prop.getValue()) {
                int number = relations.size();
                valid = false;
                problems.add("Object '" + pid + "' have  '" + number +
                             " of " + relation + "' relations but must have " +
                             "less than " + prop.getValue());
            }
        }
        return valid;
    }

    private boolean checkMinCardinality(String pid, List<String> problems,
                                        RestrictionVisitor restrictionVisitor)
            throws ObjectNotFoundException, FedoraConnectionException,
                   FedoraIllegalContentException {
        boolean valid = true;
        //Reused variable for all the cardinality restrictions
        Map<OWLObjectProperty,Integer> restrictedProperties;

        //Process the min-cardinality restrictions
        restrictedProperties = restrictionVisitor.getMinCardinality();

        for(Map.Entry<OWLObjectProperty, Integer> prop :
                restrictedProperties.entrySet()) {
            String relation = prop.getKey().getURI().toString();


            List<FedoraConnector.Relation> relations
                    = Repository.getRelations(
                    Repository.ensurePID(pid),
                    relation);

            if (relations.size() < prop.getValue()) {
                valid = false;
                problems.add("Object '"+pid+"' have  "+ relations.size()
                             +" of '"+relation+"' relations but must have more than "
                             +prop.getValue());
            }
        }
        return valid;
    }


    /**
     * Visits restrictions and collects the properties which are restricted
     */
    private static class RestrictionVisitor extends OWLDescriptionVisitorAdapter {

        private boolean processInherited = true;

        private Set<OWLClass> processedClasses;


        private Map<OWLObjectProperty,OWLClass> someValuesFrom;
        private Map<OWLObjectProperty,OWLClass> allValuesFrom;
        private Map<OWLObjectProperty, Integer> minCardinality;
        private Map<OWLObjectProperty, Integer> cardinality;
        private Map<OWLObjectProperty, Integer> maxCardinality;


        private Set<OWLOntology> onts;

        public RestrictionVisitor(Set<OWLOntology> onts) {
            someValuesFrom = new HashMap<OWLObjectProperty,OWLClass>();
            allValuesFrom = new HashMap<OWLObjectProperty,OWLClass>();
            minCardinality= new HashMap<OWLObjectProperty,Integer>();
            cardinality = new HashMap<OWLObjectProperty,Integer>();
            maxCardinality = new HashMap<OWLObjectProperty,Integer>();

            processedClasses = new HashSet<OWLClass>();
            this.onts = onts;
        }


        public void setProcessInherited(boolean processInherited) {
            this.processInherited = processInherited;
        }



        public void visit(OWLClass desc) {

            if(processInherited && !processedClasses.contains(desc)) {
                // If we are processing inherited restrictions then
                // we recursively visit named supers.  Note that we
                // need to keep track of the classes that we have processed
                // so that we don't get caught out by cycles in the taxonomy
                processedClasses.add(desc);
                for(OWLOntology ont : onts) {
                    for(OWLSubClassAxiom ax : ont.getSubClassAxiomsForLHS(desc)) {
                        ax.getSuperClass().accept(this);
                    }
                }
            }
        }


        public void reset() {
            processedClasses.clear();
            someValuesFrom.clear();
            allValuesFrom.clear();
            minCardinality.clear();
            cardinality.clear();
            maxCardinality.clear();
        }


        public void visit(OWLObjectExactCardinalityRestriction desc){

            //TODO OwlOntException to IllegalOntologyException
            cardinality.put(desc.getProperty().asOWLObjectProperty(),desc.getCardinality());
        }

        public void visit(OWLObjectMaxCardinalityRestriction desc){

            maxCardinality.put(desc.getProperty().asOWLObjectProperty(),desc.getCardinality());
        }

        public void visit(OWLObjectMinCardinalityRestriction desc){
            minCardinality.put(desc.getProperty().asOWLObjectProperty(),desc.getCardinality());
        }

        public void visit(OWLObjectAllRestriction desc){
            OWLDescription filler = desc.getFiller();
            allValuesFrom.put(desc.getProperty().asOWLObjectProperty(),filler.asOWLClass());
        }

        public void visit(OWLObjectSomeRestriction desc){
            OWLDescription filler = desc.getFiller();
            allValuesFrom.put(desc.getProperty().asOWLObjectProperty(),filler.asOWLClass());
        }


        public void visit(OWLDataSomeRestriction desc) {
        }


        public void visit(OWLDataAllRestriction desc) {
        }


        public void visit(OWLDataMinCardinalityRestriction desc) {

        }


        public void visit(OWLDataExactCardinalityRestriction desc) {
        }


        public void visit(OWLDataMaxCardinalityRestriction desc) {
        }




        public Map<OWLObjectProperty, OWLClass> getSomeValuesFrom() {
            return someValuesFrom;
        }

        public Map<OWLObjectProperty, OWLClass> getAllValuesFrom() {
            return allValuesFrom;
        }

        public Map<OWLObjectProperty, Integer> getMinCardinality() {
            return minCardinality;
        }

        public Map<OWLObjectProperty, Integer> getCardinality() {
            return cardinality;
        }

        public Map<OWLObjectProperty, Integer> getMaxCardinality() {
            return maxCardinality;
        }
    }

}
