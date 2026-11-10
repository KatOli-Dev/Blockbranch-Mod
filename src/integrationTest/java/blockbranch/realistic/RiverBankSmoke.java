package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

/** Explicit dry/submerged sediment domains; fixed lowland scene, no substituted native gorge witness. */
public final class RiverBankSmoke {
 public static void verify(ServerLevel l,RealisticGenerator g,boolean reload){var r=l.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var scene=RiverSmoke.scene(g,r);BlockPos[] witness=new BlockPos[2];int count=0;
  for(int z=scene.getZ()-96;z<=scene.getZ()+96;z+=2)for(int x=scene.getX()-96;x<=scene.getX()+96;x+=2){int code=RoutedRiver.bankCover(r.seed(),x,z,d,c);if(code<13)continue;count++;var s=d.sample(x,z);var p=new BlockPos(x,s.height(),z);if(witness[code-13]==null)witness[code-13]=p;}
  RiverSmoke.require(count>100&&witness[0]!=null&&witness[1]!=null,"Nontrivial mud and sand banks at fixed lowland scene");
  for(int k=0;k<2;k++){var p=witness[k];var chunk=l.getChunkAt(p);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();int index=(p.getZ()&15)*16+(p.getX()&15);var tag=base.encode(l.palettedContainerFactory());var copy=TerrainProfile.decode(tag,l.palettedContainerFactory());
   if(reload)RiverSmoke.require(l.getBlockState(p).is(Blocks.CLAY)&&l.getBlockState(p.below(8)).is(Blocks.GOLD_BLOCK),"Dry bank edits survive disk reopen");l.setBlock(p,base.state(p.getX(),p.getY(),p.getZ()),3);
   var resolver=g.getBiomeSource().createUncachedResolver(r);for(int qy=0;qy<4;qy++){var expected=resolver.getNoiseBiome(p.getX()>>2,qy,p.getZ()>>2);RiverSmoke.require(chunk.getNoiseBiome(p.getX()>>2,qy,p.getZ()>>2).equals(expected)&&copy.biomes().get((p.getX()&15)>>2,qy,(p.getZ()&15)>>2).equals(expected),"Dry bank source/native/decoded biome routing at all heights");}RiverSmoke.require(d.sample(p.getX(),p.getZ()).waterLevel()==DrainageNetwork.DRY,"Dry bank datum");Blockbranch.LOGGER.info("RIVER_BANK_ZOOM_DIAGNOSTIC position={} stored={} zoomed={}",p,chunk.getNoiseBiome(p.getX()>>2,0,p.getZ()>>2),l.getBiome(p));
   for(int y=p.getY()-2;y<=p.getY()+2;y++){var q=new BlockPos(p.getX(),y,p.getZ());var expected=g.surfaceState(q.getX(),y,q.getZ(),r);RiverSmoke.require(l.getBlockState(q)==expected&&copy.state(q.getX(),y,q.getZ())==expected&&copy.generate(y>>4).getBlockState(q.getX()&15,y&15,q.getZ()&15)==expected,"Dry bank native/profile/client states");}
   RiverSmoke.require(l.getBlockState(p).is(k==0?Blocks.MUD:Blocks.SAND)&&l.getFluidState(p).isEmpty()&&l.getBlockState(p.above()).isAir()&&l.getBlockState(p.below()).isSolid(),"Dry fines have solid support and no phantom water/plants");l.getBlockState(p).tick(l,p,l.getRandom());RiverSmoke.require(l.getBlockState(p)==base.state(p.getX(),p.getY(),p.getZ()),"Native sand remains supported");
   for(var type:Heightmap.Types.values())RiverSmoke.require(chunk.getHeight(type,p.getX()&15,p.getZ()&15)+1==g.getBaseHeight(p.getX(),p.getZ(),type,l,r),"Native dry-bank heightmaps");
   var old=base.encode(l.palettedContainerFactory());old.putInt("version",33);reject(old,l,"Unsupported procedural terrain version");
   var aquatic=base.encode(l.palettedContainerFactory());aquatic.getByteArray("river_margin").orElseThrow()[index]=5;reject(aquatic,l,"Invalid aquatic sediment/water cover");
   var unknown=base.encode(l.palettedContainerFactory());unknown.getByteArray("river_margin").orElseThrow()[index]=15;reject(unknown,l,"Invalid river-margin cover");
   var flooded=base.encode(l.palettedContainerFactory());flooded.getIntArray("water").orElseThrow()[index]=p.getY()+3;reject(flooded,l,"Dry River bank requires");
   var wrong=base.encode(l.palettedContainerFactory());var biomes=new PalettedContainer<>(l.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS),l.palettedContainerFactory().biomeStrategy());wrong.store("biomes",l.palettedContainerFactory().biomeContainerCodec(),biomes);RiverSmoke.require(TerrainProfile.decode(wrong,l.palettedContainerFactory()).state(p.getX(),p.getY(),p.getZ())==base.state(p.getX(),p.getY(),p.getZ()),"Physical dry bank paint independent of coarse biome identity");
   var onePlane=base.encode(l.palettedContainerFactory());var partial=new PalettedContainer<>(l.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.RIVER),l.palettedContainerFactory().biomeStrategy());partial.set((p.getX()&15)>>2,1,(p.getZ()&15)>>2,l.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS));onePlane.store("biomes",l.palettedContainerFactory().biomeContainerCodec(),partial);RiverSmoke.require(TerrainProfile.decode(onePlane,l.palettedContainerFactory()).state(p.getX(),p.getY(),p.getZ())==base.state(p.getX(),p.getY(),p.getZ()),"Biome height slots do not clip physical material");
   RiverSmoke.require(base.encode(l.palettedContainerFactory()).getByteArray("river_margin").orElseThrow()[index]==13+k,"Malformed snapshots do not mutate original dry bank");
   l.setBlock(p,Blocks.CLAY.defaultBlockState(),3);l.setBlock(p.below(8),Blocks.GOLD_BLOCK.defaultBlockState(),2);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);RiverSmoke.require(l.getBlockState(p).is(Blocks.CLAY)&&l.getBlockState(p.below(8)).is(Blocks.GOLD_BLOCK),"Dry bank edits survive eviction");
  }
  var wet=RiverSmoke.fixture(g,r);var wetBase=((SparseColumn)l.getChunkAt(wet)).blockbranch$terrain().base();var malformed=wetBase.encode(l.palettedContainerFactory());malformed.getByteArray("river_margin").orElseThrow()[(wet.getZ()&15)*16+(wet.getX()&15)]=13;reject(malformed,l,"Dry River bank requires");
  Blockbranch.LOGGER.info("RIVER_DRY_BANK_DOMAIN_NATIVE_PASS {} scene={} samples={} mud={} sand={}",reload?"RELOAD":"NEW",scene,count,witness[0],witness[1]);
 }
 private static void reject(net.minecraft.nbt.CompoundTag t,ServerLevel l,String reason){try{TerrainProfile.decode(t,l.palettedContainerFactory());throw new AssertionError("Malformed sediment accepted: "+reason);}catch(IllegalArgumentException e){if(!e.getMessage().contains(reason))throw new AssertionError("Wrong rejection: "+e.getMessage(),e);}}
 private RiverBankSmoke(){}
}
