/**
 * @author jef@cs.nott.ac.uk 
 */

package ac.uk.notts.mrl.MyJourneyDiary;

/**
 * Callback interface used by IRemoteService to send
 * synchronous notifications back to its clients. This is a
 * one-way interface so the server does not block waiting for the client.
 */
oneway interface IRemoteServiceCallback {
    /**
     * Called when the service has a new value for you.
     */
    void valueChanged(String point);
}
