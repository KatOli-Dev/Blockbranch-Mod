package blockbranch.realistic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

/** Optional derived generator data. Invalid or stale data is a cache miss, never a world error. */
public final class StructurePlacementCache {
    private static final int FORMAT=1, MAX_INTS=262144;
    public static final Codec<StructurePlacementCache> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.LONG.fieldOf("seed").forGetter(c->c.seed),
        Codec.BOOL.fieldOf("sandy_coasts").forGetter(c->c.sandyCoasts),
        Codec.STRING.fieldOf("checksum").forGetter(c->c.checksum),
        Codec.INT_STREAM.xmap(s->s.limit(MAX_INTS+1L).toArray(),Arrays::stream).fieldOf("data").forGetter(c->c.data)
    ).apply(i,StructurePlacementCache::new));
    private final long seed;
    private final boolean sandyCoasts;
    private final String checksum;
    private final int[] data;
    private StructurePlacementCache(long seed,boolean sandy,String checksum,int[] data){this.seed=seed;this.sandyCoasts=sandy;this.checksum=checksum;this.data=data.clone();}
    record Layout(StructureRegionAtlas atlas,int[] owners,int[] selected,int[] physical,int[] codes){}
    public int retainedBytes(){return data.length*Integer.BYTES;}

    static Optional<StructurePlacementCache> capture(long seed,boolean sandy,DesertWell wells){
        int[][] a=wells.atlas().snapshotArrays(),p=wells.planArrays();
        int length=4;for(int[] v:a)length+=v.length;for(int[] v:p)length+=v.length;
        if(length>MAX_INTS)return Optional.empty(); // Bound save metadata; generation is unchanged.
        var data=new int[length];data[0]=FORMAT;data[1]=a[0].length;data[2]=a[2].length;data[3]=p[0].length;
        int offset=4;for(int[][] group:new int[][][]{a,p})for(int[] v:group){System.arraycopy(v,0,data,offset,v.length);offset+=v.length;}
        return Optional.of(new StructurePlacementCache(seed,sandy,checksum(seed,sandy,data),data));
    }
    Layout restore(long expectedSeed,boolean expectedSandy){
        try{
            if(seed!=expectedSeed||sandyCoasts!=expectedSandy||data.length<4||data.length>MAX_INTS||data[0]!=FORMAT||!checksum.equals(checksum(seed,sandyCoasts,data)))return null;
            int runs=data[1],regions=data[2],plans=data[3];
            if(runs<1||regions<0||plans<0||4L+2L*runs+2L*regions+4L*plans!=data.length)return null;
            int[][] arrays=new int[8][];int offset=4;
            for(int i=0;i<8;i++){int length=i<2?runs:i<4?regions:plans;arrays[i]=Arrays.copyOfRange(data,offset,offset+length);offset+=length;}
            var atlas=StructureRegionAtlas.restore(seed,arrays[0],arrays[1],arrays[2],arrays[3]);
            int[] owners=arrays[4],selected=arrays[5],physical=arrays[6],codes=arrays[7];
            if(!Arrays.equals(owners,atlas.substantialOwnerIndices()))return null;
            var sorted=selected.clone();Arrays.sort(sorted);if(!Arrays.equals(sorted,physical))return null;
            for(int row=0;row<owners.length;row++){
                int index=selected[row];if(index== -1)continue;
                if(index<0||index>=StructureRegionAtlas.SIDE*StructureRegionAtlas.SIDE)return null;
                var cell=cell(index);if(!cell(owners[row]).equals(atlas.region(cell).owner()))return null;
                int at=Arrays.binarySearch(physical,index),code=codes[at],attempt=code&31,height=(code>>>5)+TerrainShape.FLOOR;
                if(attempt<1||attempt>16||height<TerrainShape.FLOOR+3||height>TerrainShape.CEILING-7)return null;
            }
            for(int i=0;i<physical.length;i++)if(physical[i]<0&&codes[i]!=0)return null;
            return new Layout(atlas,owners,selected,physical,codes);
        }catch(IllegalArgumentException|IndexOutOfBoundsException invalid){return null;}
    }
    private static BiomeRegionPlacement.Cell cell(int i){return new BiomeRegionPlacement.Cell(i%StructureRegionAtlas.SIDE+StructureRegionAtlas.MIN_CELL,i/StructureRegionAtlas.SIDE+StructureRegionAtlas.MIN_CELL);}
    private static String checksum(long seed,boolean sandy,int[] data){
        try{var bytes=ByteBuffer.allocate(9+data.length*4).putLong(seed).put((byte)(sandy?1:0));for(int value:data)bytes.putInt(value);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.array()));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
