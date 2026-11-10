package blockbranch.cubic;
public interface TerrainSnapshotAccess {
    TerrainProfile blockbranch$base();
    int[] blockbranch$edited();
    void blockbranch$base(TerrainProfile base,int[] edited);
}
