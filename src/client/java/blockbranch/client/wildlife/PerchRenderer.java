package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.EuropeanPerch;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
final class PerchRenderer extends MobRenderer<EuropeanPerch,LivingEntityRenderState,PerchModel> {
    static final float MODEL_SCALE=.65F;
    PerchRenderer(EntityRendererProvider.Context context){super(context,new PerchModel(PerchModel.layer().bakeRoot()),.12F);}
    @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
    @Override public Identifier getTextureLocation(LivingEntityRenderState state){return Blockbranch.id("textures/entity/european_perch"+(PerchModel.landPose(state)?"_land":"")+".png");}
    @Override protected void scale(LivingEntityRenderState state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(MODEL_SCALE,MODEL_SCALE,MODEL_SCALE);}
    @Override protected void setupRotations(LivingEntityRenderState state,com.mojang.blaze3d.vertex.PoseStack pose,float bodyRot,float scale){
        super.setupRotations(state,pose,bodyRot,scale);
        if(!state.isInWater){
            // With the native 90-degree roll, the body's one-unit half-width
            // sets its sole height. Use the unrolled model's tiny floor clearance.
            pose.translate(.1,PerchModel.landPose(state)?MODEL_SCALE*(1F/16+.001F):.1,-.1);pose.rotateDegrees(com.mojang.math.Axis.ZP,90);}
    }
}
