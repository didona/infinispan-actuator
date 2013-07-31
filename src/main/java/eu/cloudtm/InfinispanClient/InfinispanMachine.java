/*
 * INESC-ID, Instituto de Engenharia de Sistemas e Computadores Investigação e Desevolvimento em Lisboa
 * Copyright 2013 INESC-ID and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package eu.cloudtm.InfinispanClient;

/**
 * Represents an Infinispan JVM instance, composed by an hostname and a port
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class InfinispanMachine {

    private final String hostname;
    private final Integer port;
    private final String ip;
    private final String username;
    private final String password;

    /**
     * construct an Infinispan Machine instance
     *
     * @param hostname the hostname (non-null)
     * @param port     the JMX port (non-null)
     * @param ip       the ip address (non-null)
     * @param username the JMX username (optional)
     * @param password the JMX password (optional)
     */
    public InfinispanMachine(String hostname, Integer port, String ip, String username, String password) {
        if (hostname == null) {
            throw new NullPointerException("Hostname cannot be null");
        } else if (port == null) {
            throw new NullPointerException("Port cannot be null");
        } else if (ip == null) {
            throw new NullPointerException("Ip cannot be null");
        }

        this.hostname = hostname;
        this.port = port;
        this.ip = ip;
        this.username = username;
        this.password = password;
    }


    /**
     * see {@link #InfinispanMachine(String, Integer, String, String, String)}
     */
    public InfinispanMachine(String hostname, int port, String ip) {
        this(hostname, port, ip, null, null);
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
    public final int getPort() {
        return port;
    }

    /**
     * @return the ip address represented by this instance
     */
    public final String getIp() {
        return ip;
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
