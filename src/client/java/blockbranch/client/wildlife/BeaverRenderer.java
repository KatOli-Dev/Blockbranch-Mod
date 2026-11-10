package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.EurasianBeaver;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
final class BeaverRenderer extends MobRenderer<EurasianBeaver,BeaverRenderer.State,BeaverModel> {
    private static final float MODEL_SCALE=.75F;
    static final class State extends LivingEntityRenderState {int activity;boolean swimming;}
    BeaverRenderer(EntityRendererProvider.Context context){super(context,new BeaverModel(BeaverModel.layer().bakeRoot()),.3F);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Blockbranch.id("textures/entity/eurasian_beaver"+(state.isBaby?"_kit":"")+".png");}
    @Override public void extractRenderState(EurasianBeaver animal,State state,float partialTick){super.extractRenderState(animal,state,partialTick);state.activity=animal.activity();state.swimming=animal.isInWater();}
    // Native shadow scaling includes age and entity size, but not this model scale.
    @Override protected float getShadowRadius(State state){return super.getShadowRadius(state)*MODEL_SCALE;}
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(state.ageScale*MODEL_SCALE,state.ageScale*MODEL_SCALE,state.ageScale*MODEL_SCALE);}
}
