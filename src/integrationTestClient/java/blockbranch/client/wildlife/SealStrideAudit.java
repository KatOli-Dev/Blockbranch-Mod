package blockbranch.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Ground clearance from the actual transformed foreflipper polygons. */
final class SealStrideAudit {
    static void verify(){
        var root=SealModel.layer().bakeRoot();var model=new SealModel(root);var state=new SealRenderer.State();
        int poses=0;boolean lifted=false,planted=false;
        for(boolean baby:new boolean[]{false,true})for(float speed:new float[]{0,.25F,.6F,1})for(int i=0;i<128;i++){
            state.isBaby=baby;state.activity=0;state.swimming=false;state.walkAnimationSpeed=speed;
            state.walkAnimationPos=(float)(i*Math.PI*2/(128*.8));model.setupAnim(state);
            if(bottom(root)>24.0001F)throw new AssertionError("Seal land stride penetrates the sole plane baby="+baby+" speed="+speed+" phase="+i);
            float angle=root.getChild("left").zRot;
            if(speed==0&&Math.abs(angle)>1e-6)throw new AssertionError("Idle foreflipper must remain planted");
            if(speed==1){lifted|=angle<-.3F;planted|=Math.abs(angle)<1e-4;}
            poses++;
        }
        if(!lifted||!planted)throw new AssertionError("Stride must include both lifting and planting");
        state.activity=2;state.walkAnimationSpeed=1;model.setupAnim(state);
        if(Math.abs(root.getChild("left").zRot)>1e-6||Math.abs(bottom(root)-24)>1e-4)throw new AssertionError("Rest pose retains a stale walking tilt");
        // Positive control: a downward tilt must be detected by the same mesh measurement.
        root.getChild("left").zRot=.65F;root.getChild("right").zRot=-.65F;
        if(bottom(root)<26)throw new AssertionError("Foreflipper ground audit missed deliberately buried paddles");
        blockbranch.Blockbranch.LOGGER.info("SEAL_STRIDE_AUDIT PASS poses={} adultPup=true idleRestPlant=true lifted=true downwardTiltDetected=true",poses);
    }
    static float bottom(ModelPart root){
        float[] result={Float.NEGATIVE_INFINITY};int[] count={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/left")&&!path.equals("/right"))return;
            count[0]++;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var point=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ()));
                result[0]=Math.max(result[0],point.y*16);
            }
        });
        if(count[0]!=2||!Float.isFinite(result[0]))throw new AssertionError("Both finite foreflipper meshes must be measured");
        return result[0];
    }
    private SealStrideAudit(){}
}
