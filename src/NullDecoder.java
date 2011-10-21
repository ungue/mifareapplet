class NullDecoder implements IDecoder {
  
  public NullDecoder(){
    System.out.println("Null Decoder loaded");
  }

  public byte[] decode(byte[] bytes){
    return bytes;
  }
}
