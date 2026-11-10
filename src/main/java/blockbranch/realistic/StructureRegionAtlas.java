package blockbranch.realistic;

import java.util.Arrays;
import java.util.function.Consumer;

/** Definition 3: complete finite-world connectivity, prepared before chunk generation.
 * Construction uses bounded dense scratch; immutable runs retain the exact same labels. */
public final class StructureRegionAtlas {
    @FunctionalInterface public interface Mask {boolean test(int x,int z);}
    @FunctionalInterface public interface LabelMask {int sample(int x,int z);}
    public static StructureRegionAtlas labeled(long seed,LabelMask mask){return new StructureRegionAtlas(seed,SIDE,MIN_CELL,mask);}
    public static final int SIDE=2048, MIN_CELL=-1024, GRID=2048;
    private static final int LABEL_BAND_ROWS=32;
    private final int side,min;
    private final long seed;
    // Sorted canonical indices and their component sizes; sorted inclusive run ends and labels.
    private final int[] ownerKeys,sizes,runEnds,runOwners;

    public StructureRegionAtlas(long seed,Mask mask){this(seed,SIDE,MIN_CELL,mask);}
    StructureRegionAtlas(long seed,int side,int min,Mask mask){this(seed,side,min,(LabelMask)(x,z)->mask.test(x,z)?1:0);}
    private StructureRegionAtlas(long seed,int side,int min,LabelMask mask){
        if(side<1||side>SIDE)throw new IllegalArgumentException("Atlas extent");
        this.side=side;this.min=min;this.seed=seed;
        int count=Math.multiplyExact(side,side);
        var parent=new int[count];var rank=new byte[count];
        var owners=new int[count];var counts=new int[count];
        Arrays.fill(parent,-1);
        var rowLabels=new int[side];
        // Mask evaluation dominates construction and is pure per cell, so each band of rows is
        // labelled in parallel into a small buffer; the order-dependent union pass stays serial.
        var band=new int[Math.min(side,LABEL_BAND_ROWS)*side];
        for(int z0=0;z0<side;z0+=LABEL_BAND_ROWS){
            int rows=Math.min(LABEL_BAND_ROWS,side-z0),base=z0;
            java.util.stream.IntStream.range(0,rows).parallel().forEach(r->{
                int wz=(base+r+min)*GRID+GRID/2;
                for(int x=0;x<side;x++)band[r*side+x]=mask.sample((x+min)*GRID+GRID/2,wz);
            });
            for(int r=0;r<rows;r++)for(int x=0;x<side;x++){
                int z=z0+r,i=z*side+x;
                int label=band[r*side+x],above=rowLabels[x];rowLabels[x]=label;
                if(label<=0)continue;
                parent[i]=i;owners[i]=i+1;
                if(x>0&&parent[i-1]>=0&&rowLabels[x-1]==label)union(parent,rank,owners,i,i-1);
                if(z>0&&parent[i-side]>=0&&above==label)union(parent,rank,owners,i,i-side);
            }
        }
        for(int i=0;i<count;i++)if(parent[i]>=0){int root=find(parent,i);counts[owners[root]-1]++;}
        // Compress every union path before replacing labels. Root owner entries stay unchanged.
        for(int i=0;i<count;i++)if(parent[i]>=0)parent[i]=find(parent,i);
        for(int i=0;i<count;i++)owners[i]=parent[i]<0?0:owners[parent[i]];

        int regions=0,runs=1;
        for(int value:counts)if(value>0)regions++;
        for(int i=1;i<count;i++)if(owners[i]!=owners[i-1])runs++;
        ownerKeys=new int[regions];sizes=new int[regions];
        for(int i=0,j=0;i<count;i++)if(counts[i]>0){ownerKeys[j]=i;sizes[j++]=counts[i];}
        runEnds=new int[runs];runOwners=new int[runs];
        int run=0;
        for(int i=1;i<=count;i++)if(i==count||owners[i]!=owners[i-1]){
            runEnds[run]=i-1;runOwners[run++]=owners[i-1];
        }
        // No dense scratch, mask callback, terrain or world reference escapes construction.
    }
    int[][] snapshotArrays(){return new int[][]{runEnds,runOwners,ownerKeys,sizes};}
    static StructureRegionAtlas restore(long seed,int[] ends,int[] labels,int[] keys,int[] sizes){return new StructureRegionAtlas(seed,ends,labels,keys,sizes);}
    private StructureRegionAtlas(long seed,int[] ends,int[] labels,int[] keys,int[] counts){
        this.seed=seed;side=SIDE;min=MIN_CELL;runEnds=ends;runOwners=labels;ownerKeys=keys;sizes=counts;
        int count=SIDE*SIDE;
        if(ends.length<1||ends.length!=labels.length||keys.length!=counts.length||ends[ends.length-1]!=count-1)throw new IllegalArgumentException("Atlas lengths");
        for(int i=0;i<keys.length;i++)if(keys[i]<0||keys[i]>=count||(i>0&&keys[i]<=keys[i-1])||counts[i]<1||counts[i]>count)throw new IllegalArgumentException("Atlas owners");
        int[] actual=new int[keys.length];
        for(int i=0,start=0;i<ends.length;start=ends[i++]+1){
            if(ends[i]<start||ends[i]>=count)throw new IllegalArgumentException("Atlas runs");
            if(labels[i]!=0){int row=Arrays.binarySearch(keys,labels[i]-1);if(row<0)throw new IllegalArgumentException("Atlas label");actual[row]+=ends[i]-start+1;}
        }
        if(!Arrays.equals(actual,counts))throw new IllegalArgumentException("Atlas counts");
        for(int key:keys){var c=new BiomeRegionPlacement.Cell(key%SIDE+MIN_CELL,key/SIDE+MIN_CELL);if(!c.equals(region(c).owner()))throw new IllegalArgumentException("Atlas owner membership");}
    }
    private static int find(int[] p,int i){
        int root=i;while(p[root]!=root)root=p[root];
        while(p[i]!=i){int next=p[i];p[i]=root;i=next;}
        return root;
    }
    private void union(int[] p,byte[] rank,int[] owners,int a,int b){
        a=find(p,a);b=find(p,b);if(a==b)return;
        if(rank[a]<rank[b]){int t=a;a=b;b=t;}
        p[b]=a;if(rank[a]==rank[b])rank[a]++;
        int first=owners[a]-1,second=owners[b]-1;
        if(compare(second,first)<0)owners[a]=second+1;
    }
    private int compare(int a,int b){int c=Long.compareUnsigned(hash(a),hash(b));return c==0?Integer.compare(a,b):c;}
    private long hash(int i){return OakBirchWoodland.mix(seed^0x57454c4c524547L^(i%side+min)*0x9e3779b97f4a7c15L^(i/side+min)*0xc2b2ae3d27d4eb4fL);}
    public BiomeRegionPlacement.Region region(BiomeRegionPlacement.Cell cell){
        int x=cell.x()-min,z=cell.z()-min;
        if(x<0||z<0||x>=side||z>=side)return new BiomeRegionPlacement.Region(null,0,false);
        int run=Arrays.binarySearch(runEnds,z*side+x);if(run<0)run=-run-1;
        int owner=runOwners[run]-1;
        if(owner<0)return new BiomeRegionPlacement.Region(null,0,false);
        return new BiomeRegionPlacement.Region(new BiomeRegionPlacement.Cell(owner%side+min,owner/side+min),sizes[Arrays.binarySearch(ownerKeys,owner)],false);
    }
    public record Statistics(int regions,int substantial,int oversized,int largest){}
    public Statistics statistics(){
        int s=0,b=0,m=0;for(int size:sizes){if(size>=4)s++;if(size>1024)b++;m=Math.max(m,size);}
        return new Statistics(sizes.length,s,b,m);
    }
    public void forEachSubstantialOwner(Consumer<BiomeRegionPlacement.Cell> visitor){
        for(int i=0;i<sizes.length;i++)if(sizes[i]>=4)visitor.accept(new BiomeRegionPlacement.Cell(ownerKeys[i]%side+min,ownerKeys[i]/side+min));
    }
    public int[] substantialOwnerIndices(){
        int count=0;for(int size:sizes)if(size>=4)count++;
        int[] result=new int[count];int n=0;
        for(int i=0;i<sizes.length;i++)if(sizes[i]>=4)result[n++]=ownerKeys[i];
        return result;
    }
    /** Same globally ranked candidates as the dense atlas; runs only skip empty cells. */
    public int[] candidateIndices(int[] keys,int limit){
        int[] result=new int[Math.multiplyExact(keys.length,limit)];Arrays.fill(result,-1);
        for(int run=0,start=0;run<runEnds.length;start=runEnds[run++]+1){
            int row=Arrays.binarySearch(keys,runOwners[run]-1);if(row<0)continue;
            row*=limit;
            for(int i=start;i<=runEnds[run];i++)for(int k=0;k<limit;k++){
                if(result[row+k]<0||compare(i,result[row+k])<0){
                    System.arraycopy(result,row+k,result,row+k+1,limit-k-1);result[row+k]=i;break;
                }
            }
        }
        return result;
    }
    public long retainedArrayBytes(){return (long)(ownerKeys.length+sizes.length+runEnds.length+runOwners.length)*Integer.BYTES;}
}
