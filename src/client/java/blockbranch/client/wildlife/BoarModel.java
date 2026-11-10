package blockbranch.client.wildlife;

import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Accepted nine-cuboid shape with coats mapped onto its unrotated body UV net. */
final class BoarModel extends PigModel {
    BoarModel(ModelPart root) { super(root); }
    static LayerDefinition layer() {
        var mesh=createBasePigModel(CubeDeformation.NONE);
        var root=mesh.getRoot();
        // Same world-space box as the accepted native body rotated by pi/2;
        // baking that rotation into the box lets the original coat retain its UV orientation.
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,6)
            .addBox(-5,-4,-8,10,8,16),PartPose.offset(0,14,0));
        for(String name:new String[]{"left_front_leg","right_front_leg","left_hind_leg","right_hind_leg"}) {
            boolean left=name.startsWith("left"),front=name.contains("front");
            root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,58).mirror(left)
                .addBox(-2,0,-2,4,6,4),PartPose.offset(left?2.975F:-2.975F,18,front?-5:7));
        }
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(68,24)
            .addBox(-4,-4,-8,8,8,8).texOffs(72,46).addBox(-3,0,-11,6,4,3),PartPose.offset(0,12,-6));
        head.addOrReplaceChild("tusks",CubeListBuilder.create().texOffs(112,0)
            .addBox(-4,1,-11.04F,2,2,2).addBox(2,1,-11.04F,2,2,2),PartPose.ZERO);
        var result=LayerDefinition.create(mesh,128,80);
        return result;
    }
    @Override public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Rigid six-unit legs must not meet under the twelve-unit axle spacing.
        // Preserve the native phases, bounding only the extreme swing.
        rightFrontLeg.xRot=Mth.clamp(rightFrontLeg.xRot,-.9F,.9F);leftFrontLeg.xRot=Mth.clamp(leftFrontLeg.xRot,-.9F,.9F);
        rightHindLeg.xRot=Mth.clamp(rightHindLeg.xRot,-.9F,.9F);leftHindLeg.xRot=Mth.clamp(leftHindLeg.xRot,-.9F,.9F);
        head.getChild("tusks").visible=!state.isBaby;
        if(state instanceof BoarRenderer.State boar) {
            if(boar.activity==1){
                // Lower the attached head for snout-rooting. A shallow bob keeps
                // the muzzle close to the sole plane without burying it or the tusks.
                head.y+=3.75F;head.xRot=.4F+.02F*Mth.sin(state.ageInTicks*.3F);
            }
            if(boar.activity==2){
                // The standing sole plane is Y=24. Put the resting belly on that
                // same plane and tuck legs inside it instead of suspending the body.
                body.y+=6;head.y+=7.95F;head.xRot=0;
                rightFrontLeg.y=leftFrontLeg.y=rightHindLeg.y=leftHindLeg.y=21.9F;
                rightFrontLeg.xRot=leftFrontLeg.xRot=Mth.HALF_PI;
                rightHindLeg.xRot=leftHindLeg.xRot=-Mth.HALF_PI;
            }
        }
    }
}
