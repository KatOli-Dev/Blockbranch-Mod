package blockbranch.realistic;

/** Bounded exact memo of immutable per-network vegetation exclusions. No world/chunk state. */
final class SurfaceReservationCache {
    @FunctionalInterface interface Reader { boolean reserved(int x,int z); }
    private final long[] keys=new long[4096];
    // zero=empty, one=false, two=true; zero coordinates and false are real cache entries.
    private final byte[] values=new byte[keys.length];
    private final Reader reader;
    SurfaceReservationCache(Reader reader){this.reader=reader;}
    boolean reserved(int x,int z){
        long key=(x&0xffffffffL)|((long)z<<32);
        long mixed=(key^(key>>>30))*0xBF58476D1CE4E5B9L;
        mixed=(mixed^(mixed>>>27))*0x94D049BB133111EBL;
        int slot=(int)(mixed^(mixed>>>31))&(keys.length-1);
        synchronized(values){if(values[slot]!=0&&keys[slot]==key)return values[slot]==2;}
        // No lock is held while sampling terrain. Duplicate concurrent misses are harmless.
        boolean value=reader.reserved(x,z);
        synchronized(values){keys[slot]=key;values[slot]=(byte)(value?2:1);}
        return value;
    }
}
