import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class Client {
  private static DatagramSocket clientSocket;
  private static int enhancement = 0;

  private static final int INVALID = -1;
  private static final int TIMEOUT = 0;
  private static final int A = 1;
  private static final int NS = 2;
  private static final int CNAME = 5;
  private static final int PTR = 12;
  private static final int MX = 15;

  public static void main(String[] args) throws Exception {
    if ((enhancement = checkArguments(args)) == INVALID) {
      return;
    }
          System.out.println(enhancement);


    InetAddress IPAddress = InetAddress.getByName(args[0]);
    int serverPort = Integer.parseInt(args[1]);
    String domain = args[2];

    int timeout = 5;
    if (enhancement == TIMEOUT) {
      timeout = (args.length > 3 ? Integer.parseInt((args[3])) : 5);
    }
    System.out.println("Timeout is: " + timeout + "ms");

    clientSocket = new DatagramSocket();
    clientSocket.setSoTimeout(timeout);
    try {
      sendQuery(IPAddress, serverPort, domain);
    } catch (IOException e) {
      System.out.println("Could not send query: " + e);
    }

    Datagram resolverResDatagram = receiveResponse();
    Response finalRes = new Response();
    finalRes.readResPacket(resolverResDatagram);

    printFinal(finalRes, domain);
  }

  /*
   * Checks if arguments are valid
   * Arguments:     String[]    args     string of arguments given to command line
   * Return:        int                  -1 for invalid
   *                                     0 timeout
   *                                     1 for no extra argument or A
   *                                     2 for NS
   *                                     5 for CNAME
   *                                     12 for PTR
   *                                     15 for MX
   */
  public static int checkArguments(String[] args) {
    // client resolver_ip resolver_port name timeout
    if (args.length < 3 || args.length > 4) {
      System.out.println("Error: too many/few arguments\n" + 
      "Usage: client resolver_ip resolver_port name timeout");
      return INVALID;
    }

    try {
      int port = Integer.parseInt(args[1]);
      if (port < 1024 || port == 8080) {
        System.out.println("Error: invalid arguments\n" + 
        "Invalid port number, must be > 1024 and not 8080");
        return INVALID;
      }
    } catch (NumberFormatException e) {
      System.out.println("Error: invalid arguments\n" + 
        "Invalid port number, must be an integer");
      return INVALID;
    }

    if (args.length == 4) {
      // Check if advanced record
      String advanced = args[3].toUpperCase();
      switch (advanced) {
        case "A":
          return A;
        case "NS":
          return NS;
        case "CNAME":
          return CNAME;
        case "PTR":
          return PTR;
        case "MX":
          return MX;
      }

      // Check if timeout
      try {
        Integer.parseInt(args[3]);
      }
      catch (NumberFormatException e) {
        System.out.println("Error: invalid arguments\n" + 
        "Timeout to be given in integer seconds");
        return INVALID;
      }

      // Valid timeout
      return TIMEOUT;
    }

    // No extra arguments
    return A;
  }

  /*
   * Send query to resolver with given port and domain
   * Arguments:   InetAddress   IPAddress   IP address of resolver
   *              int           serverPort  port of resolver to send to
   *              String        domain      domain to resolve
   * Return:      void
  */
  public static void sendQuery(InetAddress IPAddress, int serverPort, String domain) throws IOException {
    /*
    * Start:
    *  Code referenced from
    *  https://levelup.gitconnected.com/dns-request-and-response-in-java-acbd51ad3467
    * Modifications:
    *   Removed System.out.println statements
    *   Other - in comments
    */
    // Create a byte stream that you can write to
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

    // Create request ID
    Random random = new Random();
    short ID = (short)random.nextInt(32767);
    dataOutputStream.writeShort(ID);

    // Set request flags
    String requestFlags = "0"; // QR (request = 0, res = 1)
    requestFlags += "0000"; // OPCode (QUERY = 0, IQUERY = 1, STATUS = 2)
    requestFlags += "0";    // AA (not authorative = 0, for request)
    requestFlags += "0";    // TC (not truncated = 0, truncated = 1)
    requestFlags += "0";    // RD (no recursion = 0, recursion desired = 1)
    requestFlags += "0";    // RA (recursion unavailable = 0, for request)
    requestFlags += "010";  // Z (for future use)
    requestFlags += "0000"; // RDCODE (no error = 0, for request)
    short shortFlags = Short.parseShort(requestFlags, 2);
    dataOutputStream.writeShort(shortFlags);
    
    // Set next section
    short QDCOUNT = 1;
    short ANCOUNT = 0;
    short NSCOUNT = 0;
    short ARCOUNT = 0;
    if (enhancement == PTR) { ARCOUNT = 1; }

    dataOutputStream.writeShort(QDCOUNT);
    dataOutputStream.writeShort(ANCOUNT);
    dataOutputStream.writeShort(NSCOUNT);
    dataOutputStream.writeShort(ARCOUNT);

    // Write Question section
    // Write QNAME
    String[] domainParts = domain.split("\\.");
    // Modification: restructure for PTR
    if (enhancement == PTR) {
      // dataOutputStream.writeByte(0x02);
      for (int i = domainParts.length - 1; i >= 0; i--) {
        byte[] domainBytes = domainParts[i].getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeByte(domainParts[i].length());
        dataOutputStream.writeBytes(domainParts[i]);
      }
      dataOutputStream.writeByte(0x07);
      dataOutputStream.writeBytes("in-addr");
      dataOutputStream.writeByte(0x04);
      dataOutputStream.writeBytes("arpa");
    } else {
      for (int i = 0; i < domainParts.length; i++) {
          byte[] domainBytes = domainParts[i].getBytes(StandardCharsets.UTF_8);
          dataOutputStream.writeByte(domainBytes.length);
          dataOutputStream.write(domainBytes);
      }
    }
    
    dataOutputStream.writeByte(0); // End of QNAME

    dataOutputStream.writeShort(enhancement); // QTYPES (A = 1)
    dataOutputStream.writeShort(1); // QCLASS
    if (enhancement == PTR) {
      dataOutputStream.writeByte(0x00);
      dataOutputStream.writeByte(0x00);
      dataOutputStream.writeByte(0x29);
      dataOutputStream.writeByte(0x10);
    }

    // Create sockets to send/receive via UDP
    byte[] dnsFrame = byteArrayOutputStream.toByteArray();
    DatagramPacket dnsReqPacket = new DatagramPacket(dnsFrame, dnsFrame.length, IPAddress, serverPort);
    clientSocket.send(dnsReqPacket);
    /*
    * End:
    *  Code referenced from
    *  https://levelup.gitconnected.com/dns-request-and-response-in-java-acbd51ad3467
    */
  }
    

  /*
   * Receive response from resolver using previously established connection socket
   * Arguments:   none
   * Return:      Datagram            class Datagram of the response
   */
  public static Datagram receiveResponse() throws IOException {
    // Receive response
    byte[] resolverResBytes = new byte[512];
    DatagramPacket resolverResPacket = new DatagramPacket(resolverResBytes ,resolverResBytes.length);

      try {
        clientSocket.setSoTimeout(10000);
        clientSocket.receive(resolverResPacket);
      } catch (SocketException e) {
        System.out.println("Timeout Error: " + e);
        clientSocket.close();
        return null;
      } catch (IOException e) {
        System.out.println("Could not receive response: " + e);
          clientSocket.close();
        return null;
      }

    Datagram resDatagram = new Datagram(clientSocket, resolverResPacket, resolverResBytes);
    return resDatagram;
  }

  /*
   * Print the final reponse to the client's command line
   * Arguments:   Response      res       response of the final answer
   *              String        domain    name of the resolved domain
   * Return:      void
   */
  public static void printFinal(Response res, String domain) {
    System.out.println();

    System.out.println("<<COMP3331 UDP DNS Resolver>> " + domain);
    System.out.println();

    System.out.println("->>HEADER<<-");
    System.out.println("\tstatus: " + res.getRCODE() + "\tid: " + res.getId());
    System.out.println("\tAuthorative: " + res.getAnswer() + "\tTruncated: " + res.getTC());
    System.out.println("\tQuery: " + res.getQDCOUNT() + "\tANSWER: " + res.getANCOUNT() 
      + "\tAUTHORITY: " + res.getNSCOUNT() + "\tADDITIONAL: " + res.getARCOUNT());

    switch(res.getRCODE()) {
      case 0: 
        System.out.println("\tError code 0: no errors");
        break;
      case 1:
        System.out.println("\tError code 1: query format error");
        return;
      case 2:
        System.out.println("\tError code 2: server failure");
        return;
      case 3:
        System.out.println("\tError code 3: domain name does not exist");
        return;
    }
    System.out.println();

    
    System.out.println("->>ANSWER SECTION<<-");
    if (res.getAnswer() == false) {
      System.out.println("** Non-authorative answer **");
    } else {
      switch(enhancement) {
        case A:
          if (res.getA().size() > 0) {
            for (String answer : res.getA()) {
            System.out.println("\t" + domain + "\tA\t" + answer);
            }
          } else {
            System.out.println("** Can't find answer for " + domain + " **");
            // If no A answer exists, print out possible NS and CNAME answers
            for (String answer : res.getNS()) {
              System.out.println("\t" + domain + "\tNS\t" + answer);
            }
            for (String answer : res.getCNAME()) {
              System.out.println("\t" + domain + "\tCNAME\t" + answer);
            }
          }
          break;
        case NS:
          if (res.getNS().size() > 0) {
            for (String answer : res.getNS()) {
            System.out.println("\t" + domain + "\tNS\t" + answer);
            }
          } else {
            System.out.println("** Can't find ns for " + domain + " **");
          }
          break;
        case CNAME:
          if (res.getCNAME().size() > 0) {
            for (String answer : res.getCNAME()) {
            System.out.println("\t" + domain + "\tCNAME\t" + answer);
            }
          } else {
            System.out.println("** Can't find cname for " + domain + " **");
          }
          break;
        case MX:
          if (res.getMX().size() > 0) {
            for (String answer : res.getMX()) {
            System.out.println("\t" + domain + "\tMX\t" + answer);
            }
          } else {
            System.out.println("** Can't find cname for " + domain + " **");
          }
          break;
        case PTR:
          if (res.getPTR().size() > 0) {
            for (String answer : res.getPTR()) {
            System.out.println("\t" + domain + "\tPTR\t" + answer);
            }
          } else {
            System.out.println("** Can't find cname for " + domain + " **");
          }
          break;
      }

    }

    System.out.println();
  }
}
