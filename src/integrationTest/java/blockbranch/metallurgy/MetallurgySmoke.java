package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import com.mojang.serialization.JsonOps;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ItemLike;

/** Native gameplay/codec qualification with an independently compiled, frozen old ore reader. */
public final class MetallurgySmoke {
    private static final Path DIRECTORY=Path.of(System.getProperty("blockbranch.metallurgyDirectory"));
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        try{
            var level=server.overworld();
            if(System.getProperty("blockbranch.metallurgyExpectedOre")!=null)require(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator&&generator.oreDefinition()==Integer.getInteger("blockbranch.metallurgyExpectedOre"),"explicit saved ore definition retained");
            if(Boolean.getBoolean("blockbranch.metallurgyLegacy")){
                require(level.getChunkSource().getGenerator() instanceof RealisticGenerator,"legacy fixture uses its saved Realistic generator");
                var generator=(RealisticGenerator)level.getChunkSource().getGenerator();require(generator.oreDefinition()==2,"existing world retains definition 2");
                ColumnSaveSafetySmoke.verifyStartupMarker(level,generator);
                var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow();
                require(RealisticGenerator.CODEC.codec().parse(ops,encoded).getOrThrow().oreDefinition()==2,"legacy roundtrip never promotes generation");
                require(Files.readString(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("blockbranch-columns.txt")).contains("metallurgy-catalog=20"),"legacy world gains minimum-reader catalog barrier");
                Files.writeString(DIRECTORY.resolve("result.txt"),"PASS legacy world keeps ore definition 2 with catalog barrier; no gameplay edits performed\n");
            }else{
                recipes(level);unlocks(level);dropsAndTools(level);bronzeTools(level);BronzeArmorSmoke.verify(level,DIRECTORY);ZincBrassSmoke.verify(level,DIRECTORY);ManganeseSteelSmoke.verify(level,DIRECTORY);SteelArmorSmoke.verify(level,DIRECTORY);TungstenSmoke.verify(level,DIRECTORY);StainlessSmoke.verify(level,DIRECTORY);StainlessGearSmoke.verify(level,DIRECTORY);StainlessArmorSmoke.verify(level,DIRECTORY);LeadSmoke.verify(level,DIRECTORY);FoolsDiamondSmoke.verify(level,DIRECTORY);PreciousSmoke.verify(level,DIRECTORY);SilverGearSmoke.verify(level,DIRECTORY);SilverArmorSmoke.verify(level,DIRECTORY);PlatinumAlloySmoke.verify(level,DIRECTORY);PlatinumGearSmoke.verify(level,DIRECTORY);PlatinumArmorSmoke.verify(level,DIRECTORY);TitaniumSmoke.verify(level,DIRECTORY);TitaniumGearSmoke.verify(level,DIRECTORY);TitaniumArmorSmoke.verify(level,DIRECTORY);MagnetiteSmoke.verify(level,DIRECTORY);ResoniteSmoke.verify(level,DIRECTORY);UraniumSmoke.verify(level,DIRECTORY);BismuthSmoke.verify(level,DIRECTORY);descriptors(level);legacyGeometry();persistentEdits(level);naturalTin(level);
                Files.writeString(DIRECTORY.resolve("result.txt"),"PASS native metallurgy recipes, drops, tool gates, stack/profile codecs, old readers, frozen geometry and persisted edits\n");
            }
        }catch(Throwable error){Blockbranch.LOGGER.error("METALLURGY_FAIL",error);try{Files.writeString(DIRECTORY.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static void recipes(ServerLevel level){
        var manager=level.getServer().getRecipeManager();
        var alloy=CraftingInput.of(2,2,List.of(new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),new ItemStack(MetallurgyContent.TIN_INGOT)));
        var recipe=manager.getRecipeFor(RecipeType.CRAFTING,alloy,level).orElseThrow();var output=recipe.value().assemble(alloy);
        require(output.is(MetallurgyContent.BRONZE_INGOT)&&output.getCount()==4,"three copper + one tin conserves four alloy ingots");
        var missing=CraftingInput.of(2,2,List.of(new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.COPPER_INGOT),ItemStack.EMPTY,new ItemStack(MetallurgyContent.TIN_INGOT)));
        require(manager.getRecipeFor(RecipeType.CRAFTING,missing,level).isEmpty(),"alloy cannot omit copper");
        var block=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(i->new ItemStack(MetallurgyContent.BRONZE_INGOT)).toList());
        require(manager.getRecipeFor(RecipeType.CRAFTING,block,level).orElseThrow().value().assemble(block).is(MetallurgyContent.BRONZE_BLOCK.asItem()),"bronze storage crafting");
        var unpack=CraftingInput.of(1,1,List.of(new ItemStack(MetallurgyContent.BRONZE_BLOCK)));
        var ingots=manager.getRecipeFor(RecipeType.CRAFTING,unpack,level).orElseThrow().value().assemble(unpack);
        require(ingots.is(MetallurgyContent.BRONZE_INGOT)&&ingots.getCount()==9,"storage unpacking conserves material");
        var pick=CraftingInput.of(3,3,List.of(new ItemStack(MetallurgyContent.BRONZE_INGOT),new ItemStack(MetallurgyContent.BRONZE_INGOT),new ItemStack(MetallurgyContent.BRONZE_INGOT),ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY));
        require(manager.getRecipeFor(RecipeType.CRAFTING,pick,level).orElseThrow().value().assemble(pick).is(MetallurgyContent.BRONZE_PICKAXE),"native pickaxe recipe");
        for(ItemLike material:List.of(MetallurgyContent.RAW_TIN,MetallurgyContent.TIN_ORE,MetallurgyContent.DEEPSLATE_TIN_ORE)){
            var input=new SingleRecipeInput(new ItemStack(material));
            require(manager.getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow().value().assemble(input).is(MetallurgyContent.TIN_INGOT),"native furnace tin processing");
            require(manager.getRecipeFor(RecipeType.BLASTING,input,level).orElseThrow().value().assemble(input).is(MetallurgyContent.TIN_INGOT),"native blast furnace tin processing");
        }
    }
    private static void unlocks(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MetallurgyTester");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        player.getAdvancements();
        for(ItemLike material:List.of(MetallurgyContent.RAW_TIN,MetallurgyContent.TIN_ORE,MetallurgyContent.DEEPSLATE_TIN_ORE,MetallurgyContent.TIN_INGOT,MetallurgyContent.BRONZE_INGOT,MetallurgyContent.BRONZE_BLOCK,ZincBrassContent.RAW_ZINC,ZincBrassContent.ZINC_ORE,ZincBrassContent.DEEPSLATE_ZINC_ORE,ZincBrassContent.ZINC_INGOT,ZincBrassContent.BRASS_INGOT,ZincBrassContent.BRASS_BLOCK,SteelContent.RAW_MANGANESE,SteelContent.MANGANESE_ORE,SteelContent.DEEPSLATE_MANGANESE_ORE,SteelContent.MANGANESE_INGOT,SteelContent.STEEL_INGOT,SteelContent.STEEL_BLOCK,TungstenContent.RAW_TUNGSTEN,TungstenContent.TUNGSTEN_ORE,TungstenContent.DEEPSLATE_TUNGSTEN_ORE,TungstenContent.TUNGSTEN_INGOT,TungstenContent.TUNGSTEN_STEEL_INGOT,StainlessContent.RAW_NICKEL,StainlessContent.NICKEL_ORE,StainlessContent.DEEPSLATE_NICKEL_ORE,StainlessContent.NICKEL_INGOT,StainlessContent.RAW_CHROMIUM,StainlessContent.CHROMIUM_ORE,StainlessContent.DEEPSLATE_CHROMIUM_ORE,StainlessContent.CHROMIUM_INGOT,StainlessContent.STAINLESS_STEEL_INGOT,StainlessContent.STAINLESS_STEEL_BLOCK,LeadContent.RAW_LEAD,LeadContent.LEAD_ORE,LeadContent.DEEPSLATE_LEAD_ORE,LeadContent.LEAD_INGOT,LeadContent.LEAD_ALLOY_INGOT,LeadContent.LEAD_BLOCK,LeadContent.LEAD_ALLOY_BLOCK,PreciousContent.RAW_SILVER,PreciousContent.RAW_PLATINUM,PreciousContent.SILVER_ORE,PreciousContent.DEEPSLATE_SILVER_ORE,PreciousContent.PLATINUM_ORE,PreciousContent.DEEPSLATE_PLATINUM_ORE,PreciousContent.SILVER_INGOT,PreciousContent.PLATINUM_INGOT,PreciousContent.SILVER_BLOCK,PreciousContent.PLATINUM_BLOCK,PlatinumAlloyContent.INGOT,PlatinumAlloyContent.BLOCK,TitaniumContent.RAW,TitaniumContent.ORE,TitaniumContent.DEEPSLATE_ORE,TitaniumContent.INGOT,TitaniumContent.ALLOY_INGOT,TitaniumContent.ALLOY_BLOCK,MagnetiteContent.FRAGMENT,ResoniteContent.CRYSTAL,ResoniteContent.BLOCK,UraniumContent.RAW,UraniumContent.ORE,UraniumContent.DEEPSLATE_ORE,UraniumContent.INGOT,UraniumContent.TIP,BismuthContent.RAW,BismuthContent.ORE,BismuthContent.DEEPSLATE_ORE,BismuthContent.INGOT)){
            var stack=new ItemStack(material);player.getInventory().setItem(0,stack);
            net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(player,player.getInventory(),stack);
        }
        var names=new ArrayList<>(List.of("bronze_ingot","bronze_block","bronze_ingot_from_block","bronze_pickaxe","bronze_sword","bronze_axe","bronze_shovel","bronze_hoe","bronze_helmet","bronze_chestplate","bronze_leggings","bronze_boots"));
        for(String material:List.of("raw_tin","tin_ore","deepslate_tin_ore"))for(String method:List.of("smelting","blasting"))names.add("tin_ingot_from_"+method+"_"+material);
        names.addAll(List.of("brass_ingot","brass_block","brass_ingot_from_block","brass_slab","brass_stairs","brass_bars"));
        for(String material:List.of("raw_zinc","zinc_ore","deepslate_zinc_ore"))for(String method:List.of("smelting","blasting"))names.add("zinc_ingot_from_"+method+"_"+material);
        names.addAll(List.of("steel_ingot","steel_block","steel_ingot_from_block","steel_pickaxe","steel_sword","steel_axe","steel_shovel","steel_hoe","steel_helmet","steel_chestplate","steel_leggings","steel_boots"));
        for(String material:List.of("raw_manganese","manganese_ore","deepslate_manganese_ore"))for(String method:List.of("smelting","blasting"))names.add("manganese_ingot_from_"+method+"_"+material);
        names.addAll(List.of("tungsten_steel_ingot","tungsten_steel_pickaxe","tungsten_steel_axe","tungsten_steel_shovel","tungsten_steel_hoe"));
        for(String material:List.of("raw_tungsten","tungsten_ore","deepslate_tungsten_ore"))for(String method:List.of("smelting","blasting"))names.add("tungsten_ingot_from_"+method+"_"+material);
        names.addAll(List.of("stainless_steel_ingot","stainless_steel_block","stainless_steel_ingot_from_block","stainless_steel_slab","stainless_steel_stairs","stainless_steel_pickaxe"));
        for(String metal:List.of("nickel","chromium"))for(String material:List.of("raw_"+metal,metal+"_ore","deepslate_"+metal+"_ore"))for(String method:List.of("smelting","blasting"))names.add(metal+"_ingot_from_"+method+"_"+material);
        names.addAll(List.of("stainless_steel_sword","stainless_steel_axe","stainless_steel_shovel","stainless_steel_hoe","stainless_steel_helmet","stainless_steel_chestplate","stainless_steel_leggings","stainless_steel_boots"));
        names.add("lead_alloy_ingot");for(String metal:List.of("lead","lead_alloy"))for(String suffix:List.of("block","ingot_from_block","slab","stairs"))names.add(metal+"_"+suffix);
        for(String material:List.of("raw_lead","lead_ore","deepslate_lead_ore"))for(String method:List.of("smelting","blasting"))names.add("lead_ingot_from_"+method+"_"+material);
        for(String metal:List.of("silver","platinum")){for(String suffix:List.of("block","ingot_from_block","slab","stairs"))names.add(metal+"_"+suffix);for(String material:List.of("raw_"+metal,metal+"_ore","deepslate_"+metal+"_ore"))for(String method:List.of("smelting","blasting"))names.add(metal+"_ingot_from_"+method+"_"+material);}names.addAll(List.of("silver_pickaxe","silver_sword","silver_axe","silver_shovel","silver_hoe","silver_helmet","silver_chestplate","silver_leggings","silver_boots"));
        names.addAll(List.of("platinum_alloy_ingot","platinum_alloy_block","platinum_alloy_ingot_from_block","platinum_alloy_pickaxe"));
        names.addAll(List.of("platinum_alloy_sword","platinum_alloy_axe","platinum_alloy_shovel","platinum_alloy_hoe","platinum_alloy_helmet","platinum_alloy_chestplate","platinum_alloy_leggings","platinum_alloy_boots"));
        names.addAll(List.of("titanium_alloy_ingot","titanium_alloy_block","titanium_alloy_ingot_from_block","titanium_alloy_boots"));for(String material:List.of("raw_titanium","titanium_ore","deepslate_titanium_ore"))for(String method:List.of("smelting","blasting"))names.add("titanium_ingot_from_"+method+"_"+material);
        names.addAll(List.of("titanium_alloy_pickaxe","titanium_alloy_sword","titanium_alloy_axe","titanium_alloy_shovel","titanium_alloy_hoe","titanium_alloy_helmet","titanium_alloy_chestplate","titanium_alloy_leggings"));
        names.add("magnetite_boots");
        names.addAll(List.of("resonite_block","resonite_crystals"));
        names.addAll(List.of("uranium_tips","uranium_netherite_sword","uranium_netherite_axe","uranium_netherite_spear"));for(String material:List.of("raw_uranium","uranium_ore","deepslate_uranium_ore"))for(String method:List.of("smelting","blasting"))names.add("uranium_ingot_from_"+method+"_"+material);
        names.add("bismuth_steel_pickaxe");for(String material:List.of("raw_bismuth","bismuth_ore","deepslate_bismuth_ore"))for(String method:List.of("smelting","blasting"))names.add("bismuth_ingot_from_"+method+"_"+material);
        for(String name:names)require(player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE,Blockbranch.id(name))),"ingredient pickup unlocks "+name);
    }
    private static void dropsAndTools(ServerLevel level){
        var pick=new ItemStack(MetallurgyContent.BRONZE_PICKAXE);
        require(pick.getMaxDamage()==220&&pick.isValidRepairItem(new ItemStack(MetallurgyContent.BRONZE_INGOT))&&!pick.isValidRepairItem(new ItemStack(MetallurgyContent.TIN_INGOT)),"durability and bronze-only repair");
        require(!pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())&&new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),"iron remains required for iron-tier ores");
        var silk=pick.copy();silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        for(var block:List.of(MetallurgyContent.TIN_ORE,MetallurgyContent.DEEPSLATE_TIN_ORE)){
            var state=block.defaultBlockState();require(pick.isCorrectToolForDrops(state)&&!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state),"stone/copper-tier tin harvest gate");
            var ordinary=Block.getDrops(state,level,new BlockPos(0,-60,0),null,null,pick);
            require(ordinary.size()==1&&ordinary.getFirst().is(MetallurgyContent.RAW_TIN)&&ordinary.getFirst().getCount()==1,"native tin raw drop");
            var intact=Block.getDrops(state,level,new BlockPos(0,-60,0),null,null,silk);
            require(intact.size()==1&&intact.getFirst().is(block.asItem()),"native silk touch ore drop");
        }
        var blockDrops=Block.getDrops(MetallurgyContent.BRONZE_BLOCK.defaultBlockState(),level,new BlockPos(0,-60,0),null,null,pick);
        require(blockDrops.size()==1&&blockDrops.getFirst().is(MetallurgyContent.BRONZE_BLOCK.asItem()),"bronze block drops itself");
        pick.setDamageValue(37);var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());
        var copy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,pick).getOrThrow()).getOrThrow();
        require(copy.is(MetallurgyContent.BRONZE_PICKAXE)&&copy.getDamageValue()==37,"native stack codec retains tool damage");
    }
    private static void bronzeTools(ServerLevel level)throws Exception{
        var tools=List.of(MetallurgyContent.BRONZE_SWORD,MetallurgyContent.BRONZE_AXE,MetallurgyContent.BRONZE_SHOVEL,MetallurgyContent.BRONZE_HOE);
        var tags=List.of(net.minecraft.tags.ItemTags.SWORDS,net.minecraft.tags.ItemTags.AXES,net.minecraft.tags.ItemTags.SHOVELS,net.minecraft.tags.ItemTags.HOES);
        String[] names={"sword","axe","shovel","hoe"};String[][] patterns={{"B","B","S"},{"BB","BS"," S"},{"B","S","S"},{"BB"," S"," S"}};
        var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var saved=DIRECTORY.resolve("bronze-tools.dat");
        var previous=Files.exists(saved)?net.minecraft.nbt.NbtIo.readCompressed(saved,net.minecraft.nbt.NbtAccounter.unlimitedHeap()):null;
        var next=new net.minecraft.nbt.CompoundTag();
        for(int i=0;i<tools.size();i++){
            var cells=new ArrayList<ItemStack>();for(String row:patterns[i])for(char ch:row.toCharArray())cells.add(ch=='B'?new ItemStack(MetallurgyContent.BRONZE_INGOT):ch=='S'?new ItemStack(Items.STICK):ItemStack.EMPTY);
            var input=CraftingInput.of(patterns[i][0].length(),patterns[i].length,cells);
            var output=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(output.is(tools.get(i))&&output.is(tags.get(i)),"native bronze "+names[i]+" recipe and tool/enchantment tag");
            require(output.getMaxDamage()==220&&output.isValidRepairItem(new ItemStack(MetallurgyContent.BRONZE_INGOT))&&!output.isValidRepairItem(new ItemStack(Items.COPPER_INGOT)),"shared bronze durability/repair");
            if(previous!=null){var restored=ItemStack.CODEC.parse(ops,previous.getCompoundOrEmpty(names[i])).getOrThrow();require(restored.is(tools.get(i))&&restored.getDamageValue()==37,"saved tool identity and damage survive relaunch");}
            output.setDamageValue(37);next.put(names[i],ItemStack.CODEC.encodeStart(ops,output).getOrThrow());
        }
        net.minecraft.nbt.NbtIo.writeCompressed(next,saved);
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BronzeActions");
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
        var sword=new ItemStack(MetallurgyContent.BRONZE_SWORD);player.getInventory().setItem(0,sword);
        var target=net.minecraft.world.entity.EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        require(target!=null&&sword.hurtEnemy(target,player),"native sword weapon component");sword.postHurtEnemy(target,player);require(sword.getDamageValue()==1,"native sword hit durability");
        var attribute=net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;var slot=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        double copper=new ItemStack(Items.COPPER_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double bronze=sword.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double iron=new ItemStack(Items.IRON_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        require(copper<bronze&&bronze<iron,"bronze sword strength stays between copper and iron");
        Blockbranch.LOGGER.info("BRONZE_TOOLS native recipes/actions/negative controls/sword durability and saved-stack reload={}",previous!=null);
    }
    private static void descriptors(ServerLevel level)throws Exception{
        var factory=level.palettedContainerFactory();int[] ground=new int[256],water=new int[256];Arrays.fill(ground,64);Arrays.fill(water,Integer.MIN_VALUE);
        var biomes=new PalettedContainer<>(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS),factory.biomeStrategy());
        var base=new TerrainProfile(TerrainShape.FLOOR,TerrainShape.CEILING,ground,water,biomes,factory.blockStatesStrategy());
        var layer=new OreLayer(424242,0,0,3,65535,65535);var profile=base.withLayers(UndergroundLayer.NONE,layer);
        var encoded=profile.encode(factory);require(encoded.getIntOr("version",0)==63,"proposal profile format retains a versioned ore descriptor");
        var copy=TerrainProfile.decode(encoded,factory);int count=0;
        for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int y=-160;y<61;y++){
            var state=profile.state(x,y,z);require(copy.state(x,y,z)==state,"native descriptor/profile roundtrip");
            if(state.is(MetallurgyContent.TIN_ORE)||state.is(MetallurgyContent.DEEPSLATE_TIN_ORE))count++;
        }
        require(count>0,"tin exists in bounded native profile");
        for(var host:List.of(Blocks.AIR,Blocks.WATER,Blocks.DIRT,Blocks.GRASS_BLOCK,Blocks.TUFF,Blocks.BEDROCK))require(layer.state(3,8,3,64,host.defaultBlockState())==host.defaultBlockState(),"tin never replaces non-host material");
        var invalid=layer.encode();invalid.putInt("definition",OreLayer.DEFINITION+1);try{OreLayer.decode(invalid);throw new AssertionError("unknown descriptor accepted");}catch(IllegalArgumentException expected){}
        try(var reader=new java.io.InputStreamReader(MetallurgySmoke.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json"))){
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var generator=RealisticGenerator.CODEC.codec().parse(ops,json).getOrThrow();require(generator.oreDefinition()==OreLayer.DEFINITION,"fresh preset selects current ore descriptor");
            var legacy=json.deepCopy();legacy.addProperty("ore_definition",2);require(RealisticGenerator.CODEC.codec().parse(ops,legacy).getOrThrow().oreDefinition()==2,"saved definition 2 is not promoted");
        }
        Blockbranch.LOGGER.info("METALLURGY profile tinCells={} snapshot=true",count);
    }
    private static void legacyGeometry()throws Exception{
        var url=Path.of(System.getProperty("blockbranch.oldOreClasses")).toUri().toURL();
        try(var loader=historicalReader(url);var catalogLoader=historicalReader(Path.of(System.getProperty("blockbranch.oldCatalogClasses")).toUri().toURL());var toolCatalogLoader=historicalReader(Path.of(System.getProperty("blockbranch.oldToolCatalogClasses")).toUri().toURL())){
            var type=loader.loadClass("blockbranch.realistic.OreLayer");var constructor=type.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);
            var state=type.getMethod("state",int.class,int.class,int.class,int.class,BlockState.class,boolean.class);var bind=type.getMethod("bind",int.class,int.class);long checked=0;int tin=0;
            for(long seed:new long[]{424242,1,-17})for(int definition:new int[]{1,2})for(int cx:new int[]{-1,0,1}){
                var old=bind.invoke(constructor.newInstance(seed,cx,-1,definition,65535,65535),64,64);
                var current=new OreLayer(seed,cx,-1,definition,65535,65535).bind(64,64);var added=new OreLayer(seed,cx,-1,3,65535,65535).bind(64,64);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){
                    int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=(BlockState)state.invoke(old,x,y,z,64,host,false);
                    require(current.state(x,y,z,64,host,false)==expected,"frozen definitions 1/2 remain exact");checked++;
                    if(definition==2){var next=added.state(x,y,z,64,host,false);if(expected!=host)require(next==expected,"tin cannot replace existing ores");else if(next!=host){require(next.is(MetallurgyContent.TIN_ORE)||next.is(MetallurgyContent.DEEPSLATE_TIN_ORE),"only tin is added");tin++;}}
                }
            }
            try{type.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,3,0,0).encode());throw new AssertionError("old reader accepted new descriptor");}
            catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"old reader safely rejects unsupported descriptor");}
            var oldGuard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class);
            var catalogGuard=catalogLoader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            var toolCatalogGuard=toolCatalogLoader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            for(boolean moss:new boolean[]{false,true}){
                var root=Files.createTempDirectory(DIRECTORY,"marker-");var sentinel=root.resolve("sentinel.dat");Files.writeString(sentinel,"player data remains untouched");
                ColumnWorldFormat.check(root,false,moss);ColumnWorldFormat.check(root,true,moss,true);
                String marker=Files.readString(root.resolve("blockbranch-columns.txt"));require(marker.contains("blockbranch-columns=4")&&marker.contains("versioned-decoration="+moss),"catalog marker preserves decoration barrier");
                try{oldGuard.invoke(null,root,true,moss);throw new AssertionError("old guard accepted new catalog");}
                catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"old guard safely refuses new catalog");}
                try{catalogGuard.invoke(null,root,true,moss,true);throw new AssertionError("catalog1 reader accepted catalog2");}
                catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior tin build safely rejects new tools");}
                try{toolCatalogGuard.invoke(null,root,true,moss,true);throw new AssertionError("catalog2 reader accepted armor catalog3");}
                catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior tools build safely rejects armor");}
                require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker)&&Files.readString(sentinel).equals("player data remains untouched"),"rejected older reader leaves files unchanged");
                ColumnWorldFormat.check(root,true,moss,true);
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=1"));
                ColumnWorldFormat.check(root,true,moss,true);
                require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog1 upgrades without losing decoration flag");
                String future=marker.replace("metallurgy-catalog=20","metallurgy-catalog=21");Files.writeString(root.resolve("blockbranch-columns.txt"),future);
                try{ColumnWorldFormat.check(root,true,moss,true);throw new AssertionError("future catalog accepted");}catch(IllegalStateException expected){}
                require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(future),"future catalog rejection preserves marker");Files.writeString(root.resolve("blockbranch-columns.txt"),marker);
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=2"));ColumnWorldFormat.check(root,true,moss,true);
                require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog2 upgrade preserves decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=3"));ColumnWorldFormat.check(root,true,moss,true);
                require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog3 upgrade preserves decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=4"));ColumnWorldFormat.check(root,true,moss,true);
                require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog4 upgrade preserves decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=5"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog5 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=6"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog6 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=7"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog7 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=8"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog8 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=9"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog9 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=10"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog10 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=11"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog11 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=12"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog12 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=13"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog13 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=14"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog14 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=15"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog15 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=16"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog16 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=17"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog17 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=18"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog18 upgrade retains decoration flag");
                Files.writeString(root.resolve("blockbranch-columns.txt"),marker.replace("metallurgy-catalog=20","metallurgy-catalog=19"));ColumnWorldFormat.check(root,true,moss,true);require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(marker),"catalog19 upgrade retains decoration flag");
                for(String malformed:List.of(marker+"extra=true\n",marker.replace("metallurgy-catalog=20","metallurgy-catalog=0"),marker.replace("22,20,22","22,19,22"),marker.replace("versioned-decoration=","versioned-decoration=unknown"))){Files.writeString(root.resolve("blockbranch-columns.txt"),malformed);try{ColumnWorldFormat.check(root,true,moss,true);throw new AssertionError("noncanonical marker accepted");}catch(IllegalStateException expected){}require(Files.readString(root.resolve("blockbranch-columns.txt")).equals(malformed),"malformed marker rejection preserves data");}Files.writeString(root.resolve("blockbranch-columns.txt"),marker);
                if(moss){try{ColumnWorldFormat.check(root,true,false,true);throw new AssertionError("lost decoration settings accepted");}catch(IllegalStateException expected){}}
            }
            require(tin>0,"append-only tin comparison has positive witnesses");Blockbranch.LOGGER.info("METALLURGY frozenComparisons={} tinWitnesses={} oldReaderRejects3=true",checked,tin);
        }
    }
    private static void persistentEdits(ServerLevel level)throws Exception{
        var marker=DIRECTORY.resolve("edits.txt");int y;
        if(Files.exists(marker)){
            y=Integer.parseInt(Files.readString(marker).trim());
            require(level.getBlockState(new BlockPos(32,y,0)).is(MetallurgyContent.TIN_ORE),"placed tin survives full reopen");
            require(level.getBlockState(new BlockPos(33,y,0)).is(MetallurgyContent.BRONZE_BLOCK),"bronze block survives full reopen");
            require(level.getBlockState(new BlockPos(34,y,0)).isAir(),"mined edit survives full reopen");
        }else{
            y=level.getHeight(Heightmap.Types.WORLD_SURFACE,32,0)+4;
            level.setBlock(new BlockPos(32,y,0),MetallurgyContent.TIN_ORE.defaultBlockState(),2);
            level.setBlock(new BlockPos(33,y,0),MetallurgyContent.BRONZE_BLOCK.defaultBlockState(),2);
            level.setBlock(new BlockPos(34,y,0),MetallurgyContent.TIN_ORE.defaultBlockState(),2);level.setBlock(new BlockPos(34,y,0),Blocks.AIR.defaultBlockState(),2);
            Files.writeString(marker,Integer.toString(y));
        }
        level.getServer().saveEverything(false,true,true);
    }
    private static void naturalTin(ServerLevel level)throws Exception{
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator))return;
        ColumnSaveSafetySmoke.verifyStartupMarker(level,generator);
        require(generator.oreDefinition()>=3,"actual new world keeps metallurgy definition");
        var marker=DIRECTORY.resolve("natural-mined.txt");
        if(Files.exists(marker)){
            var xyz=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            require(level.getBlockState(pos).isAir(),"naturally generated tin mining survives full reopen");
            var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();
            require(base.state(pos.getX(),pos.getY(),pos.getZ()).is(MetallurgyContent.TIN_ORE)||base.state(pos.getX(),pos.getY(),pos.getZ()).is(MetallurgyContent.DEEPSLATE_TIN_ORE),"saved procedural base retains the original vein under mining edit");
            return;
        }
        var origin=generator.getOrigin(level.getChunkSource().randomState());
        for(int dz=0;dz<=1;dz++)for(int dx=0;dx<=1;dx++){
            var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            require(base.ores().definition()==generator.oreDefinition(),"actual FULL chunk carries its saved generator ore definition");
            var snapshot=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=TinVeins.MIN_DEPTH;depth<=TinVeins.MAX_DEPTH;depth++){
                int y=base.groundHeight(x,z)-depth;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);var state=base.state(x,y,z);
                if(!state.is(MetallurgyContent.TIN_ORE)&&!state.is(MetallurgyContent.DEEPSLATE_TIN_ORE))continue;
                require(level.getBlockState(pos)==state&&snapshot.state(x,y,z)==state,"actual native vein agrees with saved procedural profile");
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());
                level.getServer().saveEverything(false,true,true);Blockbranch.LOGGER.info("METALLURGY actual naturally generated tin witness={}",pos);return;
            }
        }
        throw new AssertionError("No natural tin witness in four actual native columns");
    }
    static URLClassLoader historicalReader(URL url){return new URLClassLoader(new URL[]{url},MetallurgySmoke.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(!name.startsWith("blockbranch.realistic.OreLayer")&&!name.startsWith("blockbranch.cubic.ColumnWorldFormat")&&!name.startsWith("blockbranch.metallurgy.TinVeins")&&!name.startsWith("blockbranch.metallurgy.ZincVeins")&&!name.startsWith("blockbranch.metallurgy.ManganeseVeins")&&!name.startsWith("blockbranch.metallurgy.TungstenVeins")&&!name.startsWith("blockbranch.metallurgy.NickelVeins")&&!name.startsWith("blockbranch.metallurgy.ChromiumVeins")&&!name.startsWith("blockbranch.metallurgy.LeadVeins")&&!name.startsWith("blockbranch.metallurgy.FoolsDiamondVeins")&&!name.startsWith("blockbranch.metallurgy.SilverVeins")&&!name.startsWith("blockbranch.metallurgy.PlatinumVeins")&&!name.startsWith("blockbranch.metallurgy.TitaniumVeins")&&!name.startsWith("blockbranch.metallurgy.MagnetiteVeins")&&!name.startsWith("blockbranch.metallurgy.ResoniteVeins")&&!name.startsWith("blockbranch.metallurgy.UraniumVeins"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        };}
    private static void require(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    private MetallurgySmoke(){}
}
