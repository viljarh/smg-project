package no.ntnu.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Creates a Secure Socket Layer connection between server and client.
 */
public class SslConnection {

  private final char[] keyStorePass;
  private final KeyStore keyStore;
  private final int portNumber;

  /**
   * Creates an ssl connection.
   *
   * @param portNumber       the port for the server.
   * @param keyStorePath     the path to the keystore file.
   * @param keyStorePassword the password for the keystore.
   * @throws KeyStoreException exception.
   */
  public SslConnection(int portNumber, String keyStorePath, String keyStorePassword) throws KeyStoreException {
    this.keyStorePass = keyStorePassword.toCharArray();
    this.keyStore = KeyStore.getInstance("JKS");
    this.portNumber = portNumber;
    loadKeyStore(keyStorePath);
  }

  private void loadKeyStore(String keyStorePath) {
    try (FileInputStream keyStoreStream = new FileInputStream(keyStorePath)) {
      this.keyStore.load(keyStoreStream, this.keyStorePass);
    } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new RuntimeException("Failed to load keystore", e);
    }
  }

  /**
   * Returns the ssl socket for the server.
   *
   * @return Server ssl socket.
   * @throws KeyStoreException         keystore exception.
   * @throws NoSuchAlgorithmException  no such algorithm.
   * @throws UnrecoverableKeyException unrecoverable key exception.
   * @throws KeyManagementException    key management exception.
   * @throws IOException
   */
  public SSLServerSocket createServerSocket() throws KeyStoreException, NoSuchAlgorithmException,
      UnrecoverableKeyException, KeyManagementException, IOException {
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    keyManagerFactory.init(this.keyStore, this.keyStorePass);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

    SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
    return (SSLServerSocket) serverSocketFactory.createServerSocket(portNumber);
  }

  /**
   * Returns the ssl socket for the client.
   *
   * @param address the server address.
   * @return the client SSL socket.
   * @throws KeyStoreException        keystore exception.
   * @throws NoSuchAlgorithmException no such algorithm.
   * @throws KeyManagementException   key management exception.
   * @throws IOException
   * @throws UnknownHostException
   */
  public SSLSocket createClientSocket(String address)
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnknownHostException, IOException {
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
    trustManagerFactory.init(this.keyStore);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

    SSLSocketFactory socketFactory = sslContext.getSocketFactory();
    return (SSLSocket) socketFactory.createSocket(address, portNumber);
  }
}