package life.catalogue.common.io;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtils {
  
  public static int findFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    }
  }
}
