package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Broad body, flat paddle tail, small ears; incisors are painted, not extra geometry. */
/** Frozen main d984d2be model for protected pose comparisons. */
final class BeaverQuietReference extends EntityModel<BeaverRenderer.State> {
    private final ModelPart body,head,tail,frontLeft,frontRight,backLeft,backRight;
    BeaverQuietReference(ModelPart root){super(root);body=root.getChild("body");head=root.getChild("head");tail=root.getChild("tail");frontLeft=root.getChild("front_left");frontRight=root.getChild("front_right");backLeft=root.getChild("back_left");backRight=root.getChild("back_right");}
    static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create()
                .texOffs(0,0).addBox(-4,-2.975F,-6,8,5.975F,7)
                .texOffs(0,14).addBox(-4,-4,1,8,7,3)
                .texOffs(80,0).addBox(-3,-2.975F,4,6,5.975F,2),PartPose.offset(0,19,0));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(40,0).addBox(-3,-2,-3,6,5,6).texOffs(40,16).addBox(-3,0,-5,6,3,2),PartPose.offset(0,18,-8));
        for(int sign:new int[]{-1,1})head.addOrReplaceChild("ear"+sign,CubeListBuilder.create().texOffs(68,0).addBox(-1,-1,-.5F,2,1,1),PartPose.offset(sign*2,-2,0));
        // One broad paddle and a narrow root keep the silhouette deliberately simple.
        root.addOrReplaceChild("tail",CubeListBuilder.create()
                .texOffs(0,24).addBox(-1,0,0,2,1,2)
                .texOffs(0,26).addBox(-4,0,2,8,1,8),PartPose.offset(0,22,6));
        for(String name:new String[]{"front_left","front_right","back_left","back_right"})root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(32,24).addBox(-1.5F,0,-2,3,2,4),PartPose.offset(name.endsWith("left")?2.475F:-2.475F,22,name.startsWith("front")?-3:4));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(BeaverRenderer.State state){super.setupAnim(state);head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        float stride=state.swimming?Mth.sin(state.ageInTicks*.4F)*.4F:Mth.cos(state.walkAnimationPos*.7F)*state.walkAnimationSpeed;
        frontLeft.xRot=backRight.xRot=stride;frontRight.xRot=backLeft.xRot=-stride;
        tail.yRot=state.swimming?Mth.sin(state.ageInTicks*.25F)*.18F:0;
        if(state.activity==1)head.xRot=.3F+.08F*Mth.sin(state.ageInTicks*.35F);
        frontLeft.visible=frontRight.visible=backLeft.visible=backRight.visible=state.activity!=2;
        if(state.activity==2){
            body.y+=2;tail.y+=1;
            // The land head tuck submerges a floating kit's nose at its native
            // buoyancy height. Keep the head raised during water rest instead.
            if(state.swimming)head.xRot=0;
            else {head.y+=2;head.xRot=.2F;}
        }
    }
}
