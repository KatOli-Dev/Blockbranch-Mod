package blockbranch.metallurgy;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native magnetite attraction/equipment and an independently frozen published ore11 reader. */
public final class MagnetiteSmoke {
 public static void verify(ServerLevel l,Path d)throws Exception{MagnetiteMechanicsSmoke.verify(l,d);legacy(l,d);natural(l,d);}
 private static void legacy(ServerLevel l,Path d)throws Exception{
  long checked=0;int added=0;java.util.function.Predicate<BlockState> p=s->isMagnetite(s);require(!new OreLayer(1,0,0,11,0,0).maybeHas(p)&&new OreLayer(1,0,0,12,0,0).maybeHas(p)&&new OreLayer(1,0,0,12,0,0).affectsPredicate(p),"versioned magnetite predicates");
  try(var loader=MetallurgySmoke.historicalReader(Path.of(System.getProperty("blockbranch.oldTitaniumEquipmentClasses")).toUri().toURL())){var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,BlockState.class,boolean.class);
   for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(boolean caves:new boolean[]{false,true}){var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,11,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,11,65535,65535).bind(64,64);var current=new OreLayer(seed,cx,-1,12,65535,65535).bind(64,64);for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=(BlockState)state.invoke(frozen,x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"published ore11 exact");var next=current.state(x,y,z,64,host,caves);if(expected!=host)require(next==expected,"magnetite cannot replace earlier materials");else if(next!=host){require(isMagnetite(next)&&depth>MagnetiteVeins.MIN_DEPTH&&depth<MagnetiteVeins.MAX_DEPTH,"only magnetite in its band added");added++;}checked++;}}
   try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,12,0,0).encode());throw new AssertionError("old reader accepted12");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"ore11 reader rejects12");}
   var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);for(boolean decoration:new boolean[]{false,true}){var root=Files.createTempDirectory(d,"old-titanium-equipment-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog16 accepted17");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior reader refuses17");}require(Files.readString(marker).equals(before),"prior rejection preserves marker");}
  }
  require(added>0,"magnetite witnesses");blockbranch.Blockbranch.LOGGER.info("MAGNETITE frozenOre11Comparisons={} added={} oldReaderRejects12=true",checked,added);
 }
 private static void natural(ServerLevel l,Path d)throws Exception{
  if(!(l.getChunkSource().getGenerator() instanceof RealisticGenerator g))return;if(g.oreDefinition()<12){require(g.oreDefinition()>=3&&g.oreDefinition()<=11,"retained prior ore definition");return;}require(g.oreDefinition()>=12,"magnetite-compatible definition");var marker=d.resolve("magnetite-mined.txt");if(Files.exists(marker)){var v=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]));require(l.getBlockState(pos).isAir()&&isMagnetite(((SparseColumn)l.getChunkAt(pos)).blockbranch$terrain().base().state(pos.getX(),pos.getY(),pos.getZ())),"natural magnetite edit survives reopen");return;}
  var origin=g.getOrigin(l.getChunkSource().randomState());boolean found=false;search:for(int dz=0;dz<4;dz++)for(int dx=0;dx<4;dx++){var chunk=l.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();require(base.ores().definition()==g.oreDefinition(),"native descriptor12");var restored=TerrainProfile.decode(base.encode(l.palettedContainerFactory()),l.palettedContainerFactory());for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=48;depth<=192;depth++){int y=base.groundHeight(x,z)-depth;var value=base.state(x,y,z);if(!isMagnetite(value))continue;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);require(l.getBlockState(pos)==value&&restored.state(x,y,z)==value,"native magnetite agrees with snapshot");l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());l.getServer().saveEverything(false,true,true);blockbranch.Blockbranch.LOGGER.info("MAGNETITE natural witness={}",pos);found=true;break search;}}require(found,"bounded magnetite witness");
 }
 private static boolean isMagnetite(BlockState s){return s.is(MagnetiteContent.ORE)||s.is(MagnetiteContent.DEEPSLATE_ORE);}
 private static void require(boolean v,String message){if(!v)throw new AssertionError(message);}
 private MagnetiteSmoke(){}
}
