import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Response {
  private int id = 0;
  private String QNAME = "";
  private Boolean answer = false;
  private Boolean TC = false;
  private int RCODE = 0;
  private int QDCOUNT = 0;
  private int ANCOUNT = 0;
  private int NSCOUNT = 0;
  private int ARCOUNT = 0;
  private Datagram datagram = null;

  private static final int A = 1;
  private static final int NS = 2;
  private static final int CNAME = 5;
  private static final int PTR = 12;
  private static final int MX = 15;

  public Response() {
  }

  private ArrayList<String> nsList = new ArrayList<>();
  private ArrayList<String> aList = new ArrayList<>();
  private ArrayList<String> cnameList = new ArrayList<>();
  private ArrayList<String> mxList = new ArrayList<>();
  private ArrayList<String> ptrList = new ArrayList<>();

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getQNAME() {
    return QNAME;
  }

  public void setQNAME(String qNAME) {
    QNAME = qNAME;
  }

  public ArrayList<String> getNS() {
    return nsList;
  }

  public void addNS(String nS) {
    nsList.add(nS);
  }

  public ArrayList<String> getA() {
    return aList;
  }

  public void addA(String a) {
    aList.add(a);
  }

  public ArrayList<String> getCNAME() {
    return cnameList;
  }

  public void addCNAME(String cname) {
    cnameList.add(cname);
  }

    public ArrayList<String> getMX() {
    return mxList;
  }

  public void addMX(String mx) {
    mxList.add(mx);
  }

  public ArrayList<String> getPTR() {
    return ptrList;
  }

  public void addPTR(String ptr) {
    ptrList.add(ptr);
  }

  public Boolean getAnswer() {
    return answer;
  }

  public void setAnswer(Boolean answer) {
    this.answer = answer;
  }

  public Boolean getTC() {
    return this.TC;
  }

  public int getRCODE() {
    return RCODE;
  }
  public int getQDCOUNT() {
    return QDCOUNT;
  }

  public void setQDCOUNT(int qDCOUNT) {
    QDCOUNT = qDCOUNT;
  }

  public int getANCOUNT() {
    return ANCOUNT;
  }

  public void setANCOUNT(int aNCOUNT) {
    ANCOUNT = aNCOUNT;
  }

  public int getNSCOUNT() {
    return NSCOUNT;
  }

  public void setNSCOUNT(int nSCOUNT) {
    NSCOUNT = nSCOUNT;
  }

  public int getARCOUNT() {
    return ARCOUNT;
  }

  public void setARCOUNT(int aRCOUNT) {
    ARCOUNT = aRCOUNT;
  }

  public Datagram getDatagram() {
    return datagram;
  }

  public void setDatagram(Datagram datagram) {
    this.datagram = datagram;
  }

  public void setResponse(int id, String qNAME, int AA, int TC, int RCODE, int qDCOUNT, int aNCOUNT, int nSCOUNT, int aRCOUNT,
    Datagram datagram) {
    this.id = id;
    QNAME = qNAME;
    this.answer = (AA == 1 ? true : false);
    this.TC = (TC == 1 ? true : false);
    this.RCODE = RCODE;
    QDCOUNT = qDCOUNT;
    ANCOUNT = aNCOUNT;
    NSCOUNT = nSCOUNT;
    ARCOUNT = aRCOUNT;
    this.datagram = datagram;
  }

  /*
   * Reads the response from server
   * Arguments:   Datagram    datagram    datagram with the response from server
   * Return:      Response                class Response holding answers from response
   */
  public Response readResPacket(Datagram datagram) throws IOException {
    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(datagram.getPacketBytes()));
    /*
    * Start:
    *  Code referenced from
    *  https://levelup.gitconnected.com/dns-response-in-java-a6298e3cc7d9
    * Modifications:
    *   Removed System.out.println statements
    *   Other - in comments
    */
    // Reads the DNS response
    short id = dataInputStream.readShort();
    short flags = dataInputStream.readByte();
    int QR = (flags & 0b10000000) >>> 7;
    int opCode = ( flags & 0b01111000) >>> 3;
    int AA = ( flags & 0b00000100) >>> 2;
    int TC = ( flags & 0b00000010) >>> 1;
    int RD = flags & 0b00000001;
    flags = dataInputStream.readByte();

    int RA = (flags & 0b10000000) >>> 7;
    int Z = ( flags & 0b01110000) >>> 4;
    int RCODE = flags & 0b00001111;

    short QDCOUNT = dataInputStream.readShort();
    short ANCOUNT = dataInputStream.readShort();
    short NSCOUNT = dataInputStream.readShort();
    short ARCOUNT = dataInputStream.readShort();

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

    byte firstBytes = dataInputStream.readByte();
    int firstTwoBits = (firstBytes & 0b11000000) >>> 6;

    // Modification: create a Response class
    Boolean AABool = (AA == 1 ? true : false);
    setResponse(id, QNAME, AA, TC, RCODE, QDCOUNT, ANCOUNT, NSCOUNT, ARCOUNT, datagram);

    // Modification: iterate through all of Answer, Authority and Additional sections
    for (int i = 0; i < NSCOUNT + ANCOUNT + ARCOUNT; i++) {
      if (firstTwoBits == 3) {
        dataInputStream.readByte();
        short TYPE = dataInputStream.readShort();
        short CLASS = dataInputStream.readShort();
        int TTL = dataInputStream.readInt();
        int RDLENGTH = dataInputStream.readShort();

        ArrayList<Integer> RDATA = new ArrayList<>();        
        for(int s = 0; s < RDLENGTH; s++) {
            int nx = dataInputStream.readByte() & 0xff;
            // RDATA.add(nx);
            // check offset
            if ((TYPE == NS || TYPE == CNAME) && nx == 0xc0) {
              int offset = dataInputStream.readByte() & 0xff;
              s = RDLENGTH;
              byte[] label = Arrays.copyOfRange(datagram.getPacketBytes(), offset, datagram.getPacketBytes().length - 1);
              for (int curr = 0; label[curr] != 0; curr++) {
                int letter = label[curr] & 0xff;
                if (letter < 33 || letter > 127) { letter = 0x2e; }
                RDATA.add(letter);
              }
            } else {
              RDATA.add(nx);
            }
        }

        firstBytes = dataInputStream.readByte();
        firstTwoBits = (firstBytes & 0b11000000) >>> 6;

        // Modification: Store RDATA as a string depending on type and store 
        //  it in Response
        StringBuilder ip = new StringBuilder();
        String string = "";
        switch(TYPE) {
          case A:
            for(Integer ipPart:RDATA) {
                ip.append(ipPart).append(".");
            }
            string = ip.toString();
            string = string.substring(0, string.length() - 1);
            addA(string);
            break;
          case NS:
            for(Integer ipPart:RDATA) {
              ip.append(Character.toChars(ipPart));
            }
            string = ip.toString(); 
            addNS(string);
            break;
          case CNAME:
            for(Integer ipPart:RDATA) {
              ip.append(Character.toChars(ipPart));
            }
            string = ip.toString(); 
            addCNAME(string);
          case MX:
            for(Integer ipPart:RDATA) {
              ip.append(Character.toChars(ipPart));
            }
            string = ip.toString();             
            addMX(string);
          case PTR:
            for(Integer ipPart:RDATA) {
              ip.append(Character.toChars(ipPart));
            }
            string = ip.toString();             
            addPTR(string);
        }    
      }
    }
    /*
    * End:
    *  Code referenced from
    *  https://levelup.gitconnected.com/dns-response-in-java-a6298e3cc7d9
    */      
    return this;
  }

}
