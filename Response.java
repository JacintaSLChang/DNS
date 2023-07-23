import java.net.InetAddress;
import java.util.ArrayList;

public class Response {
  private ArrayList<String> NS;
  private ArrayList<String> A;
  private InetAddress dest;
  private Boolean answer;

  public Response(InetAddress dest) {
    NS = new ArrayList<>();
    A = new ArrayList<>();
    this.dest = dest;
    answer = false;
  }

  public void addNS(String ns) {
    this.NS.add(ns);
  }
  
  public void addA(String a) {
    this.A.add(a);
  }

  public void setAnswer(Boolean ans) {
    this.answer = ans;
  }
  
  public ArrayList<String> getNS() {
    return NS;
  }

  public ArrayList<String> getA() {
    return A;
  }

  public InetAddress getDest() {
    return dest;
  }

  public Boolean getAnswer() {
    return answer;
  }
}
