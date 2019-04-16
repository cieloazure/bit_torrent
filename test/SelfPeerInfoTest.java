import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

class SelfPeerInfoTest {



    private static Map<Integer, byte[]> splitFileIntoChunks(String fileName, long fileSize, long pieceSize) {
        Map<Integer, byte[]> fileChunks = new HashMap<>();
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[(int) pieceSize];
            int i = 0;
            System.out.println("File size:"+fileSize);
            int x = 0;
            while ((x = fis.read(buffer)) > 0) {
                fileChunks.putIfAbsent(i, buffer);
                System.out.println("Read "+x+" in "+i);
                i++;
                fileSize -= x;
                System.out.println("Remaining:"+fileSize);
                if(fileSize > pieceSize){
                    buffer = new byte[(int) pieceSize];
                }else{
                    buffer = new byte[(int)fileSize];
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileChunks;
    }

    @Test
    void combineFileChunks() {
        PeerInfo.Builder b = new PeerInfo.Builder();
        CommonConfig.Builder cb = new CommonConfig.Builder();
        cb.withFileParametersAs("alice.txt", 152136, 2000);
        CommonConfig c = cb.build();
        b.withPeerID(1);
        Map<Integer, byte[]> fileChunks = splitFileIntoChunks("alice.txt", 152136, 2000);
        SelfPeerInfo p = new SelfPeerInfo(b, false, fileChunks, null, new BitSet(), c);
        p.combineFileChunks();
    }
}