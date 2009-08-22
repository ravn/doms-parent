package net.sourceforge.ecm.validator;

import org.w3c.dom.Document;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a datastream description from a content model.
 */
public class Datastream {
    /** Name of datastream. */
    private String name;

    /** Mimetype of datastream, as declared in DS-COMPOSITE. May be null, if
     * not set. */
    private List<String> mimetypes = new ArrayList<String>();

    /** Format URI of datastream as declared in DS-COMPOSITE. May be null, if
     * not set. */
    private List<URI> formatUris = new ArrayList<URI>();

    /**
     * XML Schema for datastream as declared in DS-COMPOSITE with ECM
     * extension. May be null, for no set schema.
     */
    private Document xmlSchema = null;


    /**
     * Get name of datastream.
     *
     * @return Name of datastream.
     */
    public String getName() {
        return name;
    }

    /**
     * Set name of datastream.
     *
     * @param name Name of datastream.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get mimetype of datastream, as declared in DS-COMPOSITE.
     *
     * @return Mimetype of datastream, as declared in DS-COMPOSITE.
     */
    public List<String> getMimetypes() {
        return mimetypes;
    }

    /**
     * Set mimetype of datastream, as declared in DS-COMPOSITE.
     * @param mimetypes Mimetype of datastream, as declared in DS-COMPOSITE.
     */
    public void setMimetypes(List<String> mimetypes) {
        this.mimetypes = mimetypes;
    }

    /**
     * Get format URI of datastream as declared in DS-COMPOSITE.
     * @return Format URI of datastream as declared in DS-COMPOSITE.
     */
    public List<URI> getFormatUris() {
        return formatUris;
    }

    /**
     * Set format URI of datastream as declared in DS-COMPOSITE.
     * @param formatUris Format URI of datastream as declared in DS-COMPOSITE.
     */
    public void setFormatUris(List<URI> formatUris) {
        this.formatUris = formatUris;
    }

    /**
     * Get XML Schema for datastream as declared in DS-COMPOSITE with ECM
     * extension.
     * @return XML Schema for datastream as declared in DS-COMPOSITE with ECM
     * extension.
     */
    public Document getXmlSchema() {
        return xmlSchema;
    }

    /**
     * Set XML Schema for datastream as declared in DS-COMPOSITE with ECM
     * extension.
     * @param xmlSchema XML Schema for datastream as declared in DS-COMPOSITE
     * with ECM extension.
     */
    public void setXmlSchema(Document xmlSchema) {
        this.xmlSchema = xmlSchema;
    }

}
