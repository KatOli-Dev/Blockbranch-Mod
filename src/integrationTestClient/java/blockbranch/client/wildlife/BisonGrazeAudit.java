package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.AmericanBison;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Grass clearance measured from the transformed snout polygons. */
final class BisonGrazeAudit {
    static void verify(){
        var root=BisonModel.layer().bakeRoot();var model=new BisonModel(root);var state=new BisonRenderer.State();int poses=0;
        for(boolean baby:new boolean[]{false,true})for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<128;phase++){
            state.isBaby=baby;state.activity=1;state.yRot=yaw;state.ageInTicks=(float)(phase*Math.PI*2/(128*.2));model.setupAnim(state);
            assertGrazing(root);poses++;
        }
        root.getChild("head").y-=2;
        if(muzzleBottom(root)>=23.5F)throw new AssertionError("Grazing audit missed raised snout");
        for(int activity:new int[]{0,2}){
            state.activity=1;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            var reference=BisonModel.layer().bakeRoot();new BisonModel(reference).setupAnim(state);
            for(String name:new String[]{"head","shoulder","rump","front_left","front_right","back_left","back_right"})
                if(!root.getChild(name).storePose().equals(reference.getChild(name).storePose()))throw new AssertionError("Grazing transform leaked into activity "+activity);
        }
        Blockbranch.LOGGER.info("BISON_GRAZE_AUDIT PASS poses={} muzzleNearGrass=true headBeardAboveGrass=true rearHeadInsideShoulder=true reset=true raisedSnoutDetected=true",poses);
    }
    private static void assertGrazing(ModelPart root){
        float bottom=muzzleBottom(root);
        if(bottom<23.5F||bottom>24)throw new AssertionError("Grazing snout must meet grass without burial: "+bottom);
        float[] shoulder={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        Vector3f[] joint={null};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(path.equals("/head")&&index==0)joint[0]=pose.pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,(cube.minY+cube.maxY)/32,cube.maxZ/16)).mul(16);
            if(!path.startsWith("/head")&&!(path.equals("/shoulder")&&index==0))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(path.startsWith("/head")){if(p.y>24.0001F)throw new AssertionError("Grazing head/beard buried: "+path);}
                else for(int axis=0;axis<3;axis++){shoulder[axis]=Math.min(shoulder[axis],p.get(axis));shoulder[axis+3]=Math.max(shoulder[axis+3],p.get(axis));}
            }
        });
        if(joint[0]==null)throw new AssertionError("Head joint required");
        for(int axis=0;axis<3;axis++)if(joint[0].get(axis)<shoulder[axis]||joint[0].get(axis)>shoulder[axis+3])throw new AssertionError("Grazing head detached from shoulder");
    }
    static float muzzleBottom(ModelPart root){
        float[] bottom={Float.NEGATIVE_INFINITY};int[] count={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/head")||index!=1)return;
            count[0]++;
            for(var face:cube.polygons)for(var vertex:face.vertices())bottom[0]=Math.max(bottom[0],pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16);
        });
        if(count[0]!=1||!Float.isFinite(bottom[0]))throw new AssertionError("Finite bison snout required");
        return bottom[0];
    }
    static void observe(Minecraft client,UUID id){
        AmericanBison subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof AmericanBison bison&&bison.getUUID().equals(id))subject=bison;
        if(subject==null||!subject.isAlive()||subject.isInWater())throw new AssertionError("Live land bison required");
        var renderer=(BisonRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
        var root=BisonModel.layer().bakeRoot();new BisonModel(root).setupAnim(state);
        if(state.activity==1)assertGrazing(root);
        Blockbranch.LOGGER.info("BISON_GRAZE_KEYFRAME baby={} activity={} muzzleBottomModelY={} gapModelUnits={}",state.isBaby,state.activity,muzzleBottom(root),24-muzzleBottom(root));
    }
    private BisonGrazeAudit(){}
}
