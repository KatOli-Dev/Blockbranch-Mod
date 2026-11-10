package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Independent fixed-site oracle: installed native assets plus authentic saved piece order.
 * Never invokes the replacement processor. Roof commands are a separately frozen reviewed blueprint.
 */
public final class MansionNativeOracle {
    public record Original(BlockState state,String collision,String fluid,int blockLight,int skyLight) {}
    private record Ownership(String role,BlockPos local,Mirror mirror,Rotation rotation,BlockState original,boolean nbt) {}
    private record RoofCell(BlockPos pos,int level,boolean solid,boolean bearing) {}
    private record XZ(int x,int z){XZ next(Direction d){return new XZ(x+d.getStepX(),z+d.getStepZ());}BlockPos at(int y){return new BlockPos(x,y,z);}}
    private static final Set<String> ROOFS=Set.of("roof","roof_front","roof_corner","roof_inner_corner");
    private static final Direction[] SIDES={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    private static final Map<String,BlockState> STATES=new HashMap<>();
    public final Map<BlockPos,Original> baseline=new HashMap<>();
    public final Map<BlockPos,BlockState> expected=new HashMap<>(),roofCommands=new HashMap<>();
    public final Set<BlockPos> nativeProtection=new HashSet<>(),nativeWalkFeet=new HashSet<>();
    public final List<CompoundTag> originalNativePieces=new ArrayList<>();
    public final BoundingBox oldBounds,newBounds,core;
    public final int floor;
    public int paletteChanges,raisedColumns,raisedComponents;

    public MansionNativeOracle(Path reference,Path commands)throws Exception {
        var anatomy=JsonParser.parseString(Files.readString(reference.resolve("native-anatomy.json"))).getAsJsonObject();
        oldBounds=box(anatomy.getAsJsonArray("markerBounds"));core=box(anatomy.getAsJsonArray("nativeCoreBounds"));floor=anatomy.get("floor").getAsInt();
        newBounds=new BoundingBox(oldBounds.minX(),oldBounds.minY(),oldBounds.minZ(),oldBounds.maxX(),Math.max(core.maxY()+6,floor+19),oldBounds.maxZ());
        try(var lines=Files.lines(reference.resolve("mansion-site.voxels.jsonl"))){for(String line:(Iterable<String>)lines::iterator){var row=JsonParser.parseString(line).getAsJsonObject();var p=new BlockPos(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt());var old=new Original(state(row.get("state").getAsString()),row.get("collision").getAsString(),row.get("fluid").getAsString(),row.get("blockLight").getAsInt(),row.get("skyLight").getAsInt());baseline.put(p,old);if(core.isInside(p))expected.put(p,old.state);}}
        var owners=new HashMap<BlockPos,Ownership>();var roof=new HashMap<BlockPos,RoofCell>();var roofLast=new HashSet<BlockPos>();var protectedAll=new HashSet<BlockPos>();
        var assets=new HashMap<String,CompoundTag>();
        for(var value:JsonParser.parseString(Files.readString(reference.resolve("native-piece-anatomy.json"))).getAsJsonArray()){
            var row=value.getAsJsonObject();if(!row.get("class").getAsString().equals("WoodlandMansionPiece"))continue;
            var tag=TagParser.parseCompoundFully(row.get("fullNBT").getAsString());originalNativePieces.add(tag);
            String name=tag.getStringOr("Template","");var origin=new BlockPos(tag.getIntOr("TPX",0),tag.getIntOr("TPY",0),tag.getIntOr("TPZ",0));var mirror=Mirror.valueOf(tag.getStringOr("Mi","NONE"));var rotation=Rotation.valueOf(tag.getStringOr("Rot","NONE"));
            var asset=assets.get(name);if(asset==null){try(var in=MansionNativeOracle.class.getResourceAsStream("/data/minecraft/structure/woodland_mansion/"+name+".nbt")){if(in==null)throw new AssertionError("Installed native asset "+name);asset=NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap());assets.put(name,asset);}}
            var palette=new ArrayList<BlockState>();for(var v:asset.getListOrEmpty("palette"))palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK,(CompoundTag)v));
            for(var valueBlock:asset.getListOrEmpty("blocks")){
                var block=(CompoundTag)valueBlock;var a=block.getListOrEmpty("pos");var local=new BlockPos(a.getInt(0).orElseThrow(),a.getInt(1).orElseThrow(),a.getInt(2).orElseThrow());var p=origin.offset(StructureTemplate.transform(local,mirror,rotation,BlockPos.ZERO)).immutable();var old=palette.get(block.getIntOr("state",-1));boolean marker=old.is(Blocks.STRUCTURE_BLOCK),nbt=block.contains("nbt");
                if(marker||nbt)protectedAll.add(p);if(marker)continue;
                owners.put(p,new Ownership(name,local,mirror,rotation,old,nbt));
                if(ROOFS.contains(name)){boolean solid=!old.isAir(),bearing=!old.getCollisionShape(EmptyBlockGetter.INSTANCE,p).getFaceShape(Direction.UP).isEmpty();roof.put(p,new RoofCell(p,origin.getY()-(name.equals("roof")?3:0),solid,bearing));roofLast.add(p);}
                else{protectedAll.add(p);roofLast.remove(p);}
            }
        }
        if(originalNativePieces.size()!=602)throw new AssertionError("Authentic602 native pieces");
        for(var entry:expected.entrySet()){
            var p=entry.getKey();var owner=owners.get(p);var old=entry.getValue();if(owner==null||owner.nbt)continue;
            // A saved opening, service fixture or marker is an authoritative post-template override.
            if(owner.original.getBlock()!=old.getBlock())continue;
            var inverse=switch(owner.rotation){case CLOCKWISE_90->Rotation.COUNTERCLOCKWISE_90;case COUNTERCLOCKWISE_90->Rotation.CLOCKWISE_90;default->owner.rotation;};
            var localState=old.rotate(inverse).mirror(owner.mirror);var mapped=material(owner.role,owner.local,localState,p.getY()-floor).mirror(owner.mirror).rotate(owner.rotation);
            if(mapped!=old){entry.setValue(mapped);paletteChanges++;}
        }
        var columns=new HashSet<XZ>();int minimum=Integer.MAX_VALUE;for(var cell:roof.values()){columns.add(new XZ(cell.pos.getX(),cell.pos.getZ()));minimum=Math.min(minimum,cell.level);}
        for(var p:protectedAll)if(p.getY()>=minimum&&newBounds.isInside(p)&&columns.contains(new XZ(p.getX(),p.getZ())))nativeProtection.add(p);
        declaredRoof(commands);
        for(var e:roofCommands.entrySet())if(core.isInside(e.getKey()))expected.put(e.getKey(),e.getValue());
        for(var e:baseline.entrySet()){var p=e.getKey();if(core.isInside(p)&&protectedAll.contains(p)&&protectedAll.contains(p.above())&&empty(e.getValue())&&empty(baseline.get(p.above()))&&!empty(baseline.get(p.below())))nativeWalkFeet.add(p);}
    }
    private static boolean empty(Original v){return v!=null&&v.collision.equals("[]")&&v.fluid.equals("minecraft:empty/0");}
    private void declaredRoof(Path file)throws Exception {
        String expectedHash=Files.readString(Path.of(file+".sha256")).strip();String actualHash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));if(!actualHash.equals(expectedHash))throw new AssertionError("Frozen reviewed roof commands SHA");
        try(var rows=Files.lines(file)){for(String line:(Iterable<String>)rows::iterator){var r=JsonParser.parseString(line).getAsJsonObject();var p=new BlockPos(r.get("x").getAsInt(),r.get("y").getAsInt(),r.get("z").getAsInt());if(!newBounds.isInside(p)||nativeProtection.contains(p)||roofCommands.put(p,state(r.get("state").getAsString()))!=null)throw new AssertionError("Frozen command outside roof ownership/protection "+p);}}
    }
    /** Literal architecture contract; do not replace with a call to the production mapper. */
    static BlockState material(String name,BlockPos p,BlockState s,int height){
        if(s.isAir()||s.hasBlockEntity()||s.getLightEmission()>0||height<=0||height==8||height==19)return s;
        boolean room=name.startsWith("1x")||name.startsWith("2x");boolean architectural=room||name.startsWith("roof")||name.startsWith("wall_")||name.startsWith("small_wall")||name.startsWith("indoors_")||name.equals("entrance");
        if(architectural){if(s.is(Blocks.COBBLESTONE))return Blocks.STONE_BRICKS.defaultBlockState();if(s.is(Blocks.COBBLESTONE_STAIRS))return Blocks.STONE_BRICK_STAIRS.withPropertiesOf(s);if(s.is(Blocks.COBBLESTONE_WALL))return Blocks.STONE_BRICK_WALL.withPropertiesOf(s);}
        if(ROOFS.contains(name)){if(s.is(Blocks.DARK_OAK_PLANKS))return Blocks.SPRUCE_PLANKS.defaultBlockState();if(s.is(Blocks.DARK_OAK_STAIRS))return Blocks.SPRUCE_STAIRS.withPropertiesOf(s);return s;}
        if(name.equals("wall_window")||name.equals("wall_flat")||name.equals("small_wall")){if(!s.is(Blocks.DARK_OAK_PLANKS))return s;if(height<=2)return Blocks.STONE_BRICKS.defaultBlockState();if(p.getY()==6)return beam(Direction.Axis.Z);if(p.getZ()==0||p.getZ()==7)return beam(Direction.Axis.Y);if(p.getX()==0&&p.getY()>=2&&p.getY()<=5)return Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState();}
        if(name.startsWith("indoors_wall_")||name.startsWith("indoors_door_")){if(!s.is(Blocks.DARK_OAK_PLANKS)||p.getY()==0)return s;if(p.getY()==6)return beam(Direction.Axis.Z);if(p.getZ()==0||p.getZ()==7)return beam(Direction.Axis.Y);if(p.getY()>=2&&p.getY()<=5)return Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState();}
        if(name.equals("entrance")&&s.is(Blocks.DARK_OAK_PLANKS)&&p.getY()>=2&&(p.getX()==0||p.getX()==20||p.getZ()==0||p.getZ()==15)){if(p.getY()==6||p.getY()==14)return beam(p.getZ()==0||p.getZ()==15?Direction.Axis.X:Direction.Axis.Z);return Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState();}return s;
    }
    private static BlockState beam(Direction.Axis axis){return Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis);}
    private static BoundingBox box(JsonArray a){return new BoundingBox(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt(),a.get(3).getAsInt(),a.get(4).getAsInt(),a.get(5).getAsInt());}
    public static BlockState state(String text){return STATES.computeIfAbsent(text,t->{int end=t.indexOf('}');var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(t.substring(6,end)));var state=block.defaultBlockState();if(t.length()>end+1)for(String pair:t.substring(end+2,t.length()-1).split(",")){var kv=pair.split("=");var property=block.getStateDefinition().getProperty(kv[0]);if(property==null)throw new AssertionError(t);state=set(state,property,kv[1]);}return state;});}
    @SuppressWarnings({"rawtypes","unchecked"})private static BlockState set(BlockState state,net.minecraft.world.level.block.state.properties.Property property,String value){return state.setValue(property,(Comparable)property.getValue(value).orElseThrow());}
}
