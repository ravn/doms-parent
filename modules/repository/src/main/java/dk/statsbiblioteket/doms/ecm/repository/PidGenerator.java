package dk.statsbiblioteket.doms.ecm.repository;

import dk.statsbiblioteket.doms.ecm.repository.exceptions.PIDGeneratorException;

/**
 * Pidgenerator factory class, and interface. Use the static #getPidGenerator() to
 * get an implementation of this class, and then the #generateNextAvailablePID
 * method to generate pids
 */
public abstract class PidGenerator {

    private static PidGenerator gen;


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
