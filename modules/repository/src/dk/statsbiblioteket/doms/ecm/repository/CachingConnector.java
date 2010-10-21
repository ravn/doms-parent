package dk.statsbiblioteket.doms.ecm.repository;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.*;
import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Oct 21, 2010
 * Time: 4:54:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class CachingConnector implements FedoraConnector{

    private FedoraConnector connector;

    /**
     * The static contentmodel caches. These should not be protected, so we do
     * not care about specific user creds here
     */
    private static TimeSensitiveCache<String,PidList> inheritedContentModels
            = new TimeSensitiveCache<String,PidList>(1000*60*30,true,20);
    private static TimeSensitiveCache<String,PidList> inheritingContentModels
            = new TimeSensitiveCache<String,PidList>(1000*60*30,true,20);
    private static TimeSensitiveCache<String,PidList> contentModels
            = new TimeSensitiveCache<String,PidList>(1000*60*30,true,200);

    /**
     * This is the blob of user specific caches. Note that this is itself a cache
     * so it will be garbage collected
     */
    private static TimeSensitiveCache<FedoraUserToken,Caches> userspecificCaches
            = new TimeSensitiveCache<FedoraUserToken,Caches>(1000*60*30,true,20);

    /**
     * My specific cache.
     */
    private Caches myCaches;


    public CachingConnector(FedoraConnector connector) {
        this.connector = connector;
    }

    public void initialise(FedoraUserToken token) {
        connector.initialise(token);
        myCaches = userspecificCaches.get(token);
        if (myCaches == null){
            myCaches = new Caches();
            userspecificCaches.put(token, myCaches);
        }

    }

    public boolean exists(String pid) throws
                                      IllegalStateException,
                                      FedoraIllegalContentException,
                                      FedoraConnectionException,
                                      InvalidCredentialsException {
        return connector.exists(pid);
    }

    public boolean isDataObject(String pid) throws
                                            IllegalStateException,
                                            FedoraIllegalContentException,
                                            FedoraConnectionException,
                                            InvalidCredentialsException {
        return connector.isDataObject(pid);
    }

    public boolean isTemplate(String pid) throws
                                          IllegalStateException,
                                          ObjectNotFoundException,
                                          FedoraConnectionException,
                                          FedoraIllegalContentException,
                                          InvalidCredentialsException {
        return connector.isTemplate(pid);
    }

    public boolean isContentModel(String pid) throws
                                              IllegalStateException,
                                              FedoraIllegalContentException,
                                              FedoraConnectionException,
                                              InvalidCredentialsException {
        return connector.isContentModel(pid);
    }

    public PidList query(String query) throws
                                       IllegalStateException,
                                       FedoraConnectionException,
                                       FedoraIllegalContentException,
                                       InvalidCredentialsException {
        return connector.query(query);
    }

    public boolean addRelation(String from, String relation, String to) throws
                                                                        IllegalStateException,
                                                                        ObjectNotFoundException,
                                                                        FedoraConnectionException,
                                                                        FedoraIllegalContentException,
                                                                        InvalidCredentialsException {

        myCaches.removeRelations(FedoraUtil.ensurePID(from));
        return connector.addRelation(from, relation, to);
    }

    public boolean addLiteralRelation(String from,
                                      String relation,
                                      String value, String datatype) throws
                                                                     IllegalStateException,
                                                                     ObjectNotFoundException,
                                                                     FedoraConnectionException,
                                                                     FedoraIllegalContentException,
                                                                     InvalidCredentialsException {
        myCaches.removeRelations(FedoraUtil.ensurePID(from));
        return connector.addLiteralRelation(from, relation, value, datatype);
    }

    public Document getObjectXml(String pid) throws
                                             IllegalStateException,
                                             ObjectNotFoundException,
                                             FedoraConnectionException,
                                             FedoraIllegalContentException,
                                             InvalidCredentialsException {
        pid = FedoraUtil.ensurePID(pid);
        Document doc = myCaches.getObjectXML(pid);
        if (doc != null){
            return doc;
        }
        doc = connector.getObjectXml(pid);
        myCaches.storeObjectXML(pid,doc);
        return doc;
    }

    public String ingestDocument(Document newobject, String logmessage) throws
                                                                        IllegalStateException,
                                                                        FedoraConnectionException,
                                                                        FedoraIllegalContentException,
                                                                        InvalidCredentialsException {
        return connector.ingestDocument(newobject, logmessage);
    }

    public List<Relation> getRelations(String pid) throws
                                                   IllegalStateException,
                                                   FedoraConnectionException,
                                                   ObjectNotFoundException,
                                                   FedoraIllegalContentException,
                                                   InvalidCredentialsException {
        pid = FedoraUtil.ensurePID(pid);
        List<Relation> relations = myCaches.getRelations(pid);
        if (relations != null){
            return relations;
        }
        relations = connector.getRelations(pid);
        myCaches.storeRelations(pid,relations);
        return relations;
    }

    public List<Relation> getRelations(String pid,
                                       String relation) throws
                                                        IllegalStateException,
                                                        FedoraConnectionException,
                                                        ObjectNotFoundException,
                                                        FedoraIllegalContentException,
                                                        InvalidCredentialsException {
        List<Relation> relations = getRelations(pid);
        List<Relation> result = new ArrayList<Relation>();
        for (Relation relation1 : relations) {
            if (relation1.getRelation().equals(relation)){
                result.add(relation1);
            }
        }
        return result;
    }

    public Document getDatastream(String pid, String dsid) throws
                                                           IllegalStateException,
                                                           DatastreamNotFoundException,
                                                           FedoraConnectionException,
                                                           FedoraIllegalContentException,
                                                           ObjectNotFoundException,
                                                           InvalidCredentialsException {
        Document doc = myCaches.getDatastreamContents(pid, dsid);
        if (doc != null){
            return doc;
        }
        doc =  connector.getDatastream(pid, dsid);
        myCaches.storeDatastreamContents(pid,dsid,doc);
        return doc;
    }

    public PidList getContentModels(String pid) throws
                                                IllegalStateException,
                                                FedoraConnectionException,
                                                ObjectNotFoundException,
                                                FedoraIllegalContentException,

                                                InvalidCredentialsException {
        pid = FedoraUtil.ensureURI(pid);
        PidList models =  contentModels.get(pid);
        if (models != null){
            return models;
        }
        models =  connector.getContentModels(pid);
        contentModels.put(pid,models);
        return models;

    }

    public PidList getInheritingContentModels(String cmpid) throws
                                                            IllegalStateException,
                                                            FedoraConnectionException,
                                                            ObjectNotFoundException,
                                                            ObjectIsWrongTypeException,
                                                            FedoraIllegalContentException,
                                                            InvalidCredentialsException {
        cmpid = FedoraUtil.ensureURI(cmpid);
        PidList descendants = inheritingContentModels.get(cmpid);
        if (descendants != null){
            return descendants;
        }
        descendants =  connector.getInheritingContentModels(cmpid);
        inheritingContentModels.put(cmpid,descendants);
        return descendants;
    }



    public PidList getInheritedContentModels(String cmpid) throws
                                                           FedoraConnectionException,
                                                           ObjectNotFoundException,
                                                           ObjectIsWrongTypeException,
                                                           FedoraIllegalContentException,
                                                           InvalidCredentialsException {

        cmpid = FedoraUtil.ensureURI(cmpid);
        PidList descendants = inheritedContentModels.get(cmpid);
        if (descendants != null){
            return descendants;
        }
        descendants =  connector.getInheritedContentModels(cmpid);
        inheritedContentModels.put(cmpid,descendants);
        return descendants;
    }

    public List<String> listDatastreams(String pid) throws
                                                    IllegalStateException,
                                                    FedoraConnectionException,
                                                    ObjectNotFoundException,
                                                    FedoraIllegalContentException,
                                                    InvalidCredentialsException {
        return connector.listDatastreams(pid);
    }

    public String getUsername() {
        return connector.getUsername();
    }

    public boolean authenticate() throws FedoraConnectionException {
        return connector.authenticate();
    }

    public String getUser() {
        return connector.getUser();
    }
}
