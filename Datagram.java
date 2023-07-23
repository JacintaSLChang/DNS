import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Datagram {
  private DatagramSocket socket;

  private DatagramPacket packet;

  private byte[] packetBytes;

  private short Id;

  public Datagram(DatagramSocket socket, DatagramPacket packet, byte[] bytes) throws IOException {
    this.socket = socket;
    this.packet = packet;
    this.packetBytes = bytes;

    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(this.packetBytes));
    short Id = dataInputStream.readShort();
    this.Id = Id;
  }

  public void setDatagramSocket(DatagramSocket socket) {
    this.socket = socket;
  }

  public void setDatagramPacket(DatagramPacket packet) {
    this.packet = packet;
  }

    public void setPacketBytes(byte[] bytes) {
    this.packetBytes = bytes;
  }

  public DatagramSocket getDatagramSocket() {
    return this.socket;
  }

  public DatagramPacket getDatagramPacket() {
    return this.packet;
  }

  public byte[] getPacketBytes() {
    return this.packetBytes;
  }

  public short getId() {
    return this.Id;
  }
}
