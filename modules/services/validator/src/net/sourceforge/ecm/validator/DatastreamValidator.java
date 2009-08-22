package net.sourceforge.ecm.validator;

import net.sourceforge.ecm.exceptions.DatastreamNotFoundException;
import net.sourceforge.ecm.exceptions.FedoraConnectionException;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.exceptions.ObjectNotFoundException;
import net.sourceforge.ecm.repository.Repository;
import net.sourceforge.ecm.repository.ValidationResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;


/**
 * Class for validating datastreams against schemas
 */
public class DatastreamValidator implements Validator{


    /**
     * Validate the datastreams in object pid against the defined datastreams
     * and their schemas in content model cm
     * @param pid the object to validate
     * @param cm the merged content models with the schemas
     * @return a validationResult
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     */
    public ValidationResult validate(String pid, CompoundContentModel cm)
            throws FedoraConnectionException,
                   FedoraIllegalContentException,
                   ObjectNotFoundException {

        ValidationResult result = new ValidationResult();

        for (Datastream ds : cm.getDatastreams()) {

            if (ds.getXmlSchema() != null) {

                Document dsContents;
                try {
                    dsContents = Repository
                            .getDatastream(pid, ds.getName());
                } catch (DatastreamNotFoundException e) {
                    result.setValid(false);
                    result.add("Object " + pid +
                               " does not have the required datastream " +
                               ds.getName());
                    continue;
                }

                ValidationResult localresult = validateDocument(
                        ds.getXmlSchema(), dsContents, ds.getName(), pid);
                result = result.combine(localresult);
            }

        }

        return result;


    }

    /**
     * Validate a datastream against the schema for said datastream
     * @param xmlSchema The schema, as a DOM document
     * @param dsContents The datastream, as a DOM document
     * @param name the name of the datastream. Only used for error reporting
     * @param pid The pid of the object. Only used for error reporting
     * @return A ValidationResult object, with the problems.
     */
    private ValidationResult validateDocument(Document xmlSchema,
                                              Document dsContents,
                                              String name,
                                              String pid) {

        ValidationResult result = new ValidationResult();

        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new DOMSource(xmlSchema));
        } catch (SAXException e) {
            result.setValid(false);
            result.add(
                    "Content Model error: Unable to parse the validation "
                    + "schema for Object '" + pid +
                    "', datastream '" + name + "'. "
                    + e.getMessage() + "(" + e.getClass() + ")");
            return result;
        }
        try {
            schema.newValidator().validate(new DOMSource(dsContents));
        } catch (SAXException e) {
            result.setValid(false);
            result.add(
                    "Data error: Invalid content in Object '" + pid +
                    "', datastream '" + name
                    + "'. " + e.getMessage() + "(" + e.getClass()
                    + ")");
        } catch (IOException e) {
            result.setValid(false);
            result.add(
                    "Data error: Unable to read Object '" +"', datastream '" +
                    name + "'. " + e.getMessage() +
                    "(" + e.getClass() + ")");
        }
        return result;
    }

}
