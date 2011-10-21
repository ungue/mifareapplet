import javax.crypto.*;
import javax.crypto.spec.*;

class DESDecoder implements IDecoder {

  private byte[] key = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 }; // Hardcoded DES key 8 bytes

  private Cipher cipher;
  private SecretKey secretKey;

  public DESDecoder(){
    System.out.println("DES/ECB/PKCS5Padding Decoder loaded");

    try{
      cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
	  	secretKey = SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(key));
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public byte[] decode(byte[] bytes){
    try{
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      return cipher.doFinal(bytes);
    }catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }

  /**
   * This whould be the code to encrypt data on the server side
   */
  public byte[] encode(byte[] bytes){
    try{
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      return cipher.doFinal(bytes);
    }catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }

}
