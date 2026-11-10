package blockbranch.cubic;

import blockbranch.realistic.TerrainShape;
import blockbranch.realistic.UndergroundStructure;
import blockbranch.realistic.CaveApproach;
import blockbranch.realistic.CaveEntryRoute;
import net.minecraft.nbt.CompoundTag;

/** Small immutable cave descriptor shared by disk profiles and client snapshots. */
public final class UndergroundLayer {
    public static final UndergroundLayer NONE = new UndergroundLayer();
    private final UndergroundStructure field;
    private final long seed;
    private final int columnX, columnZ;
    private final CaveApproach approach;
    private UndergroundLayer() { field=null;seed=0;columnX=columnZ=0;approach=CaveApproach.NONE; }
    public UndergroundLayer(long seed,int columnX,int columnZ) {
        this(seed,columnX,columnZ,CaveApproach.NONE);
    }
    public UndergroundLayer(long seed,int columnX,int columnZ,CaveApproach approach) {
        this.seed=seed;this.columnX=columnX;this.columnZ=columnZ;
        if(columnX < (Integer.MIN_VALUE>>4) || columnX > (Integer.MAX_VALUE>>4)
            || columnZ < (Integer.MIN_VALUE>>4) || columnZ > (Integer.MAX_VALUE>>4))
            throw new IllegalArgumentException("Invalid underground column coordinates");
        field=new UndergroundStructure(seed);
        this.approach=approach.intersectsColumn(columnX,columnZ)?approach:CaveApproach.NONE;
    }
    public boolean enabled(){return field!=null;}
    public long seed(){return seed;}
    public boolean owns(int x,int z){return !enabled() || x==columnX&&z==columnZ;}
    public boolean voidAt(int x,int y,int z,int surface) {
        return enabled()&&field.voidAt((columnX<<4)+(x&15),y,(columnZ<<4)+(z&15),surface);
    }
    public boolean hasApproach(){return approach.present();}
    public boolean approachColumn(int x,int z){return approach.affectsColumn((columnX<<4)+(x&15),(columnZ<<4)+(z&15));}
    public boolean approachAt(int x,int y,int z){return approach.present()&&approach.contains((columnX<<4)+(x&15),y,(columnZ<<4)+(z&15));}
    public boolean approachClears(int x,int y,int z,int ground){return approach.present()&&approach.clears((columnX<<4)+(x&15),y,(columnZ<<4)+(z&15),ground);}
    public static boolean approachMaterial(net.minecraft.world.level.block.state.BlockState value){
        return value.getFluidState().isEmpty()&&!value.is(net.minecraft.world.level.block.Blocks.BEDROCK)
            &&!value.is(net.minecraft.tags.BlockTags.LOGS)&&!value.is(net.minecraft.tags.BlockTags.LEAVES);
    }
    public boolean mayAffect(int sectionY,int highestGround) {
        return approach.present()&&((long)sectionY<<4)<=approach.maxY()&&((long)sectionY<<4)+15>=approach.minY()
            ||enabled() && ((long)sectionY<<4)<highestGround-UndergroundStructure.ROOF_THICKNESS
            && ((long)sectionY<<4)+15>TerrainShape.FLOOR+UndergroundStructure.FLOOR_THICKNESS;
    }
    public CompoundTag encode(){
        if(!enabled())throw new IllegalStateException("No underground descriptor to encode");
        var tag=new CompoundTag();tag.putInt("version",approach.entry().present()?3:approach.present()?2:UndergroundStructure.VERSION);tag.putLong("seed",seed);
        if(approach.present())tag.putIntArray("approach",approach.encode());
        if(approach.entry().present())tag.putIntArray("entry_route",approach.entry().encode());
        tag.putInt("column_x",columnX);tag.putInt("column_z",columnZ);return tag;
    }
    public static UndergroundLayer decode(CompoundTag tag){
        int version=tag.getIntOr("version",-1);
        if(version!=1&&version!=2&&version!=3)throw new IllegalArgumentException("Unsupported underground definition");
        if(version<3&&tag.contains("entry_route"))throw new IllegalArgumentException("Entry route on older definition");
        if(version==1&&tag.contains("approach"))throw new IllegalArgumentException("Approach on frozen definition");
        var layer=new UndergroundLayer(tag.getLong("seed").orElseThrow(()->new IllegalArgumentException("Missing underground seed")),
            tag.getInt("column_x").orElseThrow(()->new IllegalArgumentException("Missing underground X")),
            tag.getInt("column_z").orElseThrow(()->new IllegalArgumentException("Missing underground Z")),
            version>=2?new CaveApproach(tag.getIntArray("approach").orElseThrow(()->new IllegalArgumentException("Missing approach")),version==3?new CaveEntryRoute(tag.getIntArray("entry_route").orElseThrow(()->new IllegalArgumentException("Missing entry route"))):CaveEntryRoute.NONE):CaveApproach.NONE);
        if(version==3&&!layer.approach.entry().present())throw new IllegalArgumentException("Missing entry continuation");
        if(version>=2&&!layer.hasApproach())throw new IllegalArgumentException("Approach does not affect descriptor column");
        return layer;
    }
}
