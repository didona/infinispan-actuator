package eu.cloudtm.InfinispanClient.exception;

/**
 * Created by: Fabio Perfetti
 * E-mail: perfabio87@gmail.com
 * Date: 8/28/13
 */
public class HostnameNotFoundException extends Exception {

    public HostnameNotFoundException(Throwable throwable) {
        super(throwable);
    }

    public HostnameNotFoundException(String message) {
        super(message);
    }

}
