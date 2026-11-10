package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
/** Native lead/tin conservation, building behavior and prior-definition compatibility. */
public final class LeadSmoke {
 public static void verify(ServerLevel level,Path directory)throws Exception{recipes(level);drops(level);placement(level);edits(level,directory);legacy(level,directory);natural(level,directory);}
 private static void recipes(ServerLevel level){
  var manager=level.getServer().getRecipeManager();var cells=List.of(new ItemStack(LeadContent.LEAD_INGOT),new ItemStack(LeadContent.LEAD_INGOT),new ItemStack(LeadContent.LEAD_INGOT),new ItemStack(MetallurgyContent.TIN_INGOT));var input=CraftingInput.of(2,2,cells);var out=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);require(out.is(LeadContent.LEAD_ALLOY_INGOT)&&out.getCount()==4,"3 lead + 1 tin conserves 4 alloy ingots");for(int k:new int[]{0,3}){var bad=new ArrayList<>(cells);bad.set(k,ItemStack.EMPTY);require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(2,2,bad),level).isEmpty(),"cannot omit lead/tin");}
  for(var source:List.of(LeadContent.RAW_LEAD,LeadContent.LEAD_ORE.asItem(),LeadContent.DEEPSLATE_LEAD_ORE.asItem())){var in=new SingleRecipeInput(new ItemStack(source));require(manager.getRecipeFor(RecipeType.SMELTING,in,level).orElseThrow().value().assemble(in).is(LeadContent.LEAD_INGOT),"lead smelting");require(manager.getRecipeFor(RecipeType.BLASTING,in,level).orElseThrow().value().assemble(in).is(LeadContent.LEAD_INGOT),"lead blasting");}
  for(boolean alloy:new boolean[]{false,true}){var ingot=alloy?LeadContent.LEAD_ALLOY_INGOT:LeadContent.LEAD_INGOT;var block=alloy?LeadContent.LEAD_ALLOY_BLOCK:LeadContent.LEAD_BLOCK;var outputs=List.of(block,alloy?LeadContent.LEAD_ALLOY_SLAB:LeadContent.LEAD_SLAB,alloy?LeadContent.LEAD_ALLOY_STAIRS:LeadContent.LEAD_STAIRS);String[][] shapes={{"BBB","BBB","BBB"},{"BBB"},{"B  ","BB ","BBB"}};int[] counts={1,6,4};for(int k=0;k<3;k++){var grid=new ArrayList<ItemStack>();for(String row:shapes[k])for(char c:row.toCharArray())grid.add(c=='B'?new ItemStack(k==0?ingot:block.asItem()):ItemStack.EMPTY);var in=CraftingInput.of(3,shapes[k].length,grid);var result=manager.getRecipeFor(RecipeType.CRAFTING,in,level).orElseThrow().value().assemble(in);require(result.is(outputs.get(k).asItem())&&result.getCount()==counts[k],"native lead building recipe counts");}var unpack=CraftingInput.of(1,1,List.of(new ItemStack(block)));var result=manager.getRecipeFor(RecipeType.CRAFTING,unpack,level).orElseThrow().value().assemble(unpack);require(result.is(ingot)&&result.getCount()==9,"reversible metal storage");}
 }
 private static void drops(ServerLevel level){
  var pick=new ItemStack(MetallurgyContent.BRONZE_PICKAXE);var silk=pick.copy();silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
  for(var ore:List.of(LeadContent.LEAD_ORE,LeadContent.DEEPSLATE_LEAD_ORE)){require(pick.isCorrectToolForDrops(ore.defaultBlockState())&&new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState())&&!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()),"stone-tier lead harvest gate");var raw=Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,pick);require(raw.size()==1&&raw.getFirst().is(LeadContent.RAW_LEAD),"raw lead drop");require(Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,silk).getFirst().is(ore.asItem()),"lead silk touch");}
  for(boolean alloy:new boolean[]{false,true}){var block=alloy?LeadContent.LEAD_ALLOY_BLOCK:LeadContent.LEAD_BLOCK;var slab=alloy?LeadContent.LEAD_ALLOY_SLAB:LeadContent.LEAD_SLAB;var stair=alloy?LeadContent.LEAD_ALLOY_STAIRS:LeadContent.LEAD_STAIRS;var drops=Block.getDrops(slab.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),level,BlockPos.ZERO,null,null,pick);require(drops.size()==1&&drops.getFirst().is(slab.asItem())&&drops.getFirst().getCount()==2,"double slab drops two");for(var b:List.of(block,stair))require(Block.getDrops(b.defaultBlockState(),level,BlockPos.ZERO,null,null,pick).getFirst().is(b.asItem()),"metal building self drops");}
  require(LeadContent.LEAD_ALLOY_BLOCK.defaultBlockState().getDestroySpeed(level,BlockPos.ZERO)>LeadContent.LEAD_BLOCK.defaultBlockState().getDestroySpeed(level,BlockPos.ZERO)&&LeadContent.LEAD_ALLOY_BLOCK.getExplosionResistance()>LeadContent.LEAD_BLOCK.getExplosionResistance(),"alloy native mining-time/blast-resistance tradeoff");
 }
 private static void placement(ServerLevel level){
  var player=new net.minecraft.world.entity.player.Player(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LeadBuilder")){@Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.CREATIVE;}};player.getInventory().setSelectedSlot(0);player.setYRot(90);
  for(boolean alloy:new boolean[]{false,true}){int x=alloy?400:384,y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,0)+4;var slab=alloy?LeadContent.LEAD_ALLOY_SLAB:LeadContent.LEAD_SLAB;var stair=alloy?LeadContent.LEAD_ALLOY_STAIRS:LeadContent.LEAD_STAIRS;var a=new BlockPos(x,y,0);var b=a.east(2);var wet=a.east(4);for(var p:List.of(a,b,wet)){level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}place(player,a.below(),slab);require(level.getBlockState(a).getValue(SlabBlock.TYPE)==SlabType.BOTTOM,"native lower slab");place(player,a,slab);require(level.getBlockState(a).getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"native slab combination");level.setBlock(b,Blocks.WATER.defaultBlockState(),2);place(player,b.below(),stair);require(level.getBlockState(b).is(stair)&&level.getBlockState(b).getValue(StairBlock.FACING)==Direction.WEST&&level.getBlockState(b).getValue(StairBlock.WATERLOGGED),"stair orientation/waterlogging");level.setBlock(wet,Blocks.WATER.defaultBlockState(),2);place(player,wet.below(),slab);require(level.getBlockState(wet).is(slab)&&level.getBlockState(wet).getValue(SlabBlock.WATERLOGGED),"slab waterlogging");}
 }
 private static void place(net.minecraft.world.entity.player.Player p,BlockPos support,Block block){var stack=new ItemStack(block);p.getInventory().setItem(0,stack);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);stack.useOn(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit));}
 private static void edits(ServerLevel level,Path directory)throws Exception{
  var marker=directory.resolve("lead-building-edits.txt");boolean prior=Files.exists(marker);int y=prior?Integer.parseInt(Files.readString(marker).trim()):level.getHeight(Heightmap.Types.WORLD_SURFACE,352,0)+4;
  for(boolean alloy:new boolean[]{false,true}){int x=alloy?356:352;var block=alloy?LeadContent.LEAD_ALLOY_BLOCK:LeadContent.LEAD_BLOCK;var slab=alloy?LeadContent.LEAD_ALLOY_SLAB:LeadContent.LEAD_SLAB;var stair=alloy?LeadContent.LEAD_ALLOY_STAIRS:LeadContent.LEAD_STAIRS;var a=new BlockPos(x,y,0);var b=a.east();var c=a.east(2);if(!prior){level.setBlock(a,block.defaultBlockState(),2);level.setBlock(b,slab.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),2);level.setBlock(c,stair.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST).setValue(StairBlock.WATERLOGGED,true),2);}require(level.getBlockState(a).is(block),"saved metal block");require(level.getBlockState(b).is(slab)&&level.getBlockState(b).getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"saved double slab");require(level.getBlockState(c).is(stair)&&level.getBlockState(c).getValue(StairBlock.FACING)==Direction.WEST&&level.getBlockState(c).getValue(StairBlock.WATERLOGGED),"saved stair state");}if(!prior){Files.writeString(marker,Integer.toString(y));level.getServer().saveEverything(false,true,true);}
 }
    private static void legacy(ServerLevel level,Path directory)throws Exception{
        java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> predicate=v->v.is(LeadContent.LEAD_ORE)||v.is(LeadContent.DEEPSLATE_LEAD_ORE);require(!new OreLayer(1,0,0,7,0,0).maybeHas(predicate)&&new OreLayer(1,0,0,8,0,0).maybeHas(predicate)&&new OreLayer(1,0,0,8,0,0).affectsPredicate(predicate),"new lead descriptor predicates are version gated");
        var url=Path.of(System.getProperty("blockbranch.oldEquipmentCatalogClasses")).toUri().toURL();long checked=0;int lead=0;
        try(var loader=MetallurgySmoke.historicalReader(url)){
            var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,net.minecraft.world.level.block.state.BlockState.class,boolean.class);
            for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(boolean caves:new boolean[]{false,true}){
                var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,7,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,7,65535,65535).bind(64,64);var current=new OreLayer(seed,cx,-1,8,65535,65535).bind(64,64);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){
                    int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=state.invoke(frozen,x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"published stainless definition7 remains exact");var next=current.state(x,y,z,64,host,caves);
                    if(expected!=host)require(next==expected,"lead cannot replace prior ores");else if(next!=host){require(next.is(LeadContent.LEAD_ORE)||next.is(LeadContent.DEEPSLATE_LEAD_ORE),"only lead added to remaining hosts");require(depth>LeadVeins.MIN_DEPTH&&depth<LeadVeins.MAX_DEPTH,"lead stays inside its local depth band");lead++;}checked++;
                }
            }
            try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,8,0,0).encode());throw new AssertionError("old ore reader accepted8");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"prior equipment reader safely rejects8");}
            var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            for(boolean decoration:new boolean[]{false,true}){
                var root=Files.createTempDirectory(directory,"old-equipment-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);
                try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog8 reader accepted9");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"previous equipment reader rejects catalog9");}require(Files.readString(marker).equals(before),"older marker rejection leaves data untouched");
            }
        }
        require(lead>0,"positive definition8 lead witnesses");Blockbranch.LOGGER.info("LEAD frozenStainlessComparisons={} leadWitnesses={} oldReaderRejects8=true",checked,lead);
    }
    private static void natural(ServerLevel level,Path directory)throws Exception{
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator))return;
        if(generator.oreDefinition()<8){require((generator.oreDefinition()>=3&&generator.oreDefinition()<=7),"retained prior tin world definition");Blockbranch.LOGGER.info("LEAD legacy world keeps prior ore definition; no lead backfill");return;}
        require(generator.oreDefinition()>=8,"current lead generation definition");var marker=directory.resolve("lead-mined.txt");
        if(Files.exists(marker)){
            var values=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(values[0]),Integer.parseInt(values[1]),Integer.parseInt(values[2]));require(level.getBlockState(pos).isAir(),"natural lead mining edit survives relaunch");var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();require(isLead(base.state(pos.getX(),pos.getY(),pos.getZ())),"saved lead remains in procedural base");return;
        }
        var origin=generator.getOrigin(level.getChunkSource().randomState());
        for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){
            var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();require(base.ores().definition()==generator.oreDefinition(),"actual FULL column carries its saved ore definition");var restored=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=LeadVeins.MIN_DEPTH;depth<=LeadVeins.MAX_DEPTH;depth++){
                int y=base.groundHeight(x,z)-depth;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);var value=base.state(x,y,z);if(!isLead(value))continue;
                require(level.getBlockState(pos)==value&&restored.state(x,y,z)==value,"native lead agrees with snapshot");level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());level.getServer().saveEverything(false,true,true);Blockbranch.LOGGER.info("LEAD naturally generated lead witness={}",pos);return;
            }
        }
        throw new AssertionError("no natural lead in four native columns");
    }
    private static boolean isLead(net.minecraft.world.level.block.state.BlockState state){return state.is(LeadContent.LEAD_ORE)||state.is(LeadContent.DEEPSLATE_LEAD_ORE);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private LeadSmoke(){}
}
