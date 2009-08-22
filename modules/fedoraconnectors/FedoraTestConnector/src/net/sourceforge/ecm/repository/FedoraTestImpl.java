package net.sourceforge.ecm.repository;

import net.sourceforge.ecm.exceptions.DatastreamNotFoundException;
import net.sourceforge.ecm.exceptions.FedoraConnectionException;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.exceptions.ObjectNotFoundException;
import net.sourceforge.ecm.utils.Constants;
import net.sourceforge.ecm.utils.DocumentUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * TODO abr forgot to document this class
 */
public class FedoraTestImpl implements FedoraConnector {

    private Map<String,TestObject> objects;

    //The twofold constructor method
    public void initialise(FedoraUserToken token) {

    }

    public FedoraTestImpl() {
        objects = new HashMap<String, TestObject>();

    }



    public boolean exists(String pid) {
        TestObject object = objects.get(Repository.ensurePID(pid));
        return object != null;

    }

    public boolean isDataObject(String pid) {
        return exists(pid) && !isContentModel(pid);

    }

    public boolean isTemplate(String pid) {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            return false;
        } else{
            List<Relation> rels = object.getRelations();
            for (Relation rel:rels){
                if (rel.getRelation().equals(Constants.TEMPLATE_REL)){
                    return true;
                }
            }
            return false;
        }
    }


    public boolean isContentModel(String pid) {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            return false;
        } else{
            List<Relation> rels = object.getRelations();
            for (Relation rel:rels){
                if (rel.getRelation().equals(Constants.HAS_MODEL) &&
                        rel.getTo().equals(Constants.CONTENT_MODEL_3_0)){
                    return true;
                }
            }
            return false;
        }

    }



    public PidList query(String query) throws FedoraConnectionException, FedoraIllegalContentException {
        query = query.replaceAll("\\s+"," ");//clean up for easier matching

        Map<String,String> requiments = new HashMap<String,String>();
        PidList result = new PidList();
        if (query.startsWith("select $object from <#ri> where ")){
            String questions = query.substring("select $object from <#ri> where ".length());

            //to do this quick: There is only one question

            //to do this right:
            // This is an or'ed list, isolate each of the questions
            StringTokenizer tokens = new StringTokenizer(questions, " or ");
            while (tokens.hasMoreTokens()){
                String token = tokens.nextToken();
                //of the form $object <somrel> <info:fedora/sometarget>
                if (token.startsWith("$object")){
                    String[] elements = token.split("\\s");
                    requiments.put(cutBraces(elements[1]),cutBraces(Repository.ensurePID(elements[2])));
                }   else {
                    return new PidList();
                }
            }//The requirements now list all the demands for relations

            for (TestObject o: objects.values()){
                List<Relation> rels = o.getRelations();
                boolean in=true;
                for (String demand: requiments.keySet()){
                    Relation examine = new Relation(o.getPid(),demand,requiments.get(demand));
                    if (rels.contains(examine)){
                        result.add(o.getPid());
                    } else {
                        in = false;
                    }
                }
            }
            return result;


        } else {
            return new PidList();
        }


    }

    private String cutBraces(String a){
        if (a.startsWith("<")){
            a = a.substring(1);
        }
        if (a.endsWith(">")){
            a = a.substring(0,a.length()-1);
        }
        return a;
    }

    public boolean addRelation(String from, String relation, String to) throws ObjectNotFoundException, FedoraConnectionException {
        TestObject object = objects.get(Repository.ensurePID(from));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            Relation rel = new Relation(from,Repository.ensurePID(to),relation);
            return object.add(rel);
        }
    }

    public boolean addLiteralRelation(String from, String relation, String value, String datatype) throws ObjectNotFoundException, FedoraConnectionException {
        TestObject object = objects.get(Repository.ensurePID(from));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            Relation rel = new Relation(from,value,relation);
            return object.add(rel);
        }
    }

    public Document getObjectXml(String pid) throws ObjectNotFoundException, FedoraConnectionException, FedoraIllegalContentException {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            return object.dumpAsDocument();
        }

    }

    public String ingestDocument(Document newobject, String logmessage) throws FedoraConnectionException, FedoraIllegalContentException {
        TestObject object = TestObject.parseFromDocument(newobject);
        objects.put(object.getPid(),object);
        return object.getPid();
    }

    public List<Relation> getRelations(String pid) throws FedoraConnectionException, ObjectNotFoundException {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            return object.getRelations();
        }
    }

    public List<Relation> getRelations(String pid, String relation) throws FedoraConnectionException, ObjectNotFoundException {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            List<Relation> rels = object.getRelations();
            List<Relation> returns = new ArrayList<Relation>();
            for (Relation rel:rels){
                if (rel.getRelation().equals(relation)){
                    returns.add(rel);
                }
            }
            return returns;
        }

    }

    public Document getDatastream(String pid, String dsid) throws DatastreamNotFoundException, FedoraConnectionException, FedoraIllegalContentException, ObjectNotFoundException {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            String ds = object.get(dsid);
            if (ds == null){
                throw new DatastreamNotFoundException();
            } else{
                ByteArrayInputStream in = new ByteArrayInputStream(ds.getBytes());
                try {
                    return DocumentUtils.DOCUMENT_BUILDER.parse(in);
                } catch (SAXException e) {
                    throw new FedoraIllegalContentException(e);
                } catch (IOException e) {
                    throw new Error("Failed to read a string",e);
                }
            }

        }

    }

    public PidList getContentModels(String pid) throws FedoraConnectionException, ObjectNotFoundException {
        List<Relation> rels = getRelations(pid, Constants.HAS_MODEL);
        PidList cms = new PidList();
        for (Relation rel: rels){
            cms.add(rel.getTo());
        }
        return cms;
    }

    public PidList getInheritedContentModels(String cmpid) throws FedoraConnectionException, ObjectNotFoundException {
        return new PidList();
    }

    public PidList getInheritingContentModels(String cmpid) throws FedoraConnectionException, ObjectNotFoundException {
        return new PidList();
    }

    public List<String> listDatastreams(String pid) throws FedoraConnectionException, ObjectNotFoundException {
        TestObject object = objects.get(Repository.ensurePID(pid));
        if (object == null){
            throw new ObjectNotFoundException();
        } else{
            return new ArrayList<String>(object.getDatastreams().keySet());
        }
    }

    public String getUsername() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void addObject(TestObject o){
        objects.put(o.getPid(),o);
    }

}
