package dk.statsbiblioteket.doms.ecm.repository;

import dk.statsbiblioteket.doms.ecm.exceptions.PIDGeneratorException;

/**
 * Pidgenerator factory class, and interface. Use the static #getPidGenerator() to
 * get an implementation of this class, and then the #generateNextAvailablePID
 * method to generate pids
 */
public abstract class PidGenerator {

    private static PidGenerator gen;

    /**
     * Get a implementation of this Class
     * @return
     * @throws PIDGeneratorException
     */
    public static PidGenerator getPIDGenerator() throws PIDGeneratorException{

        if (gen == null){
            String pidgenerator = Repository.getPidGenerator();

            try {
                Class<?> tor = Thread.currentThread().getContextClassLoader().loadClass(pidgenerator);
                PidGenerator pidObj = (PidGenerator) tor.newInstance();
                gen = pidObj;
                //TODO LOGGING
            } catch (ClassNotFoundException e) {
                throw new Error("PidGenerator class not found",e);
            } catch (IllegalAccessException e) {
                throw new Error("Cannot access PidGenerator class",e);
            } catch (InstantiationException e) {
                throw new Error("Cannot instantiate PidGenerator class",e);
            } catch (ClassCastException e){
                throw new Error("PidGenerator is not of the PidGenerator Interface",e);

            }
        }
        return gen;
    }

    /**
     * Get the next available pid with the prefix s
     * @param s the prefix
     * @return the next available pid
     * @throws PIDGeneratorException Catch-all exception for everything that
     * can go wrong.
     */
    public abstract String generateNextAvailablePID(String s) throws
                                                              PIDGeneratorException;

}
