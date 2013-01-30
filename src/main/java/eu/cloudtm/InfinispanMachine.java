package eu.cloudtm;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class InfinispanMachine {

    private final String hostname;
    private final String port;
    private final String username;
    private final String password;

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

    public InfinispanMachine(String hostname, int port, String username, String password) {
        this(hostname, String.valueOf(port), username, password);
    }

    public InfinispanMachine(String hostname, String port) {
        this(hostname, port, null, null);
    }

    public InfinispanMachine(String hostname, int port) {
        this(hostname, String.valueOf(port), null, null);
    }

    public final String getHostname() {
        return hostname;
    }

    public final String getPort() {
        return port;
    }

    public final String getUsername() {
        return username;
    }

    public final String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InfinispanMachine that = (InfinispanMachine) o;

        return hostname.equals(that.hostname) && port.equals(that.port);

    }

    @Override
    public int hashCode() {
        int result = hostname.hashCode();
        result = 31 * result + port.hashCode();
        return result;
    }

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
