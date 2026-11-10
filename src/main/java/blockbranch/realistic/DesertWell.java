package blockbranch.realistic;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** A shaded sandstone cistern, with the vanilla two archaeology deposits. */
public final class DesertWell {
    public record Plan(int x,int y,int z,long seed) {}
    public record Block(BlockPos pos,BlockState state) {}
    private final int definition;
    private final boolean restored;
    public boolean restoredFromSave(){return restored;}
    int[][] planArrays(){return new int[][]{atlasOwners,atlasSelected,atlasPhysical,atlasPlans};}
    private final long seed; private final DrainageNetwork terrain;private final ClimateField climate;
    private final BiomeRegionPlacement regions;
    private final StructureRegionAtlas atlas;
    private final int[] atlasPlans,atlasOwners,atlasSelected,atlasPhysical;
    private final LinkedHashMap<BiomeRegionPlacement.Cell,Optional<Plan>> plans=new LinkedHashMap<>(64,.75f,true);
    public DesertWell(long seed,DrainageNetwork terrain,ClimateField climate){this(seed,terrain,climate,1);}
    public DesertWell(long seed,DrainageNetwork terrain,ClimateField climate,int definition){this(seed,terrain,climate,definition,null);}
    DesertWell(long seed,DrainageNetwork terrain,ClimateField climate,int definition,StructurePlacementCache.Layout saved){
        if(definition<1||definition>3)throw new IllegalArgumentException("Unsupported well definition");this.definition=definition;
        this.seed=seed;this.terrain=terrain;this.climate=climate;
        regions=new BiomeRegionPlacement(seed,(x,z)->SandyDesert.climateSuitable(climate.sample(x,z,terrain::sample))&&SandyDesert.province(seed,x,z)>=.48&&terrain.sample(x,z).waterLevel()==DrainageNetwork.DRY);
        restored=definition==3&&saved!=null;
        if(restored){atlas=saved.atlas();atlasOwners=saved.owners();atlasSelected=saved.selected();atlasPhysical=saved.physical();atlasPlans=saved.codes();return;}
        atlas=definition==3?new StructureRegionAtlas(seed,(x,z)->SandyDesert.province(seed,x,z)>=.48&&climate.structureDesert(x,z,terrain)):null;
        atlasOwners=atlas==null?new int[0]:atlas.substantialOwnerIndices();atlasSelected=new int[atlasOwners.length];java.util.Arrays.fill(atlasSelected,-1);
        var ownerCodes=new int[atlasOwners.length];
        // One budget per complete region. Alternative cells rescue an unfit owner; they do not add budgets.
        // All physical terrain sampling completes here, before the generator admits chunk work.
        if(atlas!=null){var candidates=atlas.candidateIndices(atlasOwners,4);for(int row=0;row<atlasOwners.length;row++)for(int k=0;k<4;k++){
            int index=candidates[row*4+k];if(index<0)break;var cell=atlasCell(index);var result=computePlan(cell,true);if(result.isEmpty())continue;
            var p=result.get();for(int i=0;i<16;i++)if(OakBirchWoodland.mix(regions.hash(cell)+i)==p.seed()){ownerCodes[row]=((p.y()-TerrainShape.FLOOR)<<5)|(i+1);break;}atlasSelected[row]=index;break;
        }}
        // Only selected sites need plan storage, not one slot for every cell in the world.
        atlasPhysical=atlasSelected.clone();Arrays.sort(atlasPhysical);atlasPlans=new int[atlasPhysical.length];
        for(int row=0;row<atlasOwners.length;row++)if(atlasSelected[row]>=0)atlasPlans[Arrays.binarySearch(atlasPhysical,atlasSelected[row])]=ownerCodes[row];
    }
    public StructureRegionAtlas atlas(){return atlas;}
    public BiomeRegionPlacement.Region region(BiomeRegionPlacement.Cell cell){return atlas==null?regions.region(cell):atlas.region(cell);}
    private static int atlasIndex(BiomeRegionPlacement.Cell c){return (c.z()-StructureRegionAtlas.MIN_CELL)*StructureRegionAtlas.SIDE+c.x()-StructureRegionAtlas.MIN_CELL;}
    private static BiomeRegionPlacement.Cell atlasCell(int index){return new BiomeRegionPlacement.Cell(index%StructureRegionAtlas.SIDE+StructureRegionAtlas.MIN_CELL,index/StructureRegionAtlas.SIDE+StructureRegionAtlas.MIN_CELL);}
    public long retainedPlanBytes(){return (long)(atlasPlans.length+atlasOwners.length+atlasSelected.length+atlasPhysical.length)*4;}
    public Optional<Plan> plan(BiomeRegionPlacement.Cell cell){
        if(atlas!=null){
            if(cell.x()< -1024||cell.x()>=1024||cell.z()< -1024||cell.z()>=1024)return Optional.empty();
            // A selected physical cell answers chunk requests; the canonical owner also answers review queries.
            // forChunk checks the physical bounds, so routing an owner to a different cell cannot place twice.
            int index=atlasIndex(cell),physical=Arrays.binarySearch(atlasPhysical,index);
            if(physical<0){int owner=Arrays.binarySearch(atlasOwners,index);if(owner<0||atlasSelected[owner]<0)return Optional.empty();index=atlasSelected[owner];cell=atlasCell(index);physical=Arrays.binarySearch(atlasPhysical,index);}
            int code=atlasPlans[physical];
            long h=OakBirchWoodland.mix(regions.hash(cell)+(code&31)-1);
            return Optional.of(new Plan(cell.blockX()+(int)Math.floorMod(h,768)-384,(code>>>5)+TerrainShape.FLOOR,cell.blockZ()+(int)Math.floorMod(h>>>24,768)-384,h));
        }
        synchronized(this){var cached=plans.get(cell);if(cached!=null)return cached;var result=computePlan(cell);plans.put(cell,result);while(plans.size()>64)plans.remove(plans.keySet().iterator().next());return result;}
    }
    private Optional<Plan> computePlan(BiomeRegionPlacement.Cell cell){return computePlan(cell,false);}
    private Optional<Plan> computePlan(BiomeRegionPlacement.Cell cell,boolean alternative){
        var region=region(cell);Optional<Plan> result=Optional.empty();
        if((definition==1||region.cells()>=4)&&(alternative||cell.equals(region.owner())))for(int i=0;i<16;i++){
            long h=OakBirchWoodland.mix(regions.hash(cell)+i);
            int x=cell.blockX()+(int)Math.floorMod(h,768)-384,z=cell.blockZ()+(int)Math.floorMod(h>>>24,768)-384;
            int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean valid=true;
            if(definition==3){
                // Reject rough/wet attempts cheaply before climate work. Acceptance still checks
                // every support block below; these four points cannot change the final decision.
                for(int dz=-6;dz<=6;dz+=12)for(int dx=-6;dx<=6;dx+=12){
                    var corner=terrain.sample(x+dx,z+dz);min=Math.min(min,corner.height());max=Math.max(max,corner.height());
                    if(corner.waterLevel()!=DrainageNetwork.DRY||max-min>1)valid=false;
                }
                if(!valid)continue;
            }
            // contains() is constant within a 4x4 biome sample. Cache only within this attempt;
            // physical height/water checks still cover every one of the 169 support blocks.
            int quartX=Math.floorDiv(x-6,4),quartZ=Math.floorDiv(z-6,4),known=0,suitable=0;
            for(int dz=-6;dz<=6&&valid;dz++)for(int dx=-6;dx<=6;dx++){
                var t=terrain.sample(x+dx,z+dz);min=Math.min(min,t.height());max=Math.max(max,t.height());
                if(t.waterLevel()!=DrainageNetwork.DRY||max-min>1){valid=false;break;}
                if(definition==3){
                    int bit=1<<((Math.floorDiv(z+dz,4)-quartZ)*5+Math.floorDiv(x+dx,4)-quartX);
                    if((known&bit)==0){known|=bit;if(SandyDesert.contains(seed,x+dx,z+dz,terrain,climate))suitable|=bit;}
                    if((suitable&bit)==0){valid=false;break;}
                }else if(!SandyDesert.contains(seed,x+dx,z+dz,terrain,climate)){valid=false;break;}
            }
            if(valid){result=Optional.of(new Plan(x,max,z,h));break;}
        }
        return result;
    }
    public Optional<Plan> forChunk(ChunkPos chunk){
        var cell=new BiomeRegionPlacement.Cell(Math.floorDiv(chunk.getMinBlockX(),BiomeRegionPlacement.GRID),Math.floorDiv(chunk.getMinBlockZ(),BiomeRegionPlacement.GRID));
        // All attempts lie inside the central 768m square. Most chunks need no habitat search.
        if(Math.abs(chunk.getMinBlockX()+8-cell.blockX())>400||Math.abs(chunk.getMinBlockZ()+8-cell.blockZ())>400)return Optional.empty();
        return plan(cell).filter(p->p.x+6>=chunk.getMinBlockX()&&p.x-6<=chunk.getMaxBlockX()&&p.z+6>=chunk.getMinBlockZ()&&p.z-6<=chunk.getMaxBlockZ());
    }
    public static List<Block> blocks(Plan p){
        var out=new ArrayList<Block>();
        for(int z=-5;z<=5;z++)for(int x=-5;x<=5;x++)for(int y=-3;y<=6;y++){
            int r=Math.max(Math.abs(x),Math.abs(z));BlockState b=null;
            if(r<=4&&y<=0)b=(y==0?Blocks.CUT_SANDSTONE:Blocks.SANDSTONE).defaultBlockState();
            if(r<=1&&y==-1)b=Blocks.SAND.defaultBlockState();
            if(r<=1&&y==0)b=Blocks.WATER.defaultBlockState();
            if(r<=4&&y>=1&&y<=4)b=Blocks.AIR.defaultBlockState();
            if(Math.abs(x)==3&&Math.abs(z)==3&&y>=1&&y<=4)b=(y==1||y==4?Blocks.CHISELED_SANDSTONE:Blocks.SMOOTH_SANDSTONE).defaultBlockState();
            if(r<=4&&y==5)b=Blocks.SANDSTONE_SLAB.defaultBlockState();
            if(r<=2&&y==5)b=Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            if(r<=1&&y==6)b=Blocks.SANDSTONE_SLAB.defaultBlockState();
            if(r==5&&y==0&&(Math.abs(x)<=1||Math.abs(z)<=1))b=Blocks.SANDSTONE_SLAB.defaultBlockState();
            if(y==-1&&z==0&&(x==-1||x==1))b=Blocks.SUSPICIOUS_SAND.defaultBlockState();
            if(b!=null)out.add(new Block(new BlockPos(p.x+x,p.y+y,p.z+z),b));
        }
        return List.copyOf(out);
    }
    public void place(WorldGenLevel level,ChunkPos chunk){forChunk(chunk).ifPresent(p->{for(var b:blocks(p)){
        if(!new ChunkPos(b.pos.getX()>>4,b.pos.getZ()>>4).equals(chunk))continue;
        level.setBlock(b.pos,b.state,2);
        if(b.state.is(Blocks.SUSPICIOUS_SAND)&&level.getBlockEntity(b.pos) instanceof BrushableBlockEntity entity)
            entity.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY,p.seed^b.pos.asLong());
    }});}
}
