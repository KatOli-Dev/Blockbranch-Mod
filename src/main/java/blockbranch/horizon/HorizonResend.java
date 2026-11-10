package blockbranch.horizon;
import java.util.Arrays;
/**
 * When the client sends its missing-tile list. A changed list is sent at once. An unchanged,
 * non-empty list is sent again after {@link #FIRST} ticks, then at doubling intervals up to
 * {@link #MAX}, because the server drops tiles that failed or belonged to an older request
 * generation and only a new request brings them back. Client thread only.
 */
public final class HorizonResend {
    public static final int FIRST=100,MAX=1200;
    private long[] last=new long[0];
    private int sentAt,delay=FIRST;
    /** The list to send at tick {@code now}, or null to send nothing. */
    public long[] next(long[] missing,int now) {
        if(!Arrays.equals(missing,last)){last=missing;sentAt=now;delay=FIRST;return missing;}
        if(missing.length==0||now-sentAt<delay)return null;
        sentAt=now;delay=Math.min(MAX,delay*2);return missing;
    }
    /** Last list sent. */
    public long[] last(){return last;}
    public void reset(){last=new long[0];delay=FIRST;}
}
