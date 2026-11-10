package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Two plain body cuboids, a square head/muzzle, and four paddle flippers. */
final class SealModel extends EntityModel<SealRenderer.State> {
    private final ModelPart body,head,left,right,rearLeft,rearRight;
    SealModel(ModelPart root){super(root);body=root.getChild("body");head=root.getChild("head");left=root.getChild("left");right=root.getChild("right");rearLeft=root.getChild("rear_left");rearRight=root.getChild("rear_right");}
    static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        // The smaller hindquarter box gives a simple stepped silhouette, like vanilla animal models.
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,-3.5F,-9.5F,8,7,12)
            .texOffs(0,42).addBox(-3,-2.5F,2.475F,6,5.975F,7.025F),PartPose.offset(0,19.5F,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(58,0).addBox(-2.5F,-2.5F,-2.5F,5,5,5).texOffs(58,12).addBox(-2,0,-3.5F,4,2,1),PartPose.offset(0,19,-11));
        root.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,30).addBox(0,0,-2,5,1,5),PartPose.offset(3,23,-3));
        root.addOrReplaceChild("right",CubeListBuilder.create().texOffs(0,30).addBox(-5,0,-2,5,1,5),PartPose.offset(-3,23,-3));
        root.addOrReplaceChild("rear_left",CubeListBuilder.create().texOffs(24,30).addBox(0,0,0,3.975F,1,6),PartPose.offset(0,23,9));
        root.addOrReplaceChild("rear_right",CubeListBuilder.create().texOffs(24,30).addBox(-3.975F,0,0,3.975F,1,6),PartPose.offset(0,23,9));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(SealRenderer.State state){super.setupAnim(state);head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        if(state.swimming&&state.activity!=2&&state.swimPitch!=0){
            // Express the existing gaze direction in the pitched torso's local frame.
            float x=-Mth.sin(head.yRot)*Mth.cos(head.xRot),y=Mth.sin(head.xRot),z=-Mth.cos(head.yRot)*Mth.cos(head.xRot);
            float tilt=state.swimPitch*Mth.DEG_TO_RAD,s=Mth.sin(tilt),c=Mth.cos(tilt);
            float localY=c*y+s*z,localZ=-s*y+c*z;
            head.yRot=(float)Math.atan2(-x,-localZ);head.xRot=(float)Math.atan2(localY,Math.hypot(x,localZ));
        }
        // The one-unit-thick forepaddles pivot at Y=23 above the Y=24 sole plane.
        // A nonpositive land stroke lifts them and returns them to the floor.
        float paddle=state.swimming?Mth.sin(state.ageInTicks*.28F)*.35F:state.activity==2?0:
            -(.5F+.5F*Mth.sin(state.walkAnimationPos*.8F))*state.walkAnimationSpeed*.65F;
        left.zRot=paddle;right.zRot=-paddle;rearLeft.xRot=rearRight.xRot=state.swimming?paddle*.6F:0;
        if(state.isBaby){head.xScale=1.2F;head.yScale=1.15F;}
        if(state.activity==2){body.yScale=.8F;body.y+=.7F;head.y+=1.5F;head.xRot=.1F;
            left.yRot=-.45F;right.yRot=.45F;rearLeft.yRot=.15F;rearRight.yRot=-.15F;}
    }
}
