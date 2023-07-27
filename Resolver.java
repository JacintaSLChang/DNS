import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Resolver {
  private static String rootHintsFile = "named.root";
  private static List<String> rootServers; 
  private static final int dnsPort = 53;
  private static List<String> serversToQuery = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    if (checkArguments(args)) {
      return;
    }

    rootServers = readRootHintsFile();

    Datagram clientDatagram = receiveClientSocket(Integer.parseInt(args[0]));

    serversToQuery.addAll(rootServers);

    Response finalRes = iterativeResolve(clientDatagram);

    sendClientSocket(finalRes, clientDatagram);
  }

  /*
   * Checks if arguments are valid
   * Arguments:     String[]    args     string of arguments given to command line
   * Return:        Boolean              true if invalid
   */
  public static Boolean checkArguments(String[] args) {
    // client resolver_ip resolver_port name timeout
    if (args.length != 1) {
      System.out.println("Error: too many/few arguments\n" + 
      "Usage: resolver port");
      return true;
    }

    try {
      int port = Integer.parseInt(args[0]);
      if (port < 1024 || port == 8080) {
        System.out.println("Error: invalid arguments\n" + 
        "Invalid port number");
        return true;
      }
    } catch (NumberFormatException e) {
      System.out.println("Error: invalid arguments\n" + 
        "Invalid port number");
      return true;
    }

    return false;
  }

  /*  
   * Reads the root hints file and returns a string of IPV4 addresses
   * Arguments: none
   * Return:    List<String>   list of IPV4 addresses
   */
  public static List<String> readRootHintsFile() {
    List<String> roots = new ArrayList<>();

    try {
      // Read bytes from named.root
      BufferedReader reader = new BufferedReader(new FileReader(rootHintsFile));
      String line = new String();
      
      // Read each line from roots file
      while ((line = reader.readLine()) != null) {
        // Only get A and AAAA records
        if (!(line.substring(0,1).equals(";") 
        || line.substring(0,1).equals("."))) {
          // Extract IPV4 addresses
          String[] words = line.split("\\s+");
          if (words[2].equals("A")) {
            roots.add(words[3]);
          }
        }
      }
      reader.close();
    } catch (Exception FileNotFoundException) {
      System.out.println("Could not find " + rootHintsFile);
    }

    return roots;
  }

  /*  
   * Establishes socket connection to client and receives datagram from client
   * Arguments:   int       resolverPort    port number to receive datagram
   * Return:      Datagram                  class Datagram from client
   */
  public static Datagram receiveClientSocket(int resolverPort) throws IOException {
    // Create socket
		DatagramSocket serverSocket = new DatagramSocket(resolverPort);
    System.out.println("Server is ready :" + resolverPort);

    // Receive datagram
    byte[] clientQueryBytes = new byte[512];
    DatagramPacket clientQuery = new DatagramPacket(clientQueryBytes, clientQueryBytes.length);
    serverSocket.receive(clientQuery);
    clientQueryBytes = removeTrailingZeros(clientQueryBytes);

    // Store datagram information
    Datagram datagram = new Datagram(serverSocket, clientQuery, clientQueryBytes);

    return datagram;
  }

  /*
   * Iteratively query servers in the serversToQuery list until an answer is found
   * Arguments:   Datagram    clientDatagram    client's query to pass onto servers
   * Return:      Response                      class Response holding answers from response
   */
  public static Response iterativeResolve(Datagram clientDatagram) throws IOException {
    Response response = null;
    while (!serversToQuery.isEmpty()) {
        // Query the next server
        String server = serversToQuery.remove(0);
        response = queryDnsServer(clientDatagram, server);

        if (response != null) {
          // If A answer was received, return the final answer
          if (response.getAnswer() == true) {
            return response;
          // Received format or name error code. Terminate. 
          } else if (response.getRCODE() == 1 || response.getRCODE() == 3) {
            return response;
          // Received name error. Terminate.
          // If NS answer as received, add its IP addresses to 
          //  serversToQuery list
          } else if (response.getA().size() > 0) {
            // Only add first NS for faster performance
            serversToQuery.add(response.getA().get(0)); 
          }
        }
    }

    // Could not resolve the domain. Send a fake response
    return response; 
  }

  /*
   * Send query to the given server and receive its response
   * Arguments:   Datagram    clientDatagram    client's query to pass onto servers
   *              String      destIp            IP address of server to query
   * Return:      Response                      class Response holding answers from response
   */
  public static Response queryDnsServer(Datagram clientDatagram, String destIp) throws IOException {
      InetAddress dnsAddr;
      try {
        dnsAddr = InetAddress.getByName(destIp);
      } catch (UnknownHostException e) {
        System.out.println("Could not find host: " + e);
        return null;
      }

      // Create new socket to connect to DNS server
      DatagramSocket dnsSocket = new DatagramSocket();
      DatagramPacket dnsPacket = new DatagramPacket(clientDatagram.getPacketBytes(), 
      clientDatagram.getPacketBytes().length, dnsAddr, dnsPort);
      try {
        dnsSocket.send(dnsPacket);
      } catch (Exception e) {
        System.out.println("Error sending packet: " + e);
      }

      // Receive response
      byte[] dnsResBytes = new byte[512];
      DatagramPacket dnsResPacket = new DatagramPacket(dnsResBytes ,dnsResBytes.length);

      dnsSocket.setSoTimeout(1000);
      try {
        dnsSocket.receive(dnsResPacket);
      } catch (SocketTimeoutException e) {
        System.out.println("Timeout Error: " + e);
        dnsSocket.close();
        return null;
      }

      // Store datagram information
      Datagram dnsDatagram = new Datagram(dnsSocket, dnsResPacket, dnsResBytes);

      // Check Id from Dns response with client Id
      if (clientDatagram.getId() != dnsDatagram.getId()) {
        return null;
      }

      Response res = new Response();
      res.readResPacket(dnsDatagram);

      dnsSocket.close();

    return res;
  }


  /* 
   * Sends response back to client using previous socket connection
   * Arguments:   Response    res             response to send the client
   *              Datagram    clientDatagram  initial datagram client to resolver
   * Return:      void
  */
  public static void sendClientSocket(Response res, Datagram clientDatagram) {
    InetAddress addr = clientDatagram.getDatagramPacket().getAddress();
    int port = clientDatagram.getDatagramPacket().getPort();
    
		// Get previously used client-resolver socket
    DatagramSocket socket = clientDatagram.getDatagramSocket();


    Datagram sendDatagram = res.getDatagram();
    DatagramPacket sendPacket = new DatagramPacket(sendDatagram.getPacketBytes(), sendDatagram.getPacketBytes().length, addr, port);
    
    // Send datagram
    try {
      socket.send(sendPacket);
    } catch (Exception e) {
      System.out.println("Could not send final response: " + e);
    }
  }

  /* 
   * Remove trailing zeros from an array of bytes
   * Arguments:   byte[]      bytes           array to edit
   * Return:      byte[]                      edited array
  */
  public static byte[] removeTrailingZeros(byte[] bytes) {
    int length = bytes.length;
    int end = length;
    for (int i = length - 1; i > 0; i--) {
      if (bytes[i] != 0) {
        break;
      } else {
        end = i;
      }
    }
    return Arrays.copyOfRange(bytes, 0, end);
  }
}
