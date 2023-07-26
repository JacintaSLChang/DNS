import java.util.ArrayList;

public class Response {
  private int id;
  private String QNAME;
  private ArrayList<String> NS;
  private ArrayList<String> A;
  private String dest;
  private Boolean answer;
  private int QDCOUNT;
  private int ANCOUNT;
  private int NSCOUNT;
  private int ARCOUNT;
  private Datagram datagram;

  public Response(String dest) {
    id = 0;
    QNAME = "";
    NS = new ArrayList<>();
    A = new ArrayList<>();
    this.dest = dest;
    answer = false;
    QDCOUNT = 0;
    ANCOUNT = 0;
    NSCOUNT = 0;
    ARCOUNT = 0;
    datagram = null;
  }

  public Response() {
    id = 0;
    NS = new ArrayList<>();
    A = new ArrayList<>();
    this.dest = null;
    answer = false;
    QDCOUNT = 0;
    ANCOUNT = 0;
    NSCOUNT = 0;
    ARCOUNT = 0;
  }

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
    return NS;
  }

  public void addNS(String nS) {
    NS.add(nS);
  }

  public ArrayList<String> getA() {
    return A;
  }

  public void addA(String a) {
    A.add(a);
  }

  public String getDest() {
    return dest;
  }

  public void setDest(String dest) {
    this.dest = dest;
  }

  public Boolean getAnswer() {
    return answer;
  }

  public void setAnswer(Boolean answer) {
    this.answer = answer;
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

}
