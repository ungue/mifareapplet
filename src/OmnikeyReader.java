import org.apache.commons.lang3.*;
import java.util.*;
import javax.smartcardio.*;

public class OmnikeyReader extends DefaultReader {   

  public OmnikeyReader(){
    System.out.println("Omnikey reader loaded");
  }

  public CommandAPDU load_key(byte[] key, char keyType){
    byte[] cmd = new byte[]{(byte)0xff, (byte)0x82, 0x20, 0x00, 0x06};
    cmd = ArrayUtils.addAll(cmd,key);

    return new CommandAPDU(cmd);
  }
}
