package dk.statsbiblioteket.doms.ecm.repository;

import dk.statsbiblioteket.doms.ecm.repository.utils.FedoraUtil;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Oct 21, 2010
 * Time: 4:50:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class Caches {




    private TimeSensitiveCache<String,List<FedoraConnector.Relation>> relations
            = new TimeSensitiveCache<String,List<FedoraConnector.Relation>>(1000*60*3,true,20);
    private TimeSensitiveCache<String,Document> objectXML
            = new TimeSensitiveCache<String,Document>(1000*60*30,true,20);
    private  TimeSensitiveCache<String,Document> datastreamContents
            = new TimeSensitiveCache<String,Document>(1000*60*30,true,20);


    public void removeRelations(String from) {
        relations.remove(from);
    }

    public void storeRelations(String pid,
                               List<FedoraConnector.Relation> relations) {
        if (pidProtection(pid)){
            this.relations.put(pid,relations);
        }

    }
    public List<FedoraConnector.Relation> getRelations(String pid) {
        return relations.get(pid);
    }



    public Document getObjectXML(String pid) {
        return objectXML.get(pid);
    }

    public void storeObjectXML(String pid, Document doc) {
        if (pidProtection(pid)){
            objectXML.put(pid,doc);
        }
    }



    public void storeDatastreamContents(String pid, String dsid, Document doc) {
        if (pidProtection(pid)){
            datastreamContents.put(mergeStrings(pid,dsid),doc);
        }
    }

    public Document getDatastreamContents(String pid, String dsid) {
        return datastreamContents.get(mergeStrings(pid,dsid));
    }


    private String mergeStrings(String... strings) {
        String result = "";
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            if (i == 0) {
                result = string;
            } else {
                result = result + "/" + string;
            }

        }
        return result;
    }


    private boolean pidProtection(String pid){
        pid = FedoraUtil.ensurePID(pid);
        if (pid.startsWith("doms:")){
            return true;
        }
        return false;
    }
}
