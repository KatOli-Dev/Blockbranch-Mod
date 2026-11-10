package blockbranch.client.horizon;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
/**
 * Native block-model sprites and tints for horizon faces, so distant cells use the same
 * textures and biome colours as full-detail terrain. Bound to one baked model set; a
 * resource reload creates a new instance. Tinting is confined to the mesher thread.
 */
final class HorizonMaterials {
    /**
     * Atlas rectangle of a sprite, the model's tint index (-1 when untinted) and whether the
     * native chunk layer is cutout, whose transparent texels the shader fills with deeper layers.
     */
    record Face(float u0,float v0,float u1,float v1,int tintIndex,boolean cutout) {}
    record Material(BlockState state,Face top,Face side,Face overlay,float height) {}
    final BlockStateModelSet models;
    private final BlockColors colors;
    private final Face water;
    private final Map<Integer,Material> cache=new ConcurrentHashMap<>();
    private Biome tintBiome;
    private final BlockAndTintGetter tintGetter;

    HorizonMaterials(Minecraft client) {
        models=client.getModelManager().getBlockStateModelSet();colors=client.getBlockColors();
        var fluid=client.getModelManager().getFluidStateModelSet().get(Fluids.WATER.getSource(false));
        var sprite=fluid.stillMaterial().sprite();water=new Face(sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),0,false);
        waterTint=fluid.tintSource();
        tintGetter=(BlockAndTintGetter)Proxy.newProxyInstance(HorizonMaterials.class.getClassLoader(),new Class<?>[]{BlockAndTintGetter.class},(proxy,method,args)->switch(method.getName()) {
            case "getBlockTint"->((ColorResolver)args[1]).getColor(tintBiome,((BlockPos)args[0]).getX(),((BlockPos)args[0]).getZ());
            case "cardinalLighting"->CardinalLighting.DEFAULT;
            case "hashCode"->System.identityHashCode(proxy);
            case "equals"->proxy==args[0];
            case "toString"->"HorizonTintGetter";
            default->defaultValue(method.getReturnType());
        });
    }
    private final net.minecraft.client.color.block.BlockTintSource waterTint;
    Face water(){return water;}

    Material get(int stateId){return cache.computeIfAbsent(stateId,this::resolve);}
    private Material resolve(int stateId) {
        var state=Block.stateById(stateId);var model=models.get(state);var parts=new ArrayList<BlockStateModelPart>();
        model.collectParts(RandomSource.create(42L),parts);
        var particle=model.particleMaterial().sprite();var fallback=new Face(particle.getU0(),particle.getV0(),particle.getU1(),particle.getV1(),-1,state.getBlock() instanceof LeavesBlock);
        var top=quads(parts,Direction.UP);var side=quads(parts,Direction.NORTH);
        float height=1;
        try{var shape=state.getShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);if(!shape.isEmpty())height=(float)Math.max(1/16.,shape.max(Direction.Axis.Y));}catch(RuntimeException ignored){}
        return new Material(state,top.isEmpty()?fallback:face(top.getFirst()),side.isEmpty()?fallback:face(side.getFirst()),side.size()>1?face(side.get(1)):null,height);
    }
    /** Culled and unculled quads facing {@code direction}; unculled ones cover partial-height tops such as snow layers. */
    private static List<BakedQuad> quads(List<BlockStateModelPart> parts,Direction direction) {
        var out=new ArrayList<BakedQuad>();
        for(var p:parts)out.addAll(p.getQuads(direction));
        if(out.isEmpty())for(var p:parts)for(var q:p.getQuads(null))if(q.direction()==direction)out.add(q);
        return out;
    }
    private static Face face(BakedQuad q){var s=q.materialInfo().sprite();return new Face(s.getU0(),s.getV0(),s.getU1(),s.getV1(),q.materialInfo().isTinted()?q.materialInfo().tintIndex():-1,q.materialInfo().layer()==net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);}

    /** Biome colour for a face at (x,z), as 0xRRGGBB; white when untinted. */
    int tint(BlockState state,Face face,Biome biome,int x,int z) {
        if(face.tintIndex<0||biome==null)return 0xFFFFFF;
        tintBiome=biome;
        try {
            var source=face==water?waterTint:colors.getTintSource(state,face.tintIndex);
            return source==null?0xFFFFFF:source.colorInWorld(state,tintGetter,new BlockPos(x,64,z))&0xFFFFFF;
        } catch(RuntimeException unsupported) {
            var source=colors.getTintSource(state,face.tintIndex);return source==null?0xFFFFFF:source.color(state)&0xFFFFFF;
        }
    }
    private static Object defaultValue(Class<?> type) {
        if(!type.isPrimitive())return null;
        if(type==boolean.class)return false;if(type==float.class)return 0f;if(type==double.class)return 0d;if(type==long.class)return 0L;
        if(type==int.class)return 0;if(type==short.class)return (short)0;if(type==byte.class)return (byte)0;if(type==char.class)return (char)0;return null;
    }
}
