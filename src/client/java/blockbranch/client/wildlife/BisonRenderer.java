package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.AmericanBison;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
final class BisonRenderer extends MobRenderer<AmericanBison,BisonRenderer.State,BisonModel> {
    static final class State extends LivingEntityRenderState {int activity;boolean threatening;}
    BisonRenderer(EntityRendererProvider.Context context){super(context,new BisonModel(BisonModel.layer().bakeRoot()),.8F);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Blockbranch.id("textures/entity/american_bison"+(state.isBaby?"_calf":"")+".png");}
    @Override public void extractRenderState(AmericanBison animal,State state,float partialTick){super.extractRenderState(animal,state,partialTick);state.activity=animal.activity();state.threatening=animal.isAggressive()&&animal.isAlive();}
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(state.ageScale,state.ageScale,state.ageScale);}
}
