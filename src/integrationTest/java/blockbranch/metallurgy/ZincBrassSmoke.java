package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native zinc/brass qualification and independently compiled prior tin geometry. */
public final class ZincBrassSmoke {
    public static void verify(ServerLevel level,Path directory)throws Exception{
        recipes(level);drops(level);placement(level);legacy(level,directory);edits(level,directory);natural(level,directory);
    }
    private static void recipes(ServerLevel level){
        var manager=level.getServer().getRecipeManager();
        var alloy=CraftingInput.of(2,2,List.of(new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),new ItemStack(ZincBrassContent.ZINC_INGOT)));
        var output=manager.getRecipeFor(RecipeType.CRAFTING,alloy,level).orElseThrow().value().assemble(alloy);require(output.is(ZincBrassContent.BRASS_INGOT)&&output.getCount()==4,"3 copper + 1 zinc conserves 4 brass");
        String[][] patterns={{"BBB","BBB","BBB"},{"BBB"},{"B  ","BB ","BBB"},{"BBB","BBB"}};
        var materials=List.of(ZincBrassContent.BRASS_INGOT,ZincBrassContent.BRASS_BLOCK.asItem(),ZincBrassContent.BRASS_BLOCK.asItem(),ZincBrassContent.BRASS_INGOT);
        var results=List.of(ZincBrassContent.BRASS_BLOCK,ZincBrassContent.BRASS_SLAB,ZincBrassContent.BRASS_STAIRS,ZincBrassContent.BRASS_BARS);int[] counts={1,6,4,16};
        for(int i=0;i<4;i++){
            var cells=new ArrayList<ItemStack>();for(String row:patterns[i])for(char c:row.toCharArray())cells.add(c=='B'?new ItemStack(materials.get(i)):ItemStack.EMPTY);
            var input=CraftingInput.of(3,patterns[i].length,cells);var result=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(result.is(results.get(i).asItem())&&result.getCount()==counts[i],"native brass building recipe count");
        }
        var unpack=CraftingInput.of(1,1,List.of(new ItemStack(ZincBrassContent.BRASS_BLOCK)));var ingots=manager.getRecipeFor(RecipeType.CRAFTING,unpack,level).orElseThrow().value().assemble(unpack);require(ingots.is(ZincBrassContent.BRASS_INGOT)&&ingots.getCount()==9,"reversible brass storage");
        for(var source:List.of(ZincBrassContent.RAW_ZINC,ZincBrassContent.ZINC_ORE.asItem(),ZincBrassContent.DEEPSLATE_ZINC_ORE.asItem())){
            var input=new SingleRecipeInput(new ItemStack(source));require(manager.getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow().value().assemble(input).is(ZincBrassContent.ZINC_INGOT),"native zinc smelting");require(manager.getRecipeFor(RecipeType.BLASTING,input,level).orElseThrow().value().assemble(input).is(ZincBrassContent.ZINC_INGOT),"native zinc blasting");
        }
    }
    private static void drops(ServerLevel level){
        var pick=new ItemStack(MetallurgyContent.BRONZE_PICKAXE);var silk=pick.copy();silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        for(var ore:List.of(ZincBrassContent.ZINC_ORE,ZincBrassContent.DEEPSLATE_ZINC_ORE)){
            require(pick.isCorrectToolForDrops(ore.defaultBlockState())&&!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()),"zinc harvest gate");
            var raw=Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,pick);require(raw.size()==1&&raw.getFirst().is(ZincBrassContent.RAW_ZINC)&&raw.getFirst().getCount()==1,"native raw zinc drop");
            require(Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,silk).getFirst().is(ore.asItem()),"zinc silk touch");
        }
        var doubleSlab=ZincBrassContent.BRASS_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE);var result=Block.getDrops(doubleSlab,level,BlockPos.ZERO,null,null,pick);require(result.size()==1&&result.getFirst().is(ZincBrassContent.BRASS_SLAB.asItem())&&result.getFirst().getCount()==2,"double brass slab drops two slabs");
        for(var block:List.of(ZincBrassContent.BRASS_BLOCK,ZincBrassContent.BRASS_STAIRS,ZincBrassContent.BRASS_BARS))require(Block.getDrops(block.defaultBlockState(),level,BlockPos.ZERO,null,null,pick).getFirst().is(block.asItem()),"brass self drop");
    }
    private static void placement(ServerLevel level){
        var player=new net.minecraft.world.entity.player.Player(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BrassBuilder")){
            @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.CREATIVE;}
        };player.getInventory().setSelectedSlot(0);player.setYRot(90);
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,128,0)+4;
        var slab=new BlockPos(128,y,0);var stair=new BlockPos(130,y,0);var left=new BlockPos(132,y,0);var right=left.east();
        for(var pos:List.of(slab,stair,left,right)){level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        place(level,player,slab.below(),ZincBrassContent.BRASS_SLAB);require(level.getBlockState(slab).getValue(SlabBlock.TYPE)==SlabType.BOTTOM,"native lower slab placement");
        place(level,player,slab,ZincBrassContent.BRASS_SLAB);require(level.getBlockState(slab).getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"native slab combination");
        level.setBlock(stair,Blocks.WATER.defaultBlockState(),2);place(level,player,stair.below(),ZincBrassContent.BRASS_STAIRS);
        require(level.getBlockState(stair).is(ZincBrassContent.BRASS_STAIRS)&&level.getBlockState(stair).getValue(StairBlock.FACING)==Direction.WEST&&level.getBlockState(stair).getValue(StairBlock.WATERLOGGED),"native stair direction/waterlogging");
        place(level,player,left.below(),ZincBrassContent.BRASS_BARS);place(level,player,right.below(),ZincBrassContent.BRASS_BARS);
        require(level.getBlockState(left).getValue(IronBarsBlock.EAST)&&level.getBlockState(right).getValue(IronBarsBlock.WEST),"native adjoining bar connections");
    }
    private static void place(ServerLevel level,net.minecraft.world.entity.player.Player player,BlockPos support,Block block){
        var stack=new ItemStack(block);player.getInventory().setItem(0,stack);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
    }
    private static void legacy(ServerLevel level,Path directory)throws Exception{
        var url=Path.of(System.getProperty("blockbranch.oldArmorCatalogClasses")).toUri().toURL();long checked=0;int zinc=0;
        try(var loader=MetallurgySmoke.historicalReader(url)){
            var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,net.minecraft.world.level.block.state.BlockState.class,boolean.class);
            for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(boolean caves:new boolean[]{false,true}){
                var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,3,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,3,65535,65535).bind(64,64);var current=new OreLayer(seed,cx,-1,4,65535,65535).bind(64,64);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){
                    int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=state.invoke(frozen,x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"published tin definition3 remains exact");var next=current.state(x,y,z,64,host,caves);
                    if(expected!=host)require(next==expected,"zinc cannot replace prior ores or tin");else if(next!=host){require(next.is(ZincBrassContent.ZINC_ORE)||next.is(ZincBrassContent.DEEPSLATE_ZINC_ORE),"only zinc added to remaining hosts");zinc++;}checked++;
                }
            }
            try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,4,0,0).encode());throw new AssertionError("old ore reader accepted4");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"prior tin reader safely rejects4");}
            var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            for(boolean decoration:new boolean[]{false,true}){
                var root=Files.createTempDirectory(directory,"old-armor-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);
                try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog3 reader accepted4");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"previous armor reader rejects catalog4");}require(Files.readString(marker).equals(before),"older marker rejection leaves data untouched");
            }
        }
        require(zinc>0,"positive definition4 zinc witnesses");Blockbranch.LOGGER.info("ZINC_BRASS frozenTinComparisons={} zincWitnesses={} oldReaderRejects4=true",checked,zinc);
    }
    private static void edits(ServerLevel level,Path directory)throws Exception{
        var marker=directory.resolve("brass-edits.txt");int y;
        if(Files.exists(marker))y=Integer.parseInt(Files.readString(marker).trim());else{
            y=level.getHeight(Heightmap.Types.WORLD_SURFACE,96,0)+4;
            level.setBlock(new BlockPos(96,y,0),ZincBrassContent.BRASS_BLOCK.defaultBlockState(),2);
            level.setBlock(new BlockPos(97,y,0),ZincBrassContent.BRASS_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),2);
            level.setBlock(new BlockPos(98,y,0),ZincBrassContent.BRASS_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST).setValue(StairBlock.WATERLOGGED,true),2);
            level.setBlock(new BlockPos(99,y,0),ZincBrassContent.BRASS_BARS.defaultBlockState().setValue(IronBarsBlock.EAST,true).setValue(IronBarsBlock.WATERLOGGED,true),2);
            level.setBlock(new BlockPos(100,y,0),ZincBrassContent.BRASS_BARS.defaultBlockState().setValue(IronBarsBlock.WEST,true),2);Files.writeString(marker,Integer.toString(y));level.getServer().saveEverything(false,true,true);
        }
        require(level.getBlockState(new BlockPos(96,y,0)).is(ZincBrassContent.BRASS_BLOCK),"saved brass block");var slab=level.getBlockState(new BlockPos(97,y,0));require(slab.is(ZincBrassContent.BRASS_SLAB)&&slab.getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"saved double slab");
        var stair=level.getBlockState(new BlockPos(98,y,0));require(stair.is(ZincBrassContent.BRASS_STAIRS)&&stair.getValue(StairBlock.FACING)==Direction.WEST&&stair.getValue(StairBlock.WATERLOGGED),"saved stair orientation and waterlogging");
        var bars=level.getBlockState(new BlockPos(99,y,0));require(bars.is(ZincBrassContent.BRASS_BARS)&&bars.getValue(IronBarsBlock.EAST)&&bars.getValue(IronBarsBlock.WATERLOGGED),"saved bar connection/waterlogging");
    }
    private static void natural(ServerLevel level,Path directory)throws Exception{
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator))return;
        if(generator.oreDefinition()<4){require(generator.oreDefinition()==3,"retained prior tin world definition");Blockbranch.LOGGER.info("ZINC_BRASS legacy world keeps ore3; no zinc backfill");return;}
        require(generator.oreDefinition()>=4,"current zinc generation definition");var marker=directory.resolve("zinc-mined.txt");
        if(Files.exists(marker)){
            var values=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(values[0]),Integer.parseInt(values[1]),Integer.parseInt(values[2]));require(level.getBlockState(pos).isAir(),"natural zinc mining edit survives relaunch");var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();require(isZinc(base.state(pos.getX(),pos.getY(),pos.getZ())),"saved zinc remains in procedural base");return;
        }
        var origin=generator.getOrigin(level.getChunkSource().randomState());
        for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){
            var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();require(base.ores().definition()==generator.oreDefinition(),"actual FULL column carries its saved ore definition");var restored=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=ZincVeins.MIN_DEPTH;depth<=ZincVeins.MAX_DEPTH;depth++){
                int y=base.groundHeight(x,z)-depth;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);var value=base.state(x,y,z);if(!isZinc(value))continue;
                require(level.getBlockState(pos)==value&&restored.state(x,y,z)==value,"native zinc agrees with snapshot");level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());level.getServer().saveEverything(false,true,true);Blockbranch.LOGGER.info("ZINC_BRASS naturally generated zinc witness={}",pos);return;
            }
        }
        throw new AssertionError("no natural zinc in four native columns");
    }
    private static boolean isZinc(net.minecraft.world.level.block.state.BlockState state){return state.is(ZincBrassContent.ZINC_ORE)||state.is(ZincBrassContent.DEEPSLATE_ZINC_ORE);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private ZincBrassSmoke(){}
}
