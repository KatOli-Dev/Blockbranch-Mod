package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Runs in an isolated Minecraft classpath without a server or game. Loads installed vanilla assets,
 * never redistributes them. Optional argument: native piece-tag JSON export for the actual site. */
public final class MansionRoofPlanCheck {
    private static final Map<String,CompoundTag> ASSETS=new HashMap<>();
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static CompoundTag asset(String name) {
        return ASSETS.computeIfAbsent(name,n->{
            try(var stream=MansionRoofPlanCheck.class.getResourceAsStream("/data/minecraft/structure/woodland_mansion/"+n+".nbt")) {
                if(stream==null)throw new AssertionError("Missing native asset "+n);
                return NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        });
    }
    private static MansionRoofPlan.Source source(String name,BlockPos p,Mirror m,Rotation r) {
        return new MansionRoofPlan.Source(name,p,m,r,BlockPos.ZERO,asset(name));
    }
    private static Map<BlockPos,BlockState> paint(MansionRoofPlan plan,BoundingBox bounds,boolean reverse) {
        var chunks=new ArrayList<BoundingBox>();
        for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)chunks.add(new BoundingBox(x*16,bounds.minY(),z*16,x*16+15,bounds.maxY(),z*16+15));
        if(reverse)Collections.reverse(chunks);var result=new HashMap<BlockPos,BlockState>();
        for(var clip:chunks) {
            if(reverse)plan=MansionRoofPlan.load(plan.save(),bounds);
            plan.place(clip,(p,s)->{check(bounds.isInside(p)&&clip.isInside(p),"Bounds/clip");check(result.put(p,s)==null,"Duplicate write");});
        }
        return result;
    }
    private static void verify(MansionRoofPlan plan,BoundingBox bounds) { verify(plan,bounds,true); }
    private static void verify(MansionRoofPlan plan,BoundingBox bounds,boolean replay) {
        check(plan.save().equals(MansionRoofPlan.load(plan.save(),bounds).save()),"Canonical NBT round trip");
        if(replay)check(paint(plan,bounds,false).equals(paint(plan,bounds,true)),"Reverse clip/cold plan reload");
        var protection=plan.save().getIntArray("Protected").orElseThrow();var writes=paint(plan,bounds,false);
        for(int i=0;i<protection.length;i+=3)check(!writes.containsKey(new BlockPos(protection[i],protection[i+1],protection[i+2])),"Nonroof/marker protection");
        // A genuine bearing must be unchanged and every new solid has a continuous vertical stack
        // to one. Partial slab/stair UP faces count as positive bearing area, not a full opaque cube.
        var bases=new HashMap<String,Integer>();var rows=plan.save().getIntArray("Roof").orElseThrow();
        for(int i=0;i<rows.length;i+=5)if((rows[i+4]&1)!=0)bases.merge(rows[i]+","+rows[i+2]+","+rows[i+3],rows[i+1],Math::min);
        for(int i=0;i<rows.length;i+=5)if((rows[i+4]&1)!=0&&rows[i+1]==bases.get(rows[i]+","+rows[i+2]+","+rows[i+3]))check(!writes.containsKey(new BlockPos(rows[i],rows[i+1],rows[i+2])),"Original bearing kept");
        var quality=halfStepSurfaces(rows,writes);
        check(quality.get("maxSurfaceHalf")<=2L*bounds.maxY()+1,"Surface remains within original maxY+0.5 allowance");
        var byColumn=new HashMap<String,List<Integer>>();
        for(var base:bases.entrySet()){String[] coordinates=base.getKey().split(",");byColumn.computeIfAbsent(coordinates[0]+","+coordinates[1],k->new ArrayList<>()).add(base.getValue());}
        for(var e:writes.entrySet())if(!e.getValue().isAir()) {
            boolean found=false;
            for(int base:byColumn.getOrDefault(e.getKey().getX()+","+e.getKey().getZ(),List.of())) {
                if(base>=e.getKey().getY())continue;
                boolean supported=true;for(int y=base+1;y<e.getKey().getY();y++)if(writes.getOrDefault(new BlockPos(e.getKey().getX(),y,e.getKey().getZ()),Blocks.AIR.defaultBlockState()).isAir())supported=false;
                found|=supported;
            }
            check(found,"Unbroken bearing under new roof "+e.getKey());
        }
    }
    private record RoofColumn(int x,int z,int level) {
        RoofColumn offset(Direction d){return new RoofColumn(x+d.getStepX(),z+d.getStepZ(),level);}
    }
    private static Map<String,Long> halfStepSurfaces(int[] rows,Map<BlockPos,BlockState> writes) {
        var bases=new HashMap<RoofColumn,Integer>();
        for(int i=0;i<rows.length;i+=5)if((rows[i+4]&1)!=0)bases.merge(new RoofColumn(rows[i],rows[i+2],rows[i+3]),rows[i+1],Math::min);
        var surfaces=new HashMap<RoofColumn,Integer>();long slabs=0,full=0;
        for(var e:bases.entrySet()) {
            var column=e.getKey();int surface=Integer.MIN_VALUE;
            for(int y=e.getValue()+1;y<=column.level+9;y++) {
                var state=writes.get(new BlockPos(column.x,y,column.z));if(state==null||state.isAir())continue;
                check(state.is(Blocks.SPRUCE_PLANKS)||state.is(Blocks.SPRUCE_SLAB)&&state.getValue(SlabBlock.TYPE)==SlabType.BOTTOM,"Roof surface has no isolated stair ridge or floating top slab");
                surface=Math.max(surface,2*y+(state.is(Blocks.SPRUCE_SLAB)?1:2));
            }
            if(surface!=Integer.MIN_VALUE){surfaces.put(column,surface);if((surface&1)==0)full++;else slabs++;}
        }
        long rises=0,flat=0;
        for(var e:surfaces.entrySet())for(var direction:List.of(Direction.EAST,Direction.SOUTH)) {
            Integer neighbor=surfaces.get(e.getKey().offset(direction));if(neighbor==null)continue;
            int rise=Math.abs(e.getValue()-neighbor);check(rise<=1,"Neighboring active surfaces rise at most half a block "+e.getKey()+" delta="+rise);
            if(rise==1)rises++;else flat++;
        }
        return Map.of("halfStepEdges",rises,"flatEdges",flat,"slabSurfaces",slabs,"fullSurfaces",full,"maxSurfaceHalf",(long)surfaces.values().stream().mapToInt(Integer::intValue).max().orElse(Integer.MIN_VALUE));
    }
    /** Constructor intentionally unused: this focused fixture needs only getOrCreate. */
    public static final class Templates extends StructureTemplateManager {
        private Templates(){super(null,null,null,null);}
        @Override public StructureTemplate getOrCreate(Identifier id) {
            var template=new StructureTemplate();template.load(BuiltInRegistries.BLOCK,asset(id.getPath().substring(id.getPath().lastIndexOf('/')+1)));return template;
        }
    }
    private static Templates templates()throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return (Templates)((sun.misc.Unsafe)field.get(null)).allocateInstance(Templates.class);
    }
    private static List<MansionRoofPlan.Source> snapshots(List<WoodlandMansionPieces.WoodlandMansionPiece> pieces) {
        var result=new ArrayList<MansionRoofPlan.Source>();
        for(var piece:pieces) {
            var tag=piece.createTag(null);var settings=piece.placeSettings();
            result.add(new MansionRoofPlan.Source(tag.getString("Template").orElseThrow(),piece.templatePosition(),settings.getMirror(),settings.getRotation(),settings.getRotationPivot(),piece.template().save(new CompoundTag())));
        }
        return result;
    }
    private static BoundingBox bounds(List<WoodlandMansionPieces.WoodlandMansionPiece> pieces) {
        var core=BoundingBox.encapsulatingBoxes(pieces.stream().map(StructurePiece::getBoundingBox).toList()).orElseThrow();
        return new BoundingBox(core.minX()-21,core.minY()-12,core.minZ()-15,core.maxX()+21,core.maxY()+6,core.maxZ()+15);
    }
    private static void expectInvalid(CompoundTag tag,BoundingBox bounds) {
        try{MansionRoofPlan.load(tag,bounds);throw new AssertionError("Malformed roof NBT accepted");}catch(IllegalArgumentException expected){}
    }
    public static void main(String[] args)throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        int variants=0;
        var simpleBounds=new BoundingBox(-40,-12,-40,40,40,40);
        for(var mirror:Mirror.values())for(var rotation:Rotation.values()) {
            var source=new ArrayList<MansionRoofPlan.Source>();
            for(int x=0;x<3;x++)for(int z=0;z<2;z++)source.add(source("roof",new BlockPos(x*8,13,z*8),mirror,rotation));
            var plan=MansionRoofPlan.captureSources(source,simpleBounds);check(plan.commandCount()>0,"Supported roof raises");verify(plan,simpleBounds);variants++;
            for(String role:List.of("roof_front","roof_corner","roof_inner_corner")) {
                var edge=MansionRoofPlan.captureSources(List.of(source(role,new BlockPos(0,10,0),mirror,rotation)),simpleBounds);
                check(edge.commandCount()>0,"Native edge roof raises "+role);verify(edge,simpleBounds);variants++;
            }
        }
        var mutableOrigin=new BlockPos.MutableBlockPos(0,13,0);var mutableTemplate=asset("roof").copy();
        var immutable=MansionRoofPlan.captureSources(List.of(new MansionRoofPlan.Source("roof",mutableOrigin,Mirror.NONE,Rotation.NONE,BlockPos.ZERO,mutableTemplate)),simpleBounds);
        var immutableTag=immutable.save();mutableOrigin.move(1000,0,0);mutableTemplate.remove("blocks");
        check(immutableTag.equals(immutable.save()),"Captured positions and template inputs are immutable");
        var single=List.of(source("roof",new BlockPos(0,13,0),Mirror.NONE,Rotation.NONE));
        var basic=MansionRoofPlan.captureSources(single,simpleBounds);var valid=basic.save();
        for(Tag wrong:List.of(ByteTag.valueOf((byte)1),LongTag.valueOf(1),FloatTag.valueOf(1),StringTag.valueOf("1"),IntTag.valueOf(0),IntTag.valueOf(1),IntTag.valueOf(3))) {var tag=valid.copy();tag.put("Version",wrong);expectInvalid(tag,simpleBounds);}
        for(String field:List.of("Bounds","Roof","Protected")) {var tag=valid.copy();tag.putString(field,"bad");expectInvalid(tag,simpleBounds);tag=valid.copy();tag.remove(field);expectInvalid(tag,simpleBounds);}
        var tag=valid.copy();tag.putIntArray("Roof",new int[65536*5+5]);expectInvalid(tag,simpleBounds);
        tag=valid.copy();tag.putIntArray("Roof",new int[]{0,13,0,10,4});expectInvalid(tag,simpleBounds);
        tag=valid.copy();tag.putIntArray("Roof",new int[]{1000,13,0,10,3});expectInvalid(tag,simpleBounds);
        tag=valid.copy();tag.putIntArray("Roof",new int[]{0,13,0,10,3,0,13,0,10,3});expectInvalid(tag,simpleBounds);
        tag=valid.copy();tag.putIntArray("Roof",new int[]{0,13,0,8,3});expectInvalid(tag,simpleBounds);
        tag=valid.copy();tag.putIntArray("Protected",new int[]{1000,15,0});expectInvalid(tag,simpleBounds);
        expectInvalid(valid,new BoundingBox(-39,-12,-40,40,40,40));
        // Remove ownership of one bearing. That column stays entirely native; neighbors remain supported.
        var noBearing=valid.copy();var rows=noBearing.getIntArray("Roof").orElseThrow();rows[4]=5;noBearing.putIntArray("Roof",rows);
        var partial=MansionRoofPlan.load(noBearing,simpleBounds);
        check(paint(partial,simpleBounds,false).keySet().stream().noneMatch(p->p.getX()==rows[0]&&p.getZ()==rows[2]),"Partial bearing column remains native");
        check(partial.raisedColumns()==63,"Other independently supported columns remain usable");
        var noUpBearing=valid.copy();var supportRows=noUpBearing.getIntArray("Roof").orElseThrow();supportRows[4]&=3;noUpBearing.putIntArray("Roof",supportRows);
        check(MansionRoofPlan.load(noUpBearing,simpleBounds).raisedColumns()==63,"No positive UP bearing leaves native column intact");
        var obstruction=valid.copy();obstruction.putIntArray("Protected",new int[]{3,14,3});
        var blocked=MansionRoofPlan.load(obstruction,simpleBounds);check(!paint(blocked,simpleBounds,false).containsKey(new BlockPos(3,14,3)),"Protected room AIR fallback");
        check(blocked.commandCount()>0,"Isolated protected AIR does not cancel supported roof range");
        // A sparse roof source has one bearing, not its declared 8x8 size: absent cells never become AIR.
        var sparse=asset("roof").copy();var sparseBlocks=new ListTag();sparseBlocks.add(sparse.getListOrEmpty("blocks").get(0).copy());sparse.put("blocks",sparseBlocks);
        var sparsePlan=MansionRoofPlan.captureSources(List.of(new MansionRoofPlan.Source("roof",new BlockPos(0,13,0),Mirror.NONE,Rotation.NONE,BlockPos.ZERO,sparse)),simpleBounds);
        check(paint(sparsePlan,simpleBounds,false).keySet().stream().allMatch(p->p.getX()==0&&p.getZ()==0),"Sparse absent cells untouched");
        var templates=templates();
        if(args.length>0) {
            var pieces=new ArrayList<WoodlandMansionPieces.WoodlandMansionPiece>();
            var tags=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
            for(var element:tags){var nativeTag=TagParser.parseCompoundFully(element.getAsString());if(nativeTag.getStringOr("id","").equals("minecraft:wmp"))pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(templates,nativeTag));}
            var bounds=bounds(pieces);var plan=MansionRoofPlan.captureSources(snapshots(pieces),bounds);verify(plan,bounds);
            var painted=paint(plan,bounds,false);long solid=painted.values().stream().filter(s->!s.isAir()).count();long columns=painted.entrySet().stream().filter(e->!e.getValue().isAir()).map(e->e.getKey().getX()+","+e.getKey().getZ()).distinct().count();
            NbtIo.writeCompressed(plan.save(),Path.of("build/roof-plan-check/actual-plan.nbt"));
            System.out.println("ACTUAL surfaces="+halfStepSurfaces(plan.save().getIntArray("Roof").orElseThrow(),painted));
            System.out.println("ACTUAL nativePieces="+pieces.size()+" raised="+plan.raisedComponents()+" skipped="+plan.skippedComponents()+" commands="+plan.commandCount()+" solid="+solid+" raisedColumns="+columns+" maxY="+painted.keySet().stream().mapToInt(BlockPos::getY).max().orElse(-1)+" savedBytes="+Files.size(Path.of("build/roof-plan-check/actual-plan.nbt")));
        }
        int layouts=0,raised=0,skipped=0,writes=0;
        for(int seed=0;seed<Integer.getInteger("blockbranch.roofCheckSeeds",8);seed++)for(var rotation:Rotation.values()) {
            var pieces=new ArrayList<WoodlandMansionPieces.WoodlandMansionPiece>();
            WoodlandMansionPieces.generateMansion(templates,new BlockPos(0,10,0),rotation,pieces,RandomSource.create(seed));
            var bounds=bounds(pieces);var plan=MansionRoofPlan.captureSources(snapshots(pieces),bounds);verify(plan,bounds,seed==0&&rotation==Rotation.NONE);
            layouts++;raised+=plan.raisedComponents();skipped+=plan.skippedComponents();writes+=plan.commandCount();
            System.out.println("LAYOUT seed="+seed+" rotation="+rotation+" pieces="+pieces.size()+" raised="+plan.raisedComponents()+" skipped="+plan.skippedComponents()+" commands="+plan.commandCount());
        }

        System.out.println("PASS transforms="+variants+" nativeLayouts="+layouts+" raised="+raised+" skipped="+skipped+" commands="+writes+" malformed/support/sparse/clip/reload controls");
    }
}
