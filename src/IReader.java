import javax.smartcardio.*;

public interface IReader {
  CommandAPDU read(int nBlock);
  CommandAPDU write(int nBlock, byte[] val);
  CommandAPDU load_key(byte[] key, char keyType);
  CommandAPDU auth(int nBlock, char keyType);
}
