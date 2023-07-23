import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat.Type;

public class Resolver {
  private static String rootHintsFile = "named.root";

  private static List<String> rootServers; 

  private static Response[] rootResponses;
  
  private static final int dnsPort = 53;

  private static Response finalResponse;

  public static void main(String[] args) throws IOException {
    // Read the roots file
    readRootHintsFile();
    rootResponses = new Response[rootServers.size()];

    // Establish socket with client
    Datagram clientDatagram = estClientSocket(args[0]);
    
    // Find responses from root server
    int i = 0;
    for (String rootServer : Resolver.rootServers) {
      rootResponses[i] = queryDnsServer(clientDatagram, rootServer);
      i++;
    }

    // Search for answer
    Response res = new Response(null);
    for (i = 0; i < rootResponses.length; i++) {
       Response rootRes = rootResponses[i];
      if (rootRes != null && rootRes.getA().size() > 0) {
        res = rootRes;
        break;
      }
    }

    System.out.println("we got to res " + res.getDest());
    System.out.println("Answer is: " + res.getA());
        System.out.println("Size is: " + res.getA().size());

    // System.out.println(res.getA().get(12));
    // queryDnsServer(clientDatagram, res.getA().get(0));
    // queryDnsServer(clientDatagram, res.getA().get(1));
    // queryDnsServer(clientDatagram, res.getA().get(2));
    // queryDnsServer(clientDatagram, res.getA().get(3));
    // System.out.println(clientDatagram.getPacketBytes().length);
    for (i = 0; i < res.getA().size(); i++) {
      res = queryDnsServer(clientDatagram, res.getA().get(i));
      if (res != null) {
        break;
      }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///////////////////////// PROBLEM HERE ///////////////////////////
    ////////////////// why doesn't size of res carry between methods? //////////////////////
    ////////////////////////////////////////////////////////////////////////////

    finalResponse = getAnsRec(clientDatagram, res);

    System.out.println(finalResponse.getA());

  }
  
  public static Response getAnsRec(Datagram clientDatagram, Response res) throws IOException {
            System.out.println("Size is: " + res.getA().size());

    Response currRes = res;
    for (int i = 0; i < res.getA().size(); i++) {
      currRes = queryDnsServer(clientDatagram, res.getA().get(i));
      System.out.println(currRes.getA().size());
      if (currRes.getAnswer() == true) { 
        return currRes;
      } else if (currRes.getA().size() > 0) {
        return getAnsRec(clientDatagram, currRes);
      }
    }
    return res;
  }
  
  public static Response queryDnsServer(Datagram clientDatagram, String destIp) throws IOException {
      InetAddress dnsAddr = InetAddress.getByName(destIp);

      // Create new socket to connect to DNS servers
      DatagramSocket dnsSocket = new DatagramSocket();
      DatagramPacket dnsPacket = new DatagramPacket(clientDatagram.getPacketBytes(), clientDatagram.getPacketBytes().length, dnsAddr, dnsPort);
      try {
        dnsSocket.send(dnsPacket);
      } catch (Exception e) {
        System.out.println("Error sending packet: " + e);
      }
      // Receive response
      byte[] dnsResBytes = new byte[512];
      DatagramPacket dnsResPacket = new DatagramPacket(dnsResBytes ,dnsResBytes.length);

      dnsSocket.setSoTimeout(2000);
      try {
        dnsSocket.receive(dnsResPacket);
      } catch (SocketTimeoutException e) {
        System.out.println("Timeout Error: " + e);
        dnsSocket.close();
        return null;
      }

      // Read response
      Datagram dnsDatagram = new Datagram(dnsSocket, dnsResPacket, dnsResBytes);

      // Check Id from Dns response with client Id
      if (clientDatagram.getId() != dnsDatagram.getId()) {
        return null;
      }

      // Get responses from datagram
      Response res = new Response(dnsAddr);
      res = readResPacket(dnsDatagram);

      System.out.println("Received: " + dnsResPacket.getAddress() + " " + dnsResPacket.getLength() + " bytes");
      dnsSocket.close();

    return res;
  }

  public static Response readResPacket(Datagram datagram) throws IOException {
    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(datagram.getPacketBytes()));

    // Code from https://levelup.gitconnected.com/dns-response-in-java-a6298e3cc7d9 start
    // Reads the DNS response
    System.out.println("\n\nStart response decode");
        System.out.println("Transaction ID: " + dataInputStream.readShort()); // ID
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

        short QDCOUNT = dataInputStream.readShort();
        short ANCOUNT = dataInputStream.readShort();
        short NSCOUNT = dataInputStream.readShort();
        short ARCOUNT = dataInputStream.readShort();

        // System.out.println("Questions: " + String.format("%s",QDCOUNT ));
        // System.out.println("Answers RRs: " + String.format("%s", ANCOUNT));
        // System.out.println("Authority RRs: " + String.format("%s", NSCOUNT));
        // System.out.println("Additional RRs: " + String.format("%s", ARCOUNT));

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

        Response res = new Response(datagram.getDatagramPacket().getAddress());

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
                // System.out.println("IP:" + string);
                res.addA(string);
                break;
              case 2: // NS record
                for(Integer ipPart:RDATA) {
                    ip.append(Character.toChars(ipPart));
                }
                string = ip.toString();
                // System.out.println("NameServer:" + string);
                res.addNS(string);
            }
            res.setAnswer(AA == 1 ? true : false);
            // System.out.println();
          }
        }
      // Code from https://levelup.gitconnected.com/dns-response-in-java-a6298e3cc7d9 end

        return res;
      ////// Create a class that holds the type and the response
      // that way you know how to read the response (either as an IPV4 for A or a name server name for NS)
  }


  public static Datagram estClientSocket(String port) throws IOException {
    int resolverPort = Integer.parseInt(port);

		DatagramSocket serverSocket = new DatagramSocket(resolverPort);
    System.out.println("Server is ready :" + resolverPort);

    byte[] clientQueryBytes = new byte[512];
    DatagramPacket clientQuery = new DatagramPacket(clientQueryBytes, clientQueryBytes.length);
    serverSocket.receive(clientQuery);

    Datagram datagram = new Datagram(serverSocket, clientQuery, clientQueryBytes);

    return datagram;
  }

  public static void readRootHintsFile() {
  try {
    // Read bytes from named.root
    BufferedReader reader = new BufferedReader(new FileReader(rootHintsFile));
    String line = new String();
    
    rootServers = new ArrayList<>();

    // Read each line from roots file
    while ((line = reader.readLine()) != null) {
      // Only get A and AAAA records
      if (!(line.substring(0,1).equals(";") 
      || line.substring(0,1).equals("."))) {
        // Extract IPV4 addresses
        String[] words = line.split("\\s+");
        if (words[2].equals("A")) {
          rootServers.add(words[3]);
        }
      }
    }
    reader.close();
  } catch (Exception FileNotFoundException) {
    System.out.println("Could not find " + rootHintsFile);
  }
}
}
