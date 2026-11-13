package blockbranch.realistic;

import java.util.*;
import java.util.function.Function;
import blockbranch.Blockbranch;
import blockbranch.cubic.CanopyLayer;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;

/** Removes whole intersected immutable trees before construction, independently of chunk order. */
public final class ReimaginedTreeClearPiece extends StructurePiece {
 public static final StructurePieceType TYPE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Blockbranch.id("reimagined_tree_clear_v1"),(StructurePieceType)ReimaginedTreeClearPiece::new);
 public static void initialize(){}
 private static final net.minecraft.world.level.block.state.BlockState[] STATES=states();
 private static net.minecraft.world.level.block.state.BlockState[] states(){var out=new net.minecraft.world.level.block.state.BlockState[CanopyLayer.LAST_CODE+1];out[0]=Blocks.AIR.defaultBlockState();for(int i=1;i<out.length;i++)out[i]=new CanopyLayer(new int[]{(1<<16)|i}).state(0,TerrainShape.FLOOR+1,0);return out;}
 // Saved plans retain both root selection and crown footprints across partial
 // generation: 1/2 historical roots, 3/4 transitions, 5..8 redwoods,
 // 9..16 independently add dry forest; 17..32 add summer-dry woodland;
 // 33..64 add saved polar climate; 65..128 add seasonal forest;129..256 add montane needleleaf roots;
 // 257..512 add boreal slope tolerance; 513..1024 add broadleaf oak crowns; 1025..2048 add seasonal dry-forest crowns.
 // Odd/even selects legacy/whorled crowns; every bit retains its own meaning.
 private final BoundingBox construction;private final long seed;private final int clearDefinition;private volatile Map<Long,Integer> replacements;
 public ReimaginedTreeClearPiece(BoundingBox construction,long seed,int crownDefinition){this(construction,seed,crownDefinition,0);}
 public ReimaginedTreeClearPiece(BoundingBox construction,long seed,int crownDefinition,int transitions){super(TYPE,0,expanded(construction,crownDefinition+1+2*transitions));if(transitions<0||transitions>1023)throw new IllegalArgumentException("Unknown vegetation definition");if(crownDefinition!=0&&crownDefinition!=1)throw new IllegalArgumentException("Unknown boreal crown definition");this.construction=construction;this.seed=seed;this.clearDefinition=crownDefinition+1+2*transitions;}
 private ReimaginedTreeClearPiece(StructurePieceSerializationContext c,CompoundTag t){super(TYPE,t);int[] a=t.getIntArray("Construction").orElseThrow();clearDefinition=t.getIntOr("ClearDefinition",0);if((clearDefinition<1||clearDefinition>2048)||a.length!=6)throw new IllegalArgumentException("Unknown tree-clear geometry");construction=new BoundingBox(a[0],a[1],a[2],a[3],a[4],a[5]);seed=t.getLongOr("Seed",0);if(construction.getXSpan()>240||construction.getZSpan()>240||!getBoundingBox().equals(expanded(construction,clearDefinition)))throw new IllegalArgumentException("Invalid tree-clear bounds");}
 // Only redwood-bearing plans reserve the taller halo; definitions 1..8 keep exact boxes.
 private static BoundingBox expanded(BoundingBox b,int definition){return new BoundingBox(b.minX()-32,b.minY()-32,b.minZ()-32,b.maxX()+32,b.maxY()+((((definition-1)/2)&RedwoodForest.FLAG)!=0?144:80),b.maxZ()+32);}
 @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("ClearDefinition",clearDefinition);t.putIntArray("Construction",new int[]{construction.minX(),construction.minY(),construction.minZ(),construction.maxX(),construction.maxY(),construction.maxZ()});t.putLong("Seed",seed);}
 private synchronized Map<Long,Integer> replacements(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){
  if(replacements!=null)return replacements;
  int flags=(clearDefinition-1)/2;boolean seasonal=(flags&SeasonalForestExpansion.FLAG)!=0;
  if(seasonal&&g.seasonalForestDefinition()!=1)throw new IllegalStateException("Saved seasonal-forest plan requires its generation definition");
  var terrain=g.hutPlanningTerrain(random);var d=terrain.drainage();var climate=new ClimateField(seed,(flags&16)!=0);int x=construction.minX()-24,z=construction.minZ()-24,mx=construction.maxX()+24,mz=construction.maxZ()+24;
  boolean montane=(flags&MontaneForest.FLAG)!=0;
  if((flags&BorealSlope.FLAG)!=0&&g.borealSlopeDefinition()!=1)throw new IllegalStateException("Saved boreal-slope plan requires its generation definition");
  if(SeasonalCrowns.enabled(flags)&&g.seasonalCrownDefinition()!=1)throw new IllegalStateException("Saved seasonal-crown plan requires its generation definition");
  if(BroadleafCrowns.enabled(flags)&&g.broadleafCrownDefinition()!=1)throw new IllegalStateException("Saved broadleaf-crown plan requires its generation definition");
  if(montane&&g.montaneForestDefinition()!=1)throw new IllegalStateException("Saved montane plan requires its generation definition");
  var montaneOwner=montane?g.montaneForestOwner(random):null;
  var trees=new ArrayList<List<WoodlandTrees.Voxel>>();
  var temperate=new ArrayList<>(WoodlandTrees.roots(seed,x,z,mx,mz,d,climate,(clearDefinition-1)/2));var ordinary=WoodlandTrees.ordinary(temperate);temperate.removeIf(r->ColdUplands.surfaceKind(seed,r.x(),r.z(),d,climate)>0);temperate.addAll(ColdUplands.roots(seed,x,z,mx,mz,d,climate));temperate.removeIf(r->Badlands.surfaceKind(seed,r.x(),r.z(),d,climate)>0);temperate.addAll(Badlands.roots(seed,x,z,mx,mz,d,climate));if(montane)temperate.removeIf(r->montaneOwner.test(r.x(),r.z()));add(trees,temperate,r->r.family()==15?TaigaTrees.tree(r):WoodlandTrees.tree(r,WoodlandTrees.flagsFor(ordinary,r,flags)));
  if(montane)add(trees,MontaneForestTrees.roots(seed,x,z,mx,mz,d,climate,montaneOwner),TaigaTrees::tree);
  if(RedwoodForest.enabled((clearDefinition-1)/2))add(trees,RedwoodTrees.roots(seed,x,z,mx,mz,d,climate),RedwoodTrees::tree);
  var boreal=BorealTrees.roots(seed,x,z,mx,mz,d,climate,BorealSlope.relief(flags));boreal.removeIf(r->ColdUplands.surfaceKind(seed,r.x(),r.z(),d,climate)>0);add(trees,boreal,(clearDefinition&1)==1?BorealTrees::legacyTree:BorealTrees::tree);
  var dryForest=(flags&DryForest.FLAG)!=0||seasonal?g.dryForestOwner(random,seasonal):null;
  if(((flags&DryForest.FLAG)!=0||seasonal)&&dryForest==null)throw new IllegalStateException("Saved dry-forest plan requires matching generation definition");
  var summerDry=(((clearDefinition-1)/2)&SummerDryWoodland.FLAG)!=0?g.summerDryOwner(random):null;
  if((((clearDefinition-1)/2)&SummerDryWoodland.FLAG)!=0&&summerDry==null)throw new IllegalStateException("Saved summer-dry plan requires matching generation definition");
  var warm=WarmTrees.roots(seed,x,z,mx,mz,d,climate);warm.removeIf(r->Badlands.surfaceKind(seed,r.x(),r.z(),d,climate)>0||dryForest!=null&&dryForest.test(r.x(),r.z())||summerDry!=null&&summerDry.test(r.x(),r.z()));add(trees,warm,WarmTrees::tree);
  if(dryForest!=null)add(trees,DryForestTrees.roots(seed,x,z,mx,mz,d,climate,dryForest),r->ForestTrees.tree(r,false,SeasonalCrowns.enabled(flags)));
  if(summerDry!=null)add(trees,SummerDryTrees.roots(seed,x,z,mx,mz,d,climate,summerDry),SummerDryTrees::tree);
  add(trees,HumidTrees.roots(seed,x,z,mx,mz,d,climate),HumidTrees::tree);add(trees,SavannaTrees.roots(seed,x,z,mx,mz,d,climate),SavannaTrees::tree);add(trees,SavannaUplands.roots(seed,x,z,mx,mz,d,climate),SavannaUplands::tree);var jungle=JungleTrees.roots(seed,x,z,mx,mz,d,climate);jungle.addAll(TropicalVariantTrees.roots(seed,x,z,mx,mz,d,climate));if(seasonal)jungle.removeIf(r->dryForest.test(r.x(),r.z()));add(trees,jungle,JungleTrees::tree);add(trees,WindsweptTrees.roots(seed,x,z,mx,mz,d,climate),WindsweptTrees::tree);add(trees,SwampTrees.roots(seed,x,z,mx,mz,d,climate),SwampTrees::tree);
  var removed=new HashSet<Long>();var retained=new Long2IntOpenHashMap();
  for(var tree:trees){boolean cut=tree.stream().anyMatch(v->WoodlandTrees.logCode(v.code())&&construction.isInside(v.x(),v.y(),v.z()));for(var v:tree){long key=BlockPos.asLong(v.x(),v.y(),v.z());if(cut){if(!getBoundingBox().isInside(v.x(),v.y(),v.z()))throw new IllegalStateException("Tree exceeds bounded clear halo");removed.add(key);}else if(!retained.containsKey(key)||WoodlandTrees.logCode(v.code())&&!WoodlandTrees.logCode(retained.get(key)))retained.put(key,v.code());}}
  var result=new HashMap<Long,Integer>();for(long key:removed)result.put(key,retained.get(key));
  if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Tree clearing interrupted before publication");
  return replacements=Map.copyOf(result);
 }
 private static <T> void add(List<List<WoodlandTrees.Voxel>> target,List<T> roots,Function<T,List<WoodlandTrees.Voxel>> shape){for(var root:roots)target.add(shape.apply(root));}
 // The saved immutable footprint decides which trees intersect construction, not live
 // block provenance. Replay retains non-log/leaf edits, but player-placed logs/leaves
 // inside that footprint can also be replaced; this is not an edit-preservation sweep.
 @Override public void postProcess(WorldGenLevel world,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){if(!(generator instanceof RealisticGenerator g))return;var plan=replacements(g,world.getLevel().getChunkSource().randomState());for(var e:plan.entrySet()){var at=BlockPos.of(e.getKey());if(!clip.isInside(at))continue;var state=world.getBlockState(at);if(!state.is(BlockTags.LOGS)&&!state.is(BlockTags.LEAVES))continue;int code=e.getValue();var replacement=STATES[code];world.setBlock(at,replacement,2);}}
}
