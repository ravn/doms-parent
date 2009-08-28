package dk.statsbiblioteket.doms.ecm.webservice;


import fedora.client.FedoraClient;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.FedoraUserToken;
import dk.statsbiblioteket.doms.ecm.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * This class is the first custom class met when a webservice request is
 * performed. It handles the authentication of the user against the fedora
 * repository, and thus creates a FedoraUserToken. With this, it
 * initializes the chosen FedoraConnector, and with this it initializes the
 * Repository.
 *
 *<br/>
 * I believe one instance of this class is made per request. 
 *
 * @see dk.statsbiblioteket.doms.ecm.repository.FedoraConnector
 * @see dk.statsbiblioteket.doms.ecm.repository.FedoraUserToken
 * @see dk.statsbiblioteket.doms.ecm.repository.Repository
 */
public class Initializer
        implements AuthenticationProvider,
                   ServletContextAware{
    private static final String DEFAULT_SERVER = "http://localhost:8080/fedora";
    private String server;
    private String connector;
    private static final String DEFAULT_CONNECTOR = "dk.statsbiblioteket.doms.ecm.repository.FedoraSoapImpl";
    private String pidgenerator;

    private static final Log LOG = LogFactory.getLog(Initializer.class);
    private static final String DEFAULT_PIDGENERATOR = "dk.statsbiblioteket.doms.ecm.repository.PidGeneratorImpl";


    /**
     * This methods receives the authentication information from the client.
     * It checks this information against the repository, and if
     * succesful, it makes a FedoraUserToken and initialises the Repository
     * @param authentication The authentication object
     * @return A authentication object
     * @throws AuthenticationException if the user could not be authenticated
     * for whatever reason. 
     */
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof UsernamePasswordAuthenticationToken) {

            String username = (String) authentication.getPrincipal();
            String password = (String) authentication.getCredentials();

            FedoraUserToken token = new FedoraUserToken(server, username, password);
            try {
                FedoraClient client = null;

                client = new FedoraClient(
                        token.getServerurl(),
                        token.getUsername(),
                        token.getPassword());

                client.getAPIA().describeRepository();
                //TODO logging
            } catch (RemoteException e) {
                throw new AuthenticationServiceException("Could not connect to fedora",e);
            }  catch (IOException e) {
                throw new AuthenticationServiceException("Could not connect to fedora",e);
            } catch (ServiceException e) {
                throw new BadCredentialsException("Fedora does not allow describeRepository call with the given credentials",e);
            }

            GrantedAuthority auth = new GrantedAuthority(){
                String authority = "ROLE_ADMIN";

                public String getAuthority() {
                    return authority;

                }

                public int compareTo(Object o) {

                    GrantedAuthority grantedAuthority = (GrantedAuthority) o;
                    return getAuthority().compareTo(grantedAuthority.getAuthority());
                }
            };
            MyAuthentication newauth =
                    new MyAuthentication(username,password,new GrantedAuthority[]{auth});

            //Initialise the Repository with the user credentials, and the
            //correct connector.
            // This call assumes that the methods set*Context have allready been
            //called
            Repository.initialise(token, createConnector());
            Repository.setPidGenerator(pidgenerator);
            return newauth;

        } else{
            return null;
        }
    }

    /**
     * Small utility method for creating a new instance of the fedora Connector
     * @return a new FedoraConnector instance
     */
    private FedoraConnector createConnector(){
        try {
            Class<?> tor = Thread.currentThread().getContextClassLoader().loadClass(connector);
            FedoraConnector conObj = (FedoraConnector) tor.newInstance();
            return conObj;
            //TODO LOGGING
        } catch (ClassNotFoundException e) {
            throw new Error("Fedora Connector class not found",e);
        } catch (IllegalAccessException e) {
            throw new Error("Cannot access connector class",e);
        } catch (InstantiationException e) {
            throw new Error("Cannot instantiate connector class",e);
        } catch (ClassCastException e){
            throw new Error("Connector is not of the Fedora Interface",e);
        }


    }


    public boolean supports(Class authentication) {
        if (authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class) ){
            return true;
        }
        return false;
    }


    /**
     * Gets the servlet context, for reading params from web.xml.
     * Reads the params fedoraLocation and fedoraConnector, and sets them to
     * field server and field connector.
     *<br/>
     * This method is automatically invoked before the authenticate method, as
     * soon as the servletcontext becomes available. As such, the authenticate
     * method can assume that the fields are initialized.
     * @param servletContext the context
     */
    public void setServletContext(ServletContext servletContext) {


        String loc = servletContext.getInitParameter("fedoraLocation");

        if (loc != null){
            this.server = loc;
            LOG.debug("Sets fedora location to "+loc);

        } else{
            this.server = DEFAULT_SERVER;
        }

        String connector = servletContext.getInitParameter("fedoraConnector");
        if (connector != null){
            this.connector = connector;
            LOG.debug("Sets fedora connector to "+connector);
        } else {
            this.connector = DEFAULT_CONNECTOR;
        }

        String pidgenerator = servletContext.getInitParameter("fedoraPidGenerator");
        if (pidgenerator != null){
            this.pidgenerator = pidgenerator;
            LOG.debug("Sets fedora connector to "+pidgenerator);
        } else {
            this.pidgenerator = DEFAULT_PIDGENERATOR;
        }


    }


}
