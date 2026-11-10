package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.Mallard;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Bill-to-ground measurement from transformed native model polygons. */
final class MallardPeckAudit {
    static void verify(){
        var root=MallardModel.layer().bakeRoot();var model=new MallardModel(root);var state=new MallardRenderer.State();int stationary=0,protectedPoses=0;
        for(boolean baby:new boolean[]{false,true})for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<128;phase++){
            state.isBaby=baby;state.activity=1;state.yRot=yaw;state.xRot=30;state.ageInTicks=(float)(phase*Math.PI*2/(128*.2));model.setupAnim(state);
            assertPeck(root);stationary++;
        }
        root.getChild("head").y-=3.3F;
        if(billBottom(root)>=23.5F)throw new AssertionError("Peck audit missed raised bill");
        for(int activity:new int[]{0,2,3}){
            state.activity=1;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            var reference=MallardModel.layer().bakeRoot();new MallardModel(reference).setupAnim(state);samePose(root,reference);
        }
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(float speed:new float[]{0,.25F,.6F,1})for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<16;phase++){
            if(!water&&speed==0)continue;
            state.isBaby=baby;state.isInWater=water;state.activity=1;state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*5;state.yRot=yaw;model.setupAnim(state);
            var reference=MallardModel.layer().bakeRoot();state.activity=0;new MallardModel(reference).setupAnim(state);
            reference.getChild("head").xRot=.6F+.08F*net.minecraft.util.Mth.sin(state.ageInTicks*.2F);
            samePose(root,reference);protectedPoses++;
        }
        Blockbranch.LOGGER.info("MALLARD_PECK_AUDIT PASS stationaryPoses={} protectedPoses={} billNearGrass=true headAboveGrass=true headAttached=true reset=true raisedBillDetected=true waterAndApproachUnchanged=true",stationary,protectedPoses);
    }
    private static void samePose(ModelPart current,ModelPart reference){
        for(String name:new String[]{"body","head","left_wing","right_wing","left_leg","right_leg"})
            if(!current.getChild(name).storePose().equals(reference.getChild(name).storePose())||current.getChild(name).visible!=reference.getChild(name).visible)throw new AssertionError("Protected mallard pose changed: "+name);
    }
    private static void assertPeck(ModelPart root){
        float bottom=billBottom(root);
        if(bottom<23.5F||bottom>24)throw new AssertionError("Bill must meet grass without burial: "+bottom);
        float[] body={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        Vector3f[] joint={null};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(path.equals("/head")&&index==0)joint[0]=pose.pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,(cube.minY+cube.maxY)/32,cube.maxZ/16)).mul(16);
            if(!path.equals("/head")&&!path.equals("/body"))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var point=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(path.equals("/head")){if(point.y>24.0001F)throw new AssertionError("Peck head buried");}
                else for(int axis=0;axis<3;axis++){body[axis]=Math.min(body[axis],point.get(axis));body[axis+3]=Math.max(body[axis+3],point.get(axis));}
            }
        });
        if(joint[0]==null)throw new AssertionError("Head joint required");
        for(int axis=0;axis<3;axis++)if(joint[0].get(axis)<body[axis]||joint[0].get(axis)>body[axis+3])throw new AssertionError("Peck head detached from body");
    }
    static float billBottom(ModelPart root){
        float[] bottom={Float.NEGATIVE_INFINITY};int[] count={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/head")||index!=1)return;count[0]++;
            for(var face:cube.polygons)for(var vertex:face.vertices())bottom[0]=Math.max(bottom[0],pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16);
        });
        if(count[0]!=1||!Float.isFinite(bottom[0]))throw new AssertionError("Finite native bill required");
        return bottom[0];
    }
    static void observe(Minecraft client,UUID id){
        Mallard subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof Mallard duck&&duck.getUUID().equals(id))subject=duck;
        if(subject==null||!subject.isAlive()||subject.isInWater())throw new AssertionError("Live land mallard required");
        var renderer=(MallardRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
        var root=MallardModel.layer().bakeRoot();new MallardModel(root).setupAnim(state);
        if(state.activity==1&&state.walkAnimationSpeed==0)assertPeck(root);
        Blockbranch.LOGGER.info("MALLARD_PECK_KEYFRAME baby={} drake={} activity={} billBottomModelY={} gapModelUnits={}",state.isBaby,state.drake,state.activity,billBottom(root),24-billBottom(root));
    }
    private MallardPeckAudit(){}
}
