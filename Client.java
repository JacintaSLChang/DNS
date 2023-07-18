// References:
// https://levelup.gitconnected.com/dns-request-and-response-in-java-acbd51ad3467

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Client {
  public static void main(String[] args) throws Exception {
    System.out.println(args[0]);
  InetAddress IPAddress = InetAddress.getByName(args[0]);// cloudflare
  int serverPort = Integer.parseInt(args[1]);

  // Create request ID
  Random random = new Random();
  short ID = (short)random.nextInt(32767);
  System.out.println("ID is: " + ID);

  // Create a byte stream that you can write to
  ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
  DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

  // Set request flags
  String requestFlags = "0"; // QR (request = 0, res = 1)
  requestFlags += "0000"; // OPCode (QUERY = 0, IQUERY = 1, STATUS = 2)
  requestFlags += "0";    // AA (not authorative = 0, for request)
  requestFlags += "0";    // TC (not truncated = 0, truncated = 1)
  requestFlags += "0";    // RD (no recursion = 0, recursion desired = 1)
  requestFlags += "0";    // RA (recursion unavailable = 0, for request)
  requestFlags += "000";  // Z (for future use)
  requestFlags += "0000"; // RDCODE (no error = 0, for request)
  short shortFlags = Short.parseShort(requestFlags, 2);

  // Set next section
  short QDCOUNT = 1;
  short ANCOUNT = 0;
  short NSCOUNT = 0;
  short ARCOUNT = 0;

  dataOutputStream.writeShort(ID);
  dataOutputStream.writeShort(shortFlags);
  dataOutputStream.writeShort(QDCOUNT);
  dataOutputStream.writeShort(ANCOUNT);
  dataOutputStream.writeShort(NSCOUNT);
  dataOutputStream.writeShort(ARCOUNT);

  // Write Question section
  // Write QNAME
  String domain = args[2];
  domain.replaceFirst("www.|http://|https://", "");
  String[] domainParts = domain.split("\\.");
  for (int i = 0; i < domainParts.length; i++) {
      byte[] domainBytes = domainParts[i].getBytes(StandardCharsets.UTF_8);
      dataOutputStream.writeByte(domainBytes.length);
      dataOutputStream.write(domainBytes);
  }
  dataOutputStream.writeByte(0); // End of QNAME
  dataOutputStream.writeShort(1); // QTYPES (A = 1)
  dataOutputStream.writeShort(1); // QCLASS

  byte[] dnsFrame = byteArrayOutputStream.toByteArray();

  // Print out entire datagraph packet
  System.out.println("Sending: " + dnsFrame.length + " bytes");
  for (int i = 0; i < dnsFrame.length; i++) {
      System.out.print(String.format("%s", dnsFrame[i]) + " ");
  }

  // Create sockets to send/receive via UDP
  DatagramSocket clientSocket = new DatagramSocket();
  DatagramPacket dnsReqPacket = new DatagramPacket(dnsFrame, dnsFrame.length, IPAddress, serverPort);
  clientSocket.send(dnsReqPacket);

  clientSocket.close();
  }
}
