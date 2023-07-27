import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

import javax.xml.crypto.Data;

public class Client {
  private static DatagramSocket clientSocket;
  public static void main(String[] args) throws Exception {
    InetAddress IPAddress = InetAddress.getByName(args[0]);
    int serverPort = Integer.parseInt(args[1]);
    String domain = args[2];

    clientSocket = new DatagramSocket();
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
    dataOutputStream.writeShort(QDCOUNT);
    dataOutputStream.writeShort(ANCOUNT);
    dataOutputStream.writeShort(NSCOUNT);
    dataOutputStream.writeShort(ARCOUNT);

    // Write Question section
    // Write QNAME
    // Modification: remove prefix to host name
    domain = domain.replaceFirst("(http://|https://)", "");
    domain = domain.replaceFirst("www.", "");
    String[] domainParts = domain.split("\\.");
    for (int i = 0; i < domainParts.length; i++) {
        byte[] domainBytes = domainParts[i].getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeByte(domainBytes.length);
        dataOutputStream.write(domainBytes);
    }
    dataOutputStream.writeByte(0); // End of QNAME

    dataOutputStream.writeShort(1); // QTYPES (A = 1)
    dataOutputStream.writeShort(1); // QCLASS

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
    System.out.println("\tquery: " + res.getQDCOUNT() + "\tANSWER: " + res.getANCOUNT() 
      + "\tAUTHORITY: " + res.getNSCOUNT() + "\tADDITIONAL: " + res.getARCOUNT());
    System.out.println();

    System.out.println("->>ANSWER SECTION<<-");
    for (String answer : res.getA()) {
      System.out.println(domain + "\t" + answer);
    }
    System.out.println();
  }
}
