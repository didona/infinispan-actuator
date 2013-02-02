package eu.cloudtm;

/**
 * Represents an Infinispan JVM instance, composed by an hostname and a port
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class InfinispanMachine {

    private final String hostname;
    private final String port;
    private final String username;
    private final String password;

    /**
     * construct an Infinispan Machine instance
     *
     * @param hostname the hostname (non-null)
     * @param port     the JMX port (non-null)
     * @param username the JMX username (optional)
     * @param password the JMX password (optional)
     */
    public InfinispanMachine(String hostname, String port, String username, String password) {
        if (hostname == null) {
            throw new NullPointerException("Hostname cannot be null");
        } else if (port == null) {
            throw new NullPointerException("Port cannot be null");
        }

        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * see {@link #InfinispanMachine(String, String, String, String)}
     */
    public InfinispanMachine(String hostname, int port, String username, String password) {
        this(hostname, String.valueOf(port), username, password);
    }

    /**
     * see {@link #InfinispanMachine(String, String, String, String)}
     */
    public InfinispanMachine(String hostname, String port) {
        this(hostname, port, null, null);
    }

    /**
     * see {@link #InfinispanMachine(String, String, String, String)}
     */
    public InfinispanMachine(String hostname, int port) {
        this(hostname, String.valueOf(port), null, null);
    }

    /**
     * @return the hostname represented by this instance
     */
    public final String getHostname() {
        return hostname;
    }

    /**
     * @return the JMX port represented by this instance
     */
    public final String getPort() {
        return port;
    }

    /**
     * @return the JMX username represented by this instance or null if none
     */
    public final String getUsername() {
        return username;
    }

    /**
     * @return the JMX password represented by this instance or null if none
     */
    public final String getPassword() {
        return password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = hostname.hashCode();
        result = 31 * result + port.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InfinispanMachine that = (InfinispanMachine) o;

        return hostname.equals(that.hostname) && port.equals(that.port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "InfinispanMachine{" +
                "hostname='" + hostname + '\'' +
                ", port='" + port + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
