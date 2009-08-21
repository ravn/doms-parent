package net.sourceforge.ecm.repository;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The original nasty pid generator
 */
public class PidGeneratorImpl extends PidGenerator {


    private static final int FEDORA_PID_MAX_LENGTH = 64;
    private AtomicInteger counter = new AtomicInteger(0);
    /**
     * Generate the next available PID.
     *
     * @param infix A string, all or part of which may be used as part of the
     * PID, but with no guarantee. May be left empty.
     * @return The next available (unique) PID, possibly including (part of) the
     * requested infix.
     */
    public String generateNextAvailablePID(String infix) {
/*                  log.trace("Entering generateNextAvailablePID with infix = " + infix);*/
        // Calling this method should never fail.

        if (infix == null) {
            infix = "";
        }

        // Implementation note: Due to a length restriction of 64 characters
        // for Fedora PIDs, any infix longer than approx 50 characters will
        // be shortened, to ensure space for adding a unique part to the PID.
        String paddedCounter;
        String namespace = "demo:";
        // currentTimeMillis is a long, so max 64 bit,
        // so hex-version is max 16 chars.
        int rightLengthOfCurrentTimeMillis = 16;
        int rightLengthOfUniquifyingCounter = 4;
        int rightLengthOfUniquifier
                = rightLengthOfCurrentTimeMillis
                  + rightLengthOfUniquifyingCounter;
        int rightLengthOfInfix = Math.min(FEDORA_PID_MAX_LENGTH
                                          - namespace.length()
                                          - rightLengthOfUniquifier, infix.length());
        String uniquifier = Long.toHexString(System.currentTimeMillis());

        while (uniquifier.length() < rightLengthOfCurrentTimeMillis) {
            uniquifier = '0' + uniquifier;
        }

        // add counter to make uniquifier unique for several calls within the
        // same millisecond.
        // TODO We should consider eliminating the probability of two equal
        // pids, f.x. be checking against the Fedora repository..
        paddedCounter = Integer.toString((counter.getAndIncrement() % 10000));
        while (paddedCounter.length() < rightLengthOfUniquifyingCounter) {
            paddedCounter = '0' + paddedCounter;
        }
        uniquifier += paddedCounter;

/*                  log.trace("Leaving generateNextAvailablePID");*/
        return namespace + infix.substring(0, rightLengthOfInfix) + uniquifier;
    }



}
