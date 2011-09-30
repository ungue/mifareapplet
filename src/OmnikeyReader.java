import org.apache.commons.lang3.*;
import java.util.*;
import javax.smartcardio.*;

public class OmnikeyReader implements IReader {   
  
  private Map<Character,Byte> mapKeyType = null;

  public OmnikeyReader(){
    System.out.println("Omnikey reader loaded");
  }

  public CommandAPDU read(int nBlock){
    byte[] cmd = new byte[]{(byte)0xff, (byte)0xb0, 0x00, (byte)nBlock, 0x00};

    return new CommandAPDU(cmd);
  }
  
  public CommandAPDU load_key(byte[] key, char keyType){
    byte[] cmd = new byte[]{(byte)0xff, (byte)0x82, 0x20, 0x00, 0x06};
    cmd = ArrayUtils.addAll(cmd,key);

    return new CommandAPDU(cmd);
  }
  
  public CommandAPDU auth(int nBlock, char keyType){
    byte[] cmd = new byte[]{(byte)0xff, (byte)0x86, 0x00, 0x00, 0x05, 0x01, 0x00, (byte)nBlock, mapKeyType(keyType), 0x00};
    
    return new CommandAPDU(cmd);
  }
  
  public CommandAPDU write(int nBlock, byte[] val){
    byte[] cmd = new byte[]{(byte)0xff, (byte)0xd6, 0x00, (byte)nBlock, 0x10};
    cmd = ArrayUtils.addAll(cmd, val);

    return new CommandAPDU(cmd);
  }

  /**
   * Converts a, b, f to key type values
   */
  private byte mapKeyType(char keyType){
    if(mapKeyType == null){
      mapKeyType = new Hashtable<Character,Byte>();
      mapKeyType.put(new Character(MifareApplet.KEY_A), new Byte((byte)0x60));
      mapKeyType.put(new Character(MifareApplet.KEY_B), new Byte((byte)0x61));
      mapKeyType.put(new Character(MifareApplet.KEY_F), new Byte((byte)0xFF));
    }

    return mapKeyType.get(new Character(keyType)).byteValue();
  }
}
