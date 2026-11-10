package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
/** Native manganese/steel progression and frozen prior ore qualification. */
public final class ManganeseSteelSmoke {
    private static Path DIRECTORY;
    public static void verify(ServerLevel level,Path directory)throws Exception{DIRECTORY=directory;recipes(level);tools(level);mining(level);legacy(level,directory);natural(level,directory);}
    private static void tools(ServerLevel level)throws Exception{
        var tools=List.of(SteelContent.STEEL_SWORD,SteelContent.STEEL_AXE,SteelContent.STEEL_SHOVEL,SteelContent.STEEL_HOE);
        var tags=List.of(net.minecraft.tags.ItemTags.SWORDS,net.minecraft.tags.ItemTags.AXES,net.minecraft.tags.ItemTags.SHOVELS,net.minecraft.tags.ItemTags.HOES);
        String[] names={"sword","axe","shovel","hoe"};String[][] patterns={{"B","B","S"},{"BB","BS"," S"},{"B","S","S"},{"BB"," S"," S"}};
        var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var saved=DIRECTORY.resolve("steel-tools.dat");
        var previous=Files.exists(saved)?net.minecraft.nbt.NbtIo.readCompressed(saved,net.minecraft.nbt.NbtAccounter.unlimitedHeap()):null;
        var next=new net.minecraft.nbt.CompoundTag();
        for(int i=0;i<tools.size();i++){
            var cells=new ArrayList<ItemStack>();for(String row:patterns[i])for(char ch:row.toCharArray())cells.add(ch=='B'?new ItemStack(SteelContent.STEEL_INGOT):ch=='S'?new ItemStack(Items.STICK):ItemStack.EMPTY);
            var input=CraftingInput.of(patterns[i][0].length(),patterns[i].length,cells);
            var output=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(output.is(tools.get(i))&&output.is(tags.get(i)),"native steel "+names[i]+" recipe and tool/enchantment tag");
            require(output.getMaxDamage()==480&&output.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT))&&!output.isValidRepairItem(new ItemStack(Items.COPPER_INGOT)),"shared steel durability/repair");
            if(previous!=null){var restored=ItemStack.CODEC.parse(ops,previous.getCompoundOrEmpty(names[i])).getOrThrow();require(restored.is(tools.get(i))&&restored.getDamageValue()==37,"saved tool identity and damage survive relaunch");}
            output.setDamageValue(37);next.put(names[i],ItemStack.CODEC.encodeStart(ops,output).getOrThrow());
        }
        net.minecraft.nbt.NbtIo.writeCompressed(next,saved);
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SteelActions");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,48,0)+4;
        Block[] sources={Blocks.OAK_LOG,Blocks.GRASS_BLOCK,Blocks.GRASS_BLOCK};Block[] results={Blocks.STRIPPED_OAK_LOG,Blocks.DIRT_PATH,Blocks.FARMLAND};
        for(int i=0;i<3;i++){
            var pos=new BlockPos(48+i,y,0);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,sources[i].defaultBlockState(),2);
            var tool=new ItemStack(tools.get(i+1));player.getInventory().setItem(0,tool);player.getInventory().setSelectedSlot(0);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
            tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            require(level.getBlockState(pos).is(results[i])&&tool.getDamageValue()==1,"native "+names[i+1]+" action and durability");
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            require(level.getBlockState(pos).is(Blocks.STONE)&&tool.getDamageValue()==1,"unsupported action leaves blocks and durability unchanged");
        }
        var sword=new ItemStack(SteelContent.STEEL_SWORD);player.getInventory().setItem(0,sword);
        var target=net.minecraft.world.entity.EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        require(target!=null&&sword.hurtEnemy(target,player),"native sword weapon component");sword.postHurtEnemy(target,player);require(sword.getDamageValue()==1,"native sword hit durability");
        var attribute=net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;var slot=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        double copper=new ItemStack(Items.COPPER_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double steel=sword.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double iron=new ItemStack(Items.IRON_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double diamond=new ItemStack(Items.DIAMOND_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        require(steel==iron&&steel<diamond,"steel retains iron attack strength and diamond advantage");
        Blockbranch.LOGGER.info("STEEL_TOOLS native recipes/actions/negative controls/sword durability and saved-stack reload={}",previous!=null);
    }
    private static void recipes(ServerLevel level){
        var manager=level.getServer().getRecipeManager();
        for(var carbon:List.of(Items.COAL,Items.CHARCOAL)){
            var cells=new ArrayList<ItemStack>();for(int i=0;i<7;i++)cells.add(new ItemStack(Items.IRON_INGOT));cells.add(new ItemStack(SteelContent.MANGANESE_INGOT));cells.add(new ItemStack(carbon));
            var input=CraftingInput.of(3,3,cells);var result=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(result.is(SteelContent.STEEL_INGOT)&&result.getCount()==8,"iron + manganese + native coal/charcoal conserves eight metal ingots");
            for(int index:new int[]{0,7,8}){var bad=new ArrayList<>(cells);bad.set(index,ItemStack.EMPTY);require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,bad),level).isEmpty(),"steel cannot omit iron, manganese or carbon");}
        }
        var blockInput=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(i->new ItemStack(SteelContent.STEEL_INGOT)).toList());require(manager.getRecipeFor(RecipeType.CRAFTING,blockInput,level).orElseThrow().value().assemble(blockInput).is(SteelContent.STEEL_BLOCK.asItem()),"steel block crafting");
        var unpack=CraftingInput.of(1,1,List.of(new ItemStack(SteelContent.STEEL_BLOCK)));var ingots=manager.getRecipeFor(RecipeType.CRAFTING,unpack,level).orElseThrow().value().assemble(unpack);require(ingots.is(SteelContent.STEEL_INGOT)&&ingots.getCount()==9,"steel storage conservation");
        var pick=CraftingInput.of(3,3,List.of(new ItemStack(SteelContent.STEEL_INGOT),new ItemStack(SteelContent.STEEL_INGOT),new ItemStack(SteelContent.STEEL_INGOT),ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY));require(manager.getRecipeFor(RecipeType.CRAFTING,pick,level).orElseThrow().value().assemble(pick).is(SteelContent.STEEL_PICKAXE),"steel pickaxe crafting");
        for(var source:List.of(SteelContent.RAW_MANGANESE,SteelContent.MANGANESE_ORE.asItem(),SteelContent.DEEPSLATE_MANGANESE_ORE.asItem())){var input=new SingleRecipeInput(new ItemStack(source));require(manager.getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow().value().assemble(input).is(SteelContent.MANGANESE_INGOT),"native manganese smelting");require(manager.getRecipeFor(RecipeType.BLASTING,input,level).orElseThrow().value().assemble(input).is(SteelContent.MANGANESE_INGOT),"native manganese blasting");}
    }
    private static void mining(ServerLevel level)throws Exception{
        var steel=new ItemStack(SteelContent.STEEL_PICKAXE);var iron=new ItemStack(Items.IRON_PICKAXE);var diamond=new ItemStack(Items.DIAMOND_PICKAXE);var bronze=new ItemStack(MetallurgyContent.BRONZE_PICKAXE);
        require(iron.getMaxDamage()<steel.getMaxDamage()&&steel.getMaxDamage()<diamond.getMaxDamage(),"steel wear life between iron and diamond");
        require(iron.getDestroySpeed(Blocks.STONE.defaultBlockState())<steel.getDestroySpeed(Blocks.STONE.defaultBlockState())&&steel.getDestroySpeed(Blocks.STONE.defaultBlockState())<diamond.getDestroySpeed(Blocks.STONE.defaultBlockState()),"steel mining speed between iron and diamond");
        require(steel.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())&&!steel.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())&&diamond.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"native iron-tier access preserves diamond mining role");
        var silk=steel.copy();silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        for(var ore:List.of(SteelContent.MANGANESE_ORE,SteelContent.DEEPSLATE_MANGANESE_ORE)){require(steel.isCorrectToolForDrops(ore.defaultBlockState())&&iron.isCorrectToolForDrops(ore.defaultBlockState())&&!bronze.isCorrectToolForDrops(ore.defaultBlockState()),"manganese requires iron-tier mining");var drops=Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,steel);require(drops.size()==1&&drops.getFirst().is(SteelContent.RAW_MANGANESE)&&drops.getFirst().getCount()==1,"native manganese raw drop");require(Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,silk).getFirst().is(ore.asItem()),"manganese silk touch");}
        require(Block.getDrops(SteelContent.STEEL_BLOCK.defaultBlockState(),level,BlockPos.ZERO,null,null,steel).getFirst().is(SteelContent.STEEL_BLOCK.asItem()),"steel block drop");
        var marker=DIRECTORY.resolve("steel-block.txt");int y;if(Files.exists(marker)){y=Integer.parseInt(Files.readString(marker).trim());require(level.getBlockState(new BlockPos(192,y,0)).is(SteelContent.STEEL_BLOCK),"steel block survives relaunch");}else{y=level.getHeight(Heightmap.Types.WORLD_SURFACE,192,0)+4;level.setBlock(new BlockPos(192,y,0),SteelContent.STEEL_BLOCK.defaultBlockState(),2);Files.writeString(marker,Integer.toString(y));level.getServer().saveEverything(false,true,true);}
    }
    private static void legacy(ServerLevel level,Path directory)throws Exception{
        var url=Path.of(System.getProperty("blockbranch.oldZincCatalogClasses")).toUri().toURL();long checked=0;int manganese=0;
        try(var loader=MetallurgySmoke.historicalReader(url)){
            var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,net.minecraft.world.level.block.state.BlockState.class,boolean.class);
            for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(boolean caves:new boolean[]{false,true}){
                var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,4,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,4,65535,65535).bind(64,64);var current=new OreLayer(seed,cx,-1,5,65535,65535).bind(64,64);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){
                    int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=state.invoke(frozen,x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"published zinc definition4 remains exact");var next=current.state(x,y,z,64,host,caves);
                    if(expected!=host)require(next==expected,"manganese cannot replace prior ores, tin or zinc");else if(next!=host){require(next.is(SteelContent.MANGANESE_ORE)||next.is(SteelContent.DEEPSLATE_MANGANESE_ORE),"only manganese added to remaining hosts");manganese++;}checked++;
                }
            }
            try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,5,0,0).encode());throw new AssertionError("old ore reader accepted5");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"prior zinc reader safely rejects5");}
            var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            for(boolean decoration:new boolean[]{false,true}){
                var root=Files.createTempDirectory(directory,"old-zinc-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);
                try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog4 reader accepted5");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"previous zinc reader rejects catalog5");}require(Files.readString(marker).equals(before),"older marker rejection leaves data untouched");
            }
        }
        require(manganese>0,"positive definition5 manganese witnesses");Blockbranch.LOGGER.info("MANGANESE_STEEL frozenZincComparisons={} manganeseWitnesses={} oldReaderRejects5=true",checked,manganese);
    }
    private static void natural(ServerLevel level,Path directory)throws Exception{
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator))return;
        if(generator.oreDefinition()<5){require((generator.oreDefinition()==3||generator.oreDefinition()==4),"retained prior tin world definition");Blockbranch.LOGGER.info("MANGANESE_STEEL legacy world keeps prior ore definition; no manganese backfill");return;}
        require(generator.oreDefinition()>=5,"current manganese generation definition");var marker=directory.resolve("manganese-mined.txt");
        if(Files.exists(marker)){
            var values=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(values[0]),Integer.parseInt(values[1]),Integer.parseInt(values[2]));require(level.getBlockState(pos).isAir(),"natural manganese mining edit survives relaunch");var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();require(isManganese(base.state(pos.getX(),pos.getY(),pos.getZ())),"saved manganese remains in procedural base");return;
        }
        var origin=generator.getOrigin(level.getChunkSource().randomState());
        for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){
            var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();require(base.ores().definition()==generator.oreDefinition(),"actual FULL column carries its saved ore definition");var restored=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=ManganeseVeins.MIN_DEPTH;depth<=ManganeseVeins.MAX_DEPTH;depth++){
                int y=base.groundHeight(x,z)-depth;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);var value=base.state(x,y,z);if(!isManganese(value))continue;
                require(level.getBlockState(pos)==value&&restored.state(x,y,z)==value,"native manganese agrees with snapshot");level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());level.getServer().saveEverything(false,true,true);Blockbranch.LOGGER.info("MANGANESE_STEEL naturally generated manganese witness={}",pos);return;
            }
        }
        throw new AssertionError("no natural manganese in four native columns");
    }
    private static boolean isManganese(net.minecraft.world.level.block.state.BlockState state){return state.is(SteelContent.MANGANESE_ORE)||state.is(SteelContent.DEEPSLATE_MANGANESE_ORE);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private ManganeseSteelSmoke(){}
}
