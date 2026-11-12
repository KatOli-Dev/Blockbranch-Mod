package blockbranch.realistic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Frozen published-270 boreal placement stays byte-for-byte reproducible. */
public final class BorealSlopeCheck {
    public static void main(String[] args)throws Exception{
        try(var input=new BufferedReader(new InputStreamReader(Objects.requireNonNull(BorealSlopeCheck.class.getResourceAsStream("/fixtures/boreal-slope-270/oracle.txt")),StandardCharsets.UTF_8))){
            int i=0;String line;
            while((line=input.readLine())!=null){
                var s=BorealSlopeOracle.SITES[i];var head=s[0]+" "+s[1]+" "+s[2]+" ";
                require(line.startsWith(head),"Window identity "+i);
                String live=BorealSlopeOracle.describe(s[0],(int)s[1],(int)s[2]);
                require(line.equals(head+live),"Published boreal placement window "+i+": "+live);
                i++;
            }
            require(i==BorealSlopeOracle.SITES.length,"Every frozen window is checked");
        }
        System.out.println("BOREAL_SLOPE_PUBLISHED_ORACLE_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private BorealSlopeCheck(){}
}
