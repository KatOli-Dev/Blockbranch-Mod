package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.HarborSeal;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
final class SealRenderer extends MobRenderer<HarborSeal,SealRenderer.State,SealModel> {
    static final class State extends LivingEntityRenderState {int activity;boolean swimming;float swimPitch;}
    SealRenderer(EntityRendererProvider.Context context){super(context,new SealModel(SealModel.layer().bakeRoot()),.5F);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Blockbranch.id("textures/entity/harbor_seal"+(state.isBaby?"_pup":"")+".png");}
    @Override public void extractRenderState(HarborSeal animal,State state,float partialTick){
        super.extractRenderState(animal,state,partialTick);state.activity=animal.activity();state.swimming=animal.isInWater();state.swimPitch=0;
        if(state.swimming&&state.activity!=2&&animal.isAlive()){
            var velocity=animal.getDeltaMovement();
            // Ignore slow surface bobbing; blend into travel attitude without changing motion.
            float weight=Mth.clamp((float)((velocity.length()-.04)/.04),0,1);
            float angle=(float)Math.toDegrees(Math.atan2(-velocity.y,Math.hypot(velocity.x,velocity.z)));
            state.swimPitch=Mth.clamp(angle,-35,35)*weight;
        }
    }
    @Override protected void setupRotations(State state,com.mojang.blaze3d.vertex.PoseStack pose,float bodyRot,float scale){
        super.setupRotations(state,pose,bodyRot,scale);
        if(state.swimming&&state.activity!=2&&state.swimPitch!=0){
            // Rotate around the existing torso axis, keeping its center anchored to the entity.
            double center=state.ageScale*.9F*(1.501F-19.5F/16);
            pose.translate(0,center,0);pose.rotateDegrees(com.mojang.math.Axis.XP,-state.swimPitch);pose.translate(0,-center,0);
        }
    }
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(state.ageScale*.9F,state.ageScale*.9F,state.ageScale*.9F);}
}
