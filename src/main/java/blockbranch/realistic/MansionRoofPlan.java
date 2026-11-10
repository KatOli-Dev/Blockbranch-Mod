package blockbranch.realistic;

import java.util.*;
import blockbranch.mixin.cubic.ReimaginedTemplateName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Immutable definition-2 roof commands. Capture runs once before placement; replay never reads the world.
 * Native templates and their processors still place normally before this plan. Only final roof-owned
 * cells above an original bearing are cleared. Explicit native non-roof AIR is protected like stone.
 */
public final class MansionRoofPlan {
    private static final int VERSION=2, MAX_CELLS=65536, MAX_PROTECTED=131072, MAX_COMMANDS=131072;
    private static final Comparator<BlockPos> ORDER=Comparator.<BlockPos>comparingInt(BlockPos::getX)
        .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);
    private static final Set<String> ROLES=Set.of("roof","roof_front","roof_corner","roof_inner_corner");
    private static final Direction[] SIDES={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    private final BoundingBox bounds;
    private final List<Cell> cells;
    private final Set<BlockPos> protectedCells;
    private final List<Command> commands;
    private final int raisedComponents, skippedComponents, raisedColumns;

    @FunctionalInterface public interface Writer { void set(BlockPos position,BlockState state); }
    private record Cell(BlockPos position,int level,boolean solid,boolean owned,boolean bearing) {}
    private record Column(int x,int z) {
        Column offset(Direction d){return new Column(x+d.getStepX(),z+d.getStepZ());}
        BlockPos at(int y){return new BlockPos(x,y,z);}
    }
    private record Command(BlockPos position,int state) {}
    /** Serialized template snapshot for focused generation checks; no template assets are packaged. */
    static record Source(String name,BlockPos origin,Mirror mirror,Rotation rotation,BlockPos pivot,CompoundTag template) {}

    private MansionRoofPlan(BoundingBox bounds,List<Cell> cells,Set<BlockPos> protectedCells) {
        this.bounds=copy(bounds);this.cells=List.copyOf(cells);this.protectedCells=Set.copyOf(protectedCells);
        var result=build();commands=result.commands;raisedComponents=result.raised;skippedComponents=result.skipped;raisedColumns=result.columns;
    }
    public static MansionRoofPlan capture(List<? extends TemplateStructurePiece> pieces,BoundingBox bounds) {
        var sources=new ArrayList<Source>();
        for(var piece:pieces) {
            var settings=piece.placeSettings();
            sources.add(new Source(((ReimaginedTemplateName)piece).blockbranch$reimaginedTemplate(),
                piece.templatePosition(),settings.getMirror(),settings.getRotation(),settings.getRotationPivot(),
                piece.template().save(new CompoundTag())));
        }
        return captureSources(sources,bounds);
    }
    static MansionRoofPlan captureSources(List<Source> sources,BoundingBox bounds) {
        validateBounds(bounds);
        var roof=new HashMap<BlockPos,Cell>();
        var lastRoof=new HashSet<BlockPos>();
        var protectedCells=new HashSet<BlockPos>();
        for(var source:sources) {
            String name=source.name.substring(source.name.lastIndexOf('/')+1);
            boolean isRoof=ROLES.contains(name);
            int level=source.origin.getY()-(name.equals("roof")?3:0);
            var tag=source.template;
            if(tag.contains("palettes"))throw invalid("Multiple palettes are not supported");
            var palette=tag.getList("palette").orElseThrow(()->invalid("Missing palette"));
            if(palette.isEmpty())throw invalid("Empty palette");
            var solidStates=new boolean[palette.size()];var bearingStates=new boolean[palette.size()];
            for(int index=0;index<palette.size();index++) {
                var entry=palette.getCompound(index).orElseThrow(()->invalid("Invalid palette entry"));
                var state=NbtUtils.readBlockState(BuiltInRegistries.BLOCK,entry);
                solidStates[index]=!state.isAir();
                bearingStates[index]=!state.getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO).getFaceShape(Direction.UP).isEmpty();
            }
            for(var entry:tag.getList("blocks").orElseThrow(()->invalid("Missing template blocks"))) {
                if(!(entry instanceof CompoundTag block))throw invalid("Invalid template block");
                var coordinates=block.getList("pos").orElseThrow(()->invalid("Missing template position"));
                if(coordinates.size()!=3)throw invalid("Invalid template position");
                var local=new BlockPos(coordinates.getInt(0).orElseThrow(),coordinates.getInt(1).orElseThrow(),coordinates.getInt(2).orElseThrow());
                var position=source.origin.offset(StructureTemplate.transform(local,source.mirror,source.rotation,source.pivot)).immutable();
                int state=block.getInt("state").orElseThrow(()->invalid("Missing template state"));
                if(state<0||state>=palette.size())throw invalid("Invalid template palette index");
                var stateTag=palette.getCompound(state).orElseThrow(()->invalid("Invalid palette entry"));
                String id=stateTag.getString("id").orElseThrow(()->invalid("Missing palette id"));
                // The native STRUCTURE_BLOCK ignore processor does not claim a world write. Its marker
                // still owns this position for gameplay; later roof cells may never replace it.
                boolean marker=id.equals("minecraft:structure_block");
                if(marker||block.contains("nbt"))protectedCells.add(position);
                if(marker)continue;
                if(isRoof) {
                    if(!bounds.isInside(position))throw invalid("Native roof outside marker bounds");
                    if(local.getY()<0||local.getY()>3)throw invalid("Unexpected roof height");
                    boolean solid=solidStates[state];
                    roof.put(position,new Cell(position,level,solid,true,bearingStates[state]));lastRoof.add(position);
                } else {
                    protectedCells.add(position);lastRoof.remove(position);
                }
            }
        }
        if(roof.size()>MAX_CELLS)throw invalid("Roof cell limit");
        var cells=new ArrayList<Cell>();
        for(var cell:roof.values())cells.add(new Cell(cell.position,cell.level,cell.solid,lastRoof.contains(cell.position),cell.bearing));
        cells.sort(Comparator.comparing(Cell::position,ORDER));
        // Only positions which can meet a roof command are needed in the saved protection certificate.
        var columns=new HashSet<Column>();int minimum=Integer.MAX_VALUE;
        for(var cell:cells){columns.add(new Column(cell.position.getX(),cell.position.getZ()));minimum=Math.min(minimum,cell.level);}
        final int minY=minimum;
        protectedCells.removeIf(p->p.getY()<minY||!bounds.isInside(p)||!columns.contains(new Column(p.getX(),p.getZ())));
        if(protectedCells.size()>MAX_PROTECTED)throw invalid("Protection cell limit");
        return new MansionRoofPlan(bounds,cells,protectedCells);
    }
    public CompoundTag save() {
        var tag=new CompoundTag();tag.putInt("Version",VERSION);
        tag.putIntArray("Bounds",new int[]{bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()});
        int[] rows=new int[cells.size()*5];int i=0;
        for(var cell:cells){rows[i++]=cell.position.getX();rows[i++]=cell.position.getY();rows[i++]=cell.position.getZ();rows[i++]=cell.level;rows[i++]=(cell.solid?1:0)|(cell.owned?2:0)|(cell.bearing?4:0);}
        tag.putIntArray("Roof",rows);
        var protection=new ArrayList<>(protectedCells);protection.sort(ORDER);int[] protectedRows=new int[protection.size()*3];i=0;
        for(var position:protection){protectedRows[i++]=position.getX();protectedRows[i++]=position.getY();protectedRows[i++]=position.getZ();}
        tag.putIntArray("Protected",protectedRows);return tag;
    }
    public static MansionRoofPlan load(CompoundTag tag,BoundingBox bounds) {
        validateBounds(bounds);
        if(!(tag.get("Version") instanceof IntTag)||tag.getIntOr("Version",-1)!=VERSION)throw invalid("Unknown roof plan version");
        if(!tag.keySet().equals(Set.of("Version","Bounds","Roof","Protected")))throw invalid("Unknown roof plan fields");
        int[] box=array(tag,"Bounds",6,6),rows=array(tag,"Roof",5,MAX_CELLS*5),protection=array(tag,"Protected",3,MAX_PROTECTED*3);
        if(!Arrays.equals(box,new int[]{bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()}))throw invalid("Roof plan bounds differ");
        var cells=new ArrayList<Cell>();BlockPos previous=null;var columns=new HashSet<Column>();int minimum=Integer.MAX_VALUE;
        for(int i=0;i<rows.length;i+=5) {
            var position=new BlockPos(rows[i],rows[i+1],rows[i+2]);int level=rows[i+3],flags=rows[i+4];
            if(!bounds.isInside(position)||flags<0||flags>7||(flags&4)!=0&&(flags&1)==0||level<bounds.minY()||level>bounds.maxY()-3||position.getY()<level||position.getY()>level+3||previous!=null&&ORDER.compare(previous,position)>=0)throw invalid("Invalid roof source row");
            cells.add(new Cell(position,level,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0));previous=position;
            columns.add(new Column(position.getX(),position.getZ()));minimum=Math.min(minimum,level);
        }
        var protectedCells=new HashSet<BlockPos>();previous=null;
        for(int i=0;i<protection.length;i+=3) {
            var position=new BlockPos(protection[i],protection[i+1],protection[i+2]);
            if(!bounds.isInside(position)||position.getY()<minimum||!columns.contains(new Column(position.getX(),position.getZ()))||previous!=null&&ORDER.compare(previous,position)>=0)throw invalid("Invalid roof protection row");
            protectedCells.add(position);previous=position;
        }
        return new MansionRoofPlan(bounds,cells,protectedCells);
    }
    public void place(BoundingBox clip,Writer writer) {
        for(var command:commands)if(clip.isInside(command.position))writer.set(command.position,state(command.state));
    }
    public int commandCount(){return commands.size();}
    public int raisedComponents(){return raisedComponents;}
    public int skippedComponents(){return skippedComponents;}
    public int raisedColumns(){return raisedColumns;}

    private record Built(List<Command> commands,int raised,int skipped,int columns) {}
    private Built build() {
        var levels=new TreeMap<Integer,Map<Column,List<Cell>>>();
        for(var cell:cells)levels.computeIfAbsent(cell.level,k->new HashMap<>())
            .computeIfAbsent(new Column(cell.position.getX(),cell.position.getZ()),k->new ArrayList<>()).add(cell);
        var output=new TreeMap<BlockPos,Integer>(ORDER);int raised=0,skipped=0,raisedColumns=0;
        for(var level:levels.entrySet()) {
            var bearing=new HashMap<Column,Integer>();
            for(var column:level.getValue().entrySet())for(var cell:column.getValue())if(cell.solid)bearing.merge(column.getKey(),cell.position.getY(),Math::min);
            var remaining=new HashSet<>(bearing.keySet());
            while(!remaining.isEmpty()) {
                var first=remaining.iterator().next();remaining.remove(first);var component=new HashSet<Column>();component.add(first);
                var queue=new ArrayDeque<Column>();queue.add(first);
                while(!queue.isEmpty()) {
                    var column=queue.remove();
                    for(var direction:SIDES){var next=column.offset(direction);if(remaining.remove(next)){component.add(next);queue.add(next);}}
                }
                var distance=new HashMap<Column,Integer>();queue.clear();
                for(var column:component)for(var direction:SIDES)if(!component.contains(column.offset(direction))){distance.put(column,1);queue.add(column);break;}
                while(!queue.isEmpty()) {
                    var column=queue.remove();int nextDistance=distance.get(column)+1;
                    for(var direction:SIDES){var next=column.offset(direction);if(component.contains(next)&&!distance.containsKey(next)){distance.put(next,nextDistance);queue.add(next);}}
                }
                int maximumDepth=distance.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
                var caps=new HashMap<Column,Integer>();var minimum=new HashMap<Column,Integer>();
                var active=new HashSet<Column>();
                for(var column:component) {
                    int base=bearing.get(column),maximumY=Math.min(bounds.maxY(),level.getKey()+9);
                    int cap=2*maximumY+1,originalTop=base;
                    for(var cell:level.getValue().get(column))if(cell.solid&&cell.owned)originalTop=Math.max(originalTop,cell.position.getY());
                    boolean ownedBase=level.getValue().get(column).stream().anyMatch(c->c.position.getY()==base&&c.owned&&c.solid&&c.bearing);
                    // A protected cell can be touched at its lower face, never written. Heights are
                    // surface coordinates in half blocks, not the block coordinates of a stair top.
                    for(int y=base+1;y<=maximumY;y++)if(protectedCells.contains(column.at(y))){cap=2*y;break;}
                    if(ownedBase){active.add(column);minimum.put(column,2*base+3);}
                    else cap=2*(originalTop+1);
                    caps.put(column,cap);
                }
                record Height(Column column,int value) {}
                // The greatest admissible half-step field below every protected/native cap.
                var capQueue=new PriorityQueue<Height>(Comparator.comparingInt(Height::value));
                for(var column:component)capQueue.add(new Height(column,caps.get(column)));
                while(!capQueue.isEmpty()) {
                    var cap=capQueue.remove();if(cap.value!=caps.get(cap.column))continue;
                    for(var direction:SIDES) {
                        var neighbor=cap.column.offset(direction);
                        if(component.contains(neighbor)&&caps.get(neighbor)>cap.value+1) {
                            caps.put(neighbor,cap.value+1);capQueue.add(new Height(neighbor,cap.value+1));
                        }
                    }
                }
                active.removeIf(column->caps.get(column)<minimum.get(column));
                // Raise the lower envelope around unchanged roof-deck bearings before shaping it.
                // This avoids flattening or dropping an inner column merely to meet a lower eave.
                var lower=new HashMap<Column,Integer>();
                var lowerQueue=new PriorityQueue<Height>(Comparator.comparingInt(Height::value).reversed());
                for(var column:active){lower.put(column,minimum.get(column));lowerQueue.add(new Height(column,minimum.get(column)));}
                while(!lowerQueue.isEmpty()) {
                    var height=lowerQueue.remove();if(height.value!=lower.get(height.column))continue;
                    for(var direction:SIDES) {
                        var neighbor=height.column.offset(direction);
                        if(active.contains(neighbor)&&lower.get(neighbor)<height.value-1) {
                            lower.put(neighbor,height.value-1);lowerQueue.add(new Height(neighbor,height.value-1));
                        }
                    }
                }
                var surfaces=new HashMap<Column,Integer>();
                for(var column:active) {
                    int depth=distance.get(column);
                    // Two half-block eave rises meet the retained deck. The main hip then advances
                    // through half-block contours, up to the same maxY+0.5 surface.
                    // Narrow ranges use a lower ridge rather than violating that half-step slope.
                    int profile=depth<=3?6+depth:9+Math.min(10,maximumDepth-3)*(depth-3)/(maximumDepth-3);
                    surfaces.put(column,Math.min(caps.get(column),Math.max(2*level.getKey()+profile,lower.get(column))));
                }
                if(active.isEmpty()){skipped++;continue;}
                var candidate=new TreeMap<BlockPos,Integer>(ORDER);
                for(var column:component)if(active.contains(column)) {
                    int base=bearing.get(column),surface=surfaces.get(column),top=Math.floorDiv(surface-1,2);
                    for(var cell:level.getValue().get(column))if(cell.owned&&cell.position.getY()>base&&!protectedCells.contains(cell.position))candidate.put(cell.position,0);
                    // Solid fill meets the original bearing in every column. Only the exposed top is
                    // shaped, so there is no hollow membrane depending on a neighboring chunk.
                    for(int y=base+1;y<top;y++)candidate.put(column.at(y),1);
                    // A full-height surface ends in a full block; a half-height surface ends in a
                    // supported bottom slab. Isolated stair strips would add a second local ridge.
                    int shape=(surface&1)==0?1:2;
                    candidate.put(column.at(top),shape);
                }
                for(var command:candidate.entrySet()) {
                    if(!bounds.isInside(command.getKey())||protectedCells.contains(command.getKey()))throw invalid("Unsafe roof command");
                    Integer old=output.putIfAbsent(command.getKey(),command.getValue());
                    if(old!=null&&!old.equals(command.getValue()))throw invalid("Roof levels overlap");
                }
                raised++;raisedColumns+=active.size();
            }
        }
        if(output.size()>MAX_COMMANDS)throw invalid("Roof command limit");
        var commands=new ArrayList<Command>();output.forEach((position,state)->commands.add(new Command(position,state)));
        return new Built(List.copyOf(commands),raised,skipped,raisedColumns);
    }
    private static BlockState state(int state) {
        return switch(state) {
            case 0->Blocks.AIR.defaultBlockState();case 1->Blocks.SPRUCE_PLANKS.defaultBlockState();case 2->Blocks.SPRUCE_SLAB.defaultBlockState();
            default->throw invalid("Unknown roof command");
        };
    }
    private static int[] array(CompoundTag tag,String name,int stride,int maximum) {
        if(!(tag.get(name) instanceof IntArrayTag))throw invalid("Invalid "+name+" type");
        int[] value=tag.getIntArray(name).orElseThrow();if(value.length>maximum||value.length%stride!=0)throw invalid("Invalid "+name+" length");return value;
    }
    private static BoundingBox copy(BoundingBox b){return new BoundingBox(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ());}
    private static void validateBounds(BoundingBox b) {
        if((long)b.maxX()-b.minX()>192||(long)b.maxZ()-b.minZ()>192||(long)b.maxY()-b.minY()>256
            ||Math.abs((long)b.minX())>30000000||Math.abs((long)b.maxX())>30000000||Math.abs((long)b.minZ())>30000000||Math.abs((long)b.maxZ())>30000000
            ||b.minY()< -1000000||b.maxY()>1000000)throw invalid("Invalid roof bounds");
    }
    private static IllegalArgumentException invalid(String message){return new IllegalArgumentException(message);}
}
