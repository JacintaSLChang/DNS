// References:
// https://levelup.gitconnected.com/dns-request-and-response-in-java-acbd51ad3467

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

public class Client {
  public static void main(String[] args) throws Exception {
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
    requestFlags += "1";    // RD (no recursion = 0, recursion desired = 1)
    requestFlags += "0";    // RA (recursion unavailable = 0, for request)
    requestFlags += "010";  // Z (for future use)
    requestFlags += "0000"; // RDCODE (no error = 0, for request)
    short shortFlags = Short.parseShort(requestFlags, 2);
    // "0000000100000000"

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

    byte[] dnsFrame = byteArrayOutputStream.toByteArray();

    // Print out entire datagraph packet
    System.out.println("Sending: " + dnsFrame.length + " bytes");
    for (int i = 0; i < dnsFrame.length; i++) {
        System.out.print(String.format("%x", dnsFrame[i]) + " ");
    }

    // Create sockets to send/receive via UDP
    DatagramSocket clientSocket = new DatagramSocket();
    DatagramPacket dnsReqPacket = new DatagramPacket(dnsFrame, dnsFrame.length, IPAddress, serverPort);
    clientSocket.send(dnsReqPacket);


    // ///////////////////////////////////////////////////////////////////////////
    // Receive response
    byte[] dnsResBytes = new byte[512];
    DatagramPacket dnsResPacket = new DatagramPacket(dnsResBytes ,dnsResBytes.length);

    clientSocket.setSoTimeout(20000);
      try {
        clientSocket.receive(dnsResPacket);
      } catch (SocketTimeoutException e) {
        System.out.println("Timeout Error: " + e);
        clientSocket.close();
        return;
      }
    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(dnsResBytes));

    // Code from https://levelup.gitconnected.com/dns-response-in-java-a6298e3cc7d9 start
    // Reads the DNS response
    // System.out.println("\n\nStart response decode");
        short Id = dataInputStream.readShort(); // ID
        // System.out.println("Transaction ID: " + dataInputStream.readShort()); // ID
        short flags = dataInputStream.readByte();
        int QR = (flags & 0b10000000) >>> 7;
        int opCode = ( flags & 0b01111000) >>> 3;
        int AA = ( flags & 0b00000100) >>> 2;
        int TC = ( flags & 0b00000010) >>> 1;
        int RD = flags & 0b00000001;
        // System.out.println("QR "+QR);
        // System.out.println("Opcode "+opCode);
        // System.out.println("AA "+AA);
        // System.out.println("TC "+TC);
        // System.out.println("RD "+RD);
        flags = dataInputStream.readByte();
        int RA = (flags & 0b10000000) >>> 7;
        int Z = ( flags & 0b01110000) >>> 4;
        int RCODE = flags & 0b00001111;
        // System.out.println("RA "+RA);
        // System.out.println("Z "+ Z);
        // System.out.println("RCODE " +RCODE);

         QDCOUNT = dataInputStream.readShort();
         ANCOUNT = dataInputStream.readShort();
         NSCOUNT = dataInputStream.readShort();
         ARCOUNT = dataInputStream.readShort();

        // System.out.println("Questions: " + String.format("%s",QDCOUNT ));
        System.out.println("Answers RRs: " + String.format("%s", ANCOUNT));
        System.out.println("Authority RRs: " + String.format("%s", NSCOUNT));
        System.out.println("Additional RRs: " + String.format("%s", ARCOUNT));

        String QNAME = "";
        int recLen;
        while ((recLen = dataInputStream.readByte()) > 0) {
            byte[] record = new byte[recLen];
            for (int i = 0; i < recLen; i++) {
                record[i] = dataInputStream.readByte();
            }
            QNAME = new String(record);
        }
        short QTYPE = dataInputStream.readShort();
        short QCLASS = dataInputStream.readShort();
        // System.out.println("Record: " + QNAME);
        // System.out.println("Record Type: " + String.format("%s", QTYPE));
        // System.out.println("Class: " + String.format("%s", QCLASS));

        // System.out.println("\n\nstart answer, authority, and additional sections\n");

        byte firstBytes = dataInputStream.readByte();
        int firstTwoBits = (firstBytes & 0b11000000) >>> 6;

        for (int i = 0; i < NSCOUNT + ANCOUNT + ARCOUNT; i++) {
          if (firstTwoBits == 3) {
            dataInputStream.readByte();
            ArrayList<Integer> RDATA = new ArrayList<>();
            short TYPE = dataInputStream.readShort();
            short CLASS = dataInputStream.readShort();
            int TTL = dataInputStream.readInt();
            int RDLENGTH = dataInputStream.readShort();
            // System.out.println("RDLEN: " + RDLENGTH);
            for(int s = 0; s < RDLENGTH; s++) {
                int nx = dataInputStream.readByte() & 0xff;
                RDATA.add(nx);
            }
            // System.out.println("Type: " + TYPE);
            // System.out.println("Class: " + CLASS);
            // System.out.println("Time to live: " + TTL);
            // System.out.println("Rd Length: " + RDLENGTH);

            firstBytes = dataInputStream.readByte();
            firstTwoBits = (firstBytes & 0b11000000) >>> 6;

            StringBuilder ip = new StringBuilder();
            String string = "";
            switch (TYPE) {
              case 1: // A record
                for(Integer ipPart:RDATA) {
                    ip.append(ipPart).append(".");
                }
                string = ip.toString();
                string = string.substring(0, string.length() - 1);
                System.out.println("IP:" + string);
                break;
              case 2: // NS record
                for(Integer ipPart:RDATA) {
                    ip.append(Character.toChars(ipPart));
                }
                string = ip.toString();
                System.out.println("NameServer:" + string);
            }
            // res.setAnswer(AA == 1 ? true : false);
            // System.out.println();
          }
        }
    //     ///////////////////////////////////////////////////////////////////////////
    clientSocket.close();
    }
}
