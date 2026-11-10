package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Opt-in observation only: follows the same ground-visible native plants through fixed camera views. */
final class FlowerVisibilityDiagnostic {
    private record Plant(BlockPos pos,String type){}
    private final List<Plant> plants=new ArrayList<>();
    private final List<Map<String,Object>> views=new ArrayList<>();
    private record Projection(double x,double y,double w) {boolean visible(){return w>0&&x>=0&&x<1024&&y>=0&&y<576;}}
    private static Projection project(Matrix4f matrix,Vec3 camera,Vec3 point){
        var p=new Vector4f((float)(point.x-camera.x),(float)(point.y-camera.y),(float)(point.z-camera.z),1);matrix.transform(p);
        return new Projection((p.x/p.w+1)*512,(1-p.y/p.w)*288,p.w);
    }
    void sample(Minecraft client,BlockPos bed,int stage)throws Exception{
        var camera=client.gameRenderer.mainCamera();var render=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        var matrix=camera.getViewRotationProjectionMatrix(new Matrix4f());var origin=camera.position();
        if(origin.distanceToSqr(client.player.getEyePosition())>.01||Math.abs(net.minecraft.util.Mth.wrapDegrees(camera.yRot()-client.player.getYRot()))>.05||Math.abs(camera.xRot()-client.player.getXRot())>.05)throw new AssertionError("Actual renderer camera differs from stable requested player camera");
        if(stage==2){
            for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++)for(int x=bed.getX()-64;x<=bed.getX()+64;x++){
                var base=((SparseColumn)client.level.getChunk(x>>4,z>>4)).blockbranch$terrain().base();
                int y=base.top(s->s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.PODZOL)||s.is(Blocks.COARSE_DIRT)||s.is(Blocks.ROOTED_DIRT),x,z,ColumnLimits.MAX_Y-1);
                var pos=new BlockPos(x,y,z);var state=client.level.getBlockState(pos);String type=state.is(BlockTags.SMALL_FLOWERS)?"flower":state.is(Blocks.SHORT_GRASS)?"grass":null;
                if(type==null)continue;var center=Vec3.atLowerCornerOf(pos).add(.5,.4,.5);
                if(project(matrix,origin,center).visible()&&clear(client,origin,center,pos))plants.add(new Plant(pos,type));
            }
            if(plants.stream().filter(p->p.type.equals("flower")).count()<10)throw new AssertionError("No substantial ground-visible flower witness");
        }
        var samples=new ArrayList<Map<String,Object>>();int flowers=0,onScreen=0,unoccluded=0,meshReady=0;double span=0;
        for(var plant:plants){
            var p=plant.pos;var center=Vec3.atLowerCornerOf(p).add(.5,.4,.5);var projected=project(matrix,origin,center);
            var bottom=project(matrix,origin,center.add(0,-.4,0));var top=project(matrix,origin,center.add(0,.35,0));double size=Math.hypot(top.x-bottom.x,top.y-bottom.y);
            var state=client.level.getBlockState(p);boolean present=plant.type.equals("flower")?state.is(BlockTags.SMALL_FLOWERS):state.is(Blocks.SHORT_GRASS);if(!present)throw new AssertionError("Tracked native plant changed between captures: "+p);
            var section=client.levelRenderer.viewArea().getRenderSectionAt(p);boolean compiled=section!=null&&section.getSectionNode()==SectionPos.asLong(p)&&section.getSectionMesh()!=CompiledSectionMesh.UNCOMPILED&&!section.getSectionMesh().isEmpty(ChunkSectionLayer.CUTOUT);
            boolean rayClear=clear(client,origin,center,p);
            var row=new LinkedHashMap<String,Object>();row.put("x",p.getX());row.put("y",p.getY());row.put("z",p.getZ());row.put("type",plant.type);row.put("state",state.toString());row.put("distance",origin.distanceTo(center));row.put("screenX",projected.x);row.put("screenY",projected.y);row.put("on_screen",projected.visible());row.put("solid_block_ray_clear",rayClear);row.put("cutout_section_ready",compiled);row.put("projected_vertical_075m_span_pixels",size);samples.add(row);
            if(plant.type.equals("flower")){flowers++;if(projected.visible()){onScreen++;if(rayClear){unoccluded++;span+=size;if(compiled)meshReady++;}}}
        }
        var view=new LinkedHashMap<String,Object>();view.put("stage",stage);view.put("camera",origin.toString());view.put("yaw",camera.yRot());view.put("pitch",camera.xRot());view.put("render_camera",render.pos.toString());view.put("render_yaw",render.yRot);view.put("render_pitch",render.xRot);view.put("fov",camera.getFov());view.put("tracked_ground_visible_flowers",flowers);view.put("on_screen_flowers",onScreen);view.put("unoccluded_on_screen_flowers",unoccluded);view.put("unoccluded_cutout_ready",meshReady);view.put("mean_projected_vertical_075m_span_pixels",unoccluded==0?0:span/unoccluded);view.put("plants",samples);views.add(view);
        Files.writeString(client.gameDirectory.toPath().resolve("flower-visibility.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views)+"\n");
        Blockbranch.LOGGER.info("FLOWER_VISIBILITY stage={} camera={} yaw={} pitch={} renderCamera={} renderYaw={} renderPitch={} fov={} tracked={} onScreen={} clear={} cutoutReady={} mean075mSpan={}",stage,origin,camera.yRot(),camera.xRot(),render.pos,render.yRot,render.xRot,camera.getFov(),flowers,onScreen,unoccluded,meshReady,unoccluded==0?0:span/unoccluded);
    }
    private static boolean clear(Minecraft client,Vec3 from,Vec3 to,BlockPos plant){
        var hit=client.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,client.player));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(plant);
    }
}
