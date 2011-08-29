import org.apache.commons.lang3.*;
import java.util.*;
import javax.smartcardio.*;
import java.security.*;
import org.json.*;
import java.applet.Applet;

public class MifareApplet extends Applet {

  public static final char KEY_A = 'a';
  public static final char KEY_B = 'b';
  public static final char KEY_F = 'f';
  
  public static final String PROTOCOL_T0  = "T=0";
  public static final String PROTOCOL_T1  = "T=1";
  public static final String PROTOCOL_TCL = "T=CL";

  private Map<Character,Byte> mapKeyType = null;

  private String protocol           = PROTOCOL_T1; //T=0, T=1, T=CL

  private CardTerminal terminal = null;
  private Card         card     = null;

  public String terminals() {  
    System.out.println("Lectores");
    List<String> l = new ArrayList<String>();
    try {
      CardTerminals tList = TerminalFactory.getDefault().terminals();
      for(Iterator<CardTerminal> it = tList.list().iterator(); it.hasNext();){
        l.add(it.next().getName());
      }
    }catch(CardException e){
      e.printStackTrace();
    }
    JSONArray json = new JSONArray(l);
    return json.toString();
  }


  public void setTerminal(String terminal){
    this.terminal = TerminalFactory.getDefault().terminals().getTerminal(terminal);
  }

  public String getTerminal(){
    return this.terminal.toString();
  }

  public String getProtocol(){
    return this.protocol;
  }

  public void setProtocol(String protocol){
    this.protocol = protocol;
  }


  /**************************************************************************
   *
   *                         CARDS OPERATIONS
   *
   **************************************************************************/

  /**
   * Reads block from terminal
   */
  public String read(int nBlock) throws CardException{
    System.out.println("Read");
    byte[] cmd = new byte[]{(byte)0xff, (byte)0xb0, 0x00, (byte)nBlock, 0x00};
    ResponseAPDU r = send(new CommandAPDU(cmd));
    return APDUtoJSON(r).toString();
  }
  
  /**
   * Load key
   */
  public String load_key(byte[] key, char keyType) throws CardException{
    System.out.println("Load Key");
    byte[] cmd = new byte[]{(byte)0xff, (byte)0x82, 0x00, mapKeyType(keyType), 0x06};
    cmd = ArrayUtils.addAll(cmd,key);

    ResponseAPDU r = send(new CommandAPDU(cmd));
    return APDUtoJSON(r).toString();
  }
  
  /**
   * Auth
   */
  public String auth(int nBlock, char keyType) throws CardException{
    System.out.println("Auth");
    byte[] cmd = new byte[]{(byte)0xff, (byte)0x86, 0x00, 0x00, 0x05, 0x01, 0x00, (byte)nBlock, mapKeyType(keyType), 0x00};
    ResponseAPDU r = send(new CommandAPDU(cmd));
    return APDUtoJSON(r).toString();
  }
  
  /**
   * Writes
   */
  public String write(int nBlock, byte[] val) throws CardException{
    System.out.println("Write");
    byte[] cmd = new byte[]{(byte)0xff, (byte)0xd6, 0x00, (byte)nBlock};
    cmd = ArrayUtils.addAll(cmd, val);

    ResponseAPDU r = send(new CommandAPDU(cmd));
    return APDUtoJSON(r).toString();
  }

  /**********************************************************************************
   *
   *        Transactions
   *
   * ********************************************************************************/

  public void beginTransaction() throws CardException{
    try{
      AccessController.doPrivileged(
        new PrivilegedExceptionAction<Object>(){
          public Object run() throws CardException{
            try{
              card    = terminal.connect(protocol);
              return null;
            }catch(CardException e){
              throw e;
            }
          }
        }
      );
    }catch(PrivilegedActionException e){
      e.printStackTrace();
      throw (CardException)e.getException();
    }
  }

  public void endTransaction(){
    try{
      AccessController.doPrivileged(
        new PrivilegedExceptionAction<Object>(){
          public Object run() throws CardException{
            try{
              if(card != null) card.disconnect(false);
              return null;
            }catch(CardException e){
              throw e;
            }finally{
              card = null;
            }
          }
        }
      );
    }catch(PrivilegedActionException e){
      e.printStackTrace();
    }
  }

  /**
   * Sends command APDU to the terminal and receives the response into another APDU.
   */
  private ResponseAPDU send(CommandAPDU cmdApdu) throws CardException{
    final CommandAPDU apdu = cmdApdu;

    try{
      ResponseAPDU r = (ResponseAPDU)AccessController.doPrivileged(
          new PrivilegedExceptionAction<ResponseAPDU>(){
            public ResponseAPDU run() throws CardException{
             
              try{
                ResponseAPDU r = card.getBasicChannel().transmit(apdu);
                return r;
              }catch(CardException e){
                e.printStackTrace();
                throw e;
              }
            }
          }
      );
      return r;
    }catch(PrivilegedActionException e){
      e.printStackTrace();
      throw (CardException)e.getException();
    }
  }

  /**
   * Serializes an apdu to JSON
   */
  private JSONObject APDUtoJSON(ResponseAPDU apdu){
    JSONObject json = new JSONObject();
    JSONObject data = new JSONObject();

    try{
      if(apdu.getSW() == 0x9000) 
        json.put("success", true);
      else 
        json.put("success", false);
      
      data.put("SW1",    apdu.getSW1());
      data.put("SW2",    apdu.getSW2());
      data.put("data",   byteArrayToUnsigned(apdu.getData()));

      json.put("apdu", data);
    }catch(JSONException e){
      e.printStackTrace();
    }

    return json;
  }

  /**
   * Converts a, b, f to key type values
   */
  private byte mapKeyType(char keyType){
    if(mapKeyType == null){
      mapKeyType = new Hashtable<Character,Byte>();
      mapKeyType.put(new Character(KEY_A), new Byte((byte)0x60));
      mapKeyType.put(new Character(KEY_B), new Byte((byte)0x61));
      mapKeyType.put(new Character(KEY_F), new Byte((byte)0xFF));
    }

    return mapKeyType.get(new Character(keyType)).byteValue();
  }

  /**
   * Converts signed byte array to int array (unsigned byte representation)
   */
  private int[] byteArrayToUnsigned(byte[] bArray){
    int[] iArray = new int[bArray.length];

    for(int i=0; i<iArray.length; i++){
      iArray[i] = (0xff & bArray[i]);
    }

    return iArray;
  }
}
