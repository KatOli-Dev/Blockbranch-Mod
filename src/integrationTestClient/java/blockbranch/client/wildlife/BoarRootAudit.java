package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.WildBoar;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Soil clearance measured from the rendered snout polygons, in model units. */
final class BoarRootAudit {
    static void verify(){
        var root=BoarModel.layer().bakeRoot();var model=new BoarModel(root);var state=new BoarRenderer.State();int poses=0;
        for(boolean baby:new boolean[]{false,true})for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<128;phase++){
            state.isBaby=baby;state.activity=1;state.yRot=yaw;state.ageInTicks=(float)(phase*Math.PI*2/(128*.3));model.setupAnim(state);
            assertRooting(root);poses++;
        }
        root.getChild("head").y-=3.75F;
        if(muzzleBottom(root)>=23.4F)throw new AssertionError("Rooting audit missed raised snout");
        for(int activity:new int[]{0,2}){
            state.activity=1;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            var reference=BoarModel.layer().bakeRoot();new BoarModel(reference).setupAnim(state);
            for(String name:new String[]{"body","head","left_front_leg","right_front_leg","left_hind_leg","right_hind_leg"})
                if(!root.getChild(name).storePose().equals(reference.getChild(name).storePose()))throw new AssertionError("Rooting pose leaked into activity "+activity);
        }
        Blockbranch.LOGGER.info("BOAR_ROOT_AUDIT PASS poses={} snoutNearSoil=true headTusksAboveSoil=true headJointInsideBody=true reset=true raisedSnoutDetected=true",poses);
    }
    private static void assertRooting(ModelPart root){
        float bottom=muzzleBottom(root);
        if(bottom<23.4F||bottom>24)throw new AssertionError("Rooting snout must meet soil without burial: "+bottom);
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.startsWith("/head"))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(p.y>24.0001F)throw new AssertionError("Rooting head/tusk buried: "+path);
            }
        });
        var stack=new PoseStack();root.getChild("head").translateAndRotate(stack);
        var joint=stack.last().pose().transformPosition(new Vector3f()).mul(16);
        if(joint.x< -5||joint.x>5||joint.y<10||joint.y>18||joint.z< -8||joint.z>8)throw new AssertionError("Rooting head joint outside torso");
    }
    static float muzzleBottom(ModelPart root){
        float[] bottom={Float.NEGATIVE_INFINITY};int[] count={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/head")||index!=1)return;
            count[0]++;
            for(var face:cube.polygons)for(var vertex:face.vertices())bottom[0]=Math.max(bottom[0],pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16);
        });
        if(count[0]!=1||!Float.isFinite(bottom[0]))throw new AssertionError("Finite boar snout required");
        return bottom[0];
    }
    static void observe(Minecraft client,UUID id){
        WildBoar subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof WildBoar boar&&boar.getUUID().equals(id))subject=boar;
        if(subject==null||!subject.isAlive()||subject.isInWater())throw new AssertionError("Live land boar required");
        var renderer=(BoarRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
        var root=BoarModel.layer().bakeRoot();new BoarModel(root).setupAnim(state);
        if(state.activity==1)assertRooting(root);
        Blockbranch.LOGGER.info("BOAR_ROOT_KEYFRAME baby={} activity={} muzzleBottomModelY={} gapModelUnits={}",state.isBaby,state.activity,muzzleBottom(root),24-muzzleBottom(root));
    }
    private BoarRootAudit(){}
}
