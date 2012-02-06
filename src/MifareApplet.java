import org.apache.commons.lang3.*;
import java.util.*;
import java.io.*;
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

  public static final int E_NO_ERROR                 = 0x9000;
  public static final int E_LENGTH_INCORRECT         = 0x6700;
  public static final int E_INVALID_INSTRUCTION_BYTE = 0x6D00;
  public static final int E_CLASS_NOT_SUPPORTED      = 0x6E00;
  public static final int E_UNKNOWN_COMMAND          = 0x6F00;
  public static final int E_NO_INFORMATION_GIVEN     = 0x6300;
  public static final int E_MEMORY_FAILURE           = 0x6581;
  public static final int E_CLASS_BYTE_INCORRECT     = 0x6800;
  public static final int E_FUNCTION_NOT_SUPPORTED   = 0x6A81;
  public static final int E_WRONG_PARAMETER          = 0x6B00;

  private Properties readersProperties = null;
  private Properties configProperties = null;
  private boolean isVolatile = true;

  private String protocol           = PROTOCOL_T1; //T=0, T=1, T=CL

  private CardTerminal terminal = null;
  private Card         card     = null;

  private Exception lastException = null;

  private IReader reader = null;
  private IDecoder decoder = null;

  public void init(){
    reloadReadersProperties();
    reloadConfigProperties();
    
    initFromParameters();
  }

  public String terminals() {  
    List<String> l = new ArrayList<String>();
    try {
      CardTerminals tList = TerminalFactory.getDefault().terminals();
      for(Iterator<CardTerminal> it = tList.list().iterator(); it.hasNext();){
        l.add(it.next().getName());
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    JSONArray json = new JSONArray(l);
    return json.toString();
  }


  public void setTerminal(String terminal){
    this.terminal = TerminalFactory.getDefault().terminals().getTerminal(terminal);
    loadReader();
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

  public String getLastException(){
    String res = null;
    if(this.lastException != null){
      res = ExceptionToJSON(this.lastException).toString();
    }
    return res;
  }

  /**************************************************************************
   *
   *                         CARDS OPERATIONS
   *
   **************************************************************************/

  /**
   * Checks if a card is present on terminal
   */  
  public boolean isCardPresent(){
    boolean present = false;

    try{
      present = this.terminal.isCardPresent();
    }catch(Exception e){
      e.printStackTrace();
    }

    System.out.println("Card present?:" + (present ? "YES" : "NO"));
    return present;
  }

  /**
   * Reads block from terminal
   */
  public String read(int nBlock){
    String res = null;

    try{
      cleanLastException();
      System.out.print("Read: ");
    
      CommandAPDU apdu = this.reader.read(nBlock);
      showAPDU(apdu.getBytes());
      ResponseAPDU r = send(apdu);

      res = APDUtoJSON(r).toString();

    }catch(Exception e){
      this.lastException = e;
    }finally{
      return res;
    }
  }
  
  /**
   * Load key
   */
  public String load_key(byte[] key, char keyType){
    String res = null;
    
    try{
      cleanLastException();
      System.out.println("Load Key");

      CommandAPDU apdu = this.reader.load_key(this.decoder.decode(key), keyType);
      showAPDU(apdu.getBytes());
      ResponseAPDU r = send(apdu);

      res = APDUtoJSON(r).toString();
    }catch(Exception e){
      this.lastException = e;
    }finally{
      return res;
    }
  }
  
  /**
   * Auth
   */
  public String auth(int nBlock, char keyType){
    String res = null;

    try{
      cleanLastException();
      System.out.println("Auth");
      
      CommandAPDU apdu = this.reader.auth(nBlock, keyType);
      showAPDU(apdu.getBytes());
      ResponseAPDU r = send(apdu);

      res = APDUtoJSON(r).toString();
    }catch(Exception e){
      this.lastException = e;
    }finally{
      return res;
    }
  }
  
  /**
   * Writes
   */
  public String write(int nBlock, byte[] val){
    String res = null;

    try{
      cleanLastException();
      System.out.println("Write");

      CommandAPDU apdu = this.reader.write(nBlock, val);
      showAPDU(apdu.getBytes());
      ResponseAPDU r = send(apdu);

      res = APDUtoJSON(r).toString();
    }catch(Exception e){
      this.lastException = e;
    }finally{
      return res;
    }
  }

  /**********************************************************************************
   *
   *        Transactions
   *
   * ********************************************************************************/

  public void beginTransaction(){
    try{
      cleanLastException();
      AccessController.doPrivileged(
        new PrivilegedExceptionAction<Object>(){
          public Object run() throws CardException{
            try{
              card = terminal.connect(protocol);
              return null;
            }catch(CardException e){
              throw e;
            }
          }
        }
      );
    }catch(PrivilegedActionException e){
      e.printStackTrace();
      this.lastException = e.getException();
    }
  }

  public void endTransaction(){
    try{
      cleanLastException();
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
      if(apdu.getSW() == E_NO_ERROR){
        json.put("success", true);
      }else{
        json.put("success", false);
        json.put("error", apdu.getSW());
      }
      
      data.put("data",   byteArrayToUnsigned(apdu.getData()));

      json.put("apdu", data);
    }catch(JSONException e){
      e.printStackTrace();
    }

    return json;
  }

  /**
   * Serializes an Exception to JSON
   */
  private JSONObject ExceptionToJSON(Exception exc){
    JSONObject json = new JSONObject();

    try{
      json.put("type", exc.getClass().getName());
      json.put("message", exc.getMessage());
    }catch(JSONException e){
      e.printStackTrace();
    }

    return json;
  }

  private void cleanLastException(){
    this.lastException = null;
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
  
  /**
   * Reload readers properties
   */
  private void reloadReadersProperties(){
    try{
      if(readersProperties == null){
	      readersProperties = new Properties();
	      readersProperties.load(getClass().getResourceAsStream("readers.properties"));
      }
    }catch(IOException e){
      e.printStackTrace();
    }
  }

  /**
   * Loads IReader from readers.properties
   */
  private void loadReader(){
    System.out.println("Searching in properties " + this.terminal.getName());
    String strReader = readersProperties.getProperty(this.terminal.getName().toUpperCase(), "DefaultReader");
    System.out.println("Loading " + strReader);
    try{
      this.reader = (IReader)getClass().getClassLoader().loadClass(strReader).newInstance();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reload config properties
   */
  private void reloadConfigProperties(){
    try{
      if(configProperties == null){
	      configProperties = new Properties();
	      configProperties.load(getClass().getResourceAsStream("config.properties"));

        // Instantiates all needed classes
        loadConfig();
      }
    }catch(IOException e){
      e.printStackTrace();
    }
  }
  
  /**
   * Loads Config from config.properties
   */
  private void loadConfig(){
    String strDecoder = configProperties.getProperty("mifareapplet.keys.decoder", "NullDecoder");

    try{
      this.decoder = (IDecoder)getClass().getClassLoader().loadClass(strDecoder).newInstance();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Loads parameters terminal and protocol to initialize them into accessors 
   */
  private void initFromParameters(){
    String terminal = this.getParameter("terminal");
    String protocol = this.getParameter("protocol");

    if (terminal != null) this.setTerminal(terminal);
    if (protocol != null) this.setProtocol(protocol);
  }

  /**
   * Displays APDU
   */
  private void showAPDU(byte[] apdu){
    for(int i=0; i<apdu.length; i++){
      System.out.print(Integer.toString(0xff & apdu[i], 16));
      System.out.print(' ');
    }
    System.out.println();
  }

}
