package dk.statsbiblioteket.doms.ecm.repository;

/**
 * A Fedora user token. This class encapsulates the fedora server url and the
 * user credentials.
 *
 * @see dk.statsbiblioteket.doms.ecm.repository.FedoraConnector
 */
public class FedoraUserToken {
    private final String serverurl;
    private final String username;
    private final String password;

    /**
     * Constructor. Creates a new user token.
     * @param serverurl the location of the fedora server. In the form
     * http://localhost:8080/fedora
     * @param username The username to connect with
     * @param password The password to connect with
     */
    public FedoraUserToken(String serverurl, String username, String password) {
        this.serverurl = serverurl;
        this.username = username;
        this.password = password;
    }

    /**
     * Get the server url.
     * @return the server url
     */
    public String getServerurl() {
        return serverurl;
    }

    /**
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FedoraUserToken)) {
            return false;
        }

        FedoraUserToken that = (FedoraUserToken) o;

        if (!password.equals(that.password)) {
            return false;
        }
        if (!serverurl.equals(that.serverurl)) {
            return false;
        }
        if (!username.equals(that.username)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = serverurl.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }
}
