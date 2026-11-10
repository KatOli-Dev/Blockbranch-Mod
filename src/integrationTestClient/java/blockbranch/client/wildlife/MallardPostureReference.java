package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Frozen main cbbe9f93 poses for non-flight and juvenile preservation. */
final class MallardPostureReference extends EntityModel<MallardRenderer.State> {
    private final ModelPart body,head,leftWing,rightWing,leftLeg,rightLeg;
    MallardPostureReference(ModelPart root){super(root);body=root.getChild("body");head=root.getChild("head");leftWing=root.getChild("left_wing");rightWing=root.getChild("right_wing");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");}
    static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,-3,-6,8,6,12),PartPose.offset(0,19,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,20).addBox(-2.5F,-5,-3,5,6,5).texOffs(24,20).addBox(-2,-2,-6,4,2,3),PartPose.offset(0,17,-5));
        root.addOrReplaceChild("left_wing",CubeListBuilder.create().texOffs(40,0).addBox(0,0,-5,1,4,9),PartPose.offset(4,17,0));
        root.addOrReplaceChild("right_wing",CubeListBuilder.create().texOffs(40,0).addBox(-1,0,-5,1,4,9),PartPose.offset(-4,17,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,34).addBox(-1,0,-2,2,2,3),PartPose.offset(2,22,2));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,34).addBox(-1,0,-2,2,2,3),PartPose.offset(-2,22,2));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(MallardRenderer.State state){super.setupAnim(state);head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        if(state.isBaby){
            // A larger head and compact downy torso distinguish a hatchling without
            // extra cubes. Keep the belly at Y=22 so the existing feet remain attached.
            head.xScale=head.yScale=head.zScale=1.3F;
            body.xScale=.9F;body.yScale=1.15F;body.zScale=.82F;body.y=18.55F;
            leftWing.x=3.6F;rightWing.x=-3.6F;
            leftWing.xScale=rightWing.xScale=.9F;leftWing.zScale=rightWing.zScale=.82F;
        }
        leftLeg.xRot=Mth.cos(state.walkAnimationPos*.7F)*state.walkAnimationSpeed;rightLeg.xRot=-leftLeg.xRot;
        if(state.activity==1){
            float bob=Mth.sin(state.ageInTicks*.2F);
            head.xRot=.6F+.08F*bob;
            if(!state.isInWater){
                // Reach grass as the stride settles; a shallow bob keeps the bill
                // above the sole plane. The same activity retains its water pose.
                float reach=1-Mth.clamp(state.walkAnimationSpeed*4,0,1);
                head.y+=(state.isBaby?2.25F:3.3F)*reach;head.xRot=Mth.lerp(reach,head.xRot,.6F+.02F*bob);
            }
        }
        leftLeg.visible=rightLeg.visible=state.activity!=2;
        if(state.activity==2){body.y+=2;leftWing.y+=2;rightWing.y+=2;head.y+=2;head.xRot=.3F;}
        // Retain the folded angle and period, but carry the outer wings above the back on upstroke.
        if(state.activity==3){float flap=1.2F+Mth.sin(state.ageInTicks*.8F);leftWing.zRot=-flap;rightWing.zRot=flap;}
    }
}
