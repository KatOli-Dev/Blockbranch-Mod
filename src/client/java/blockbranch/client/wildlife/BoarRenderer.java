package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.WildBoar;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

final class BoarRenderer extends MobRenderer<WildBoar, BoarRenderer.State, BoarModel> {
    static final class State extends LivingEntityRenderState { int activity; }
    BoarRenderer(EntityRendererProvider.Context context) { super(context, new BoarModel(BoarModel.layer().bakeRoot()), .6F); }
    @Override public State createRenderState() { return new State(); }
    @Override public Identifier getTextureLocation(State state) {
        return Blockbranch.id("textures/entity/" + (state.isBaby ? "wild_boar_piglet" : "wild_boar") + ".png");
    }
    @Override public void extractRenderState(WildBoar boar, State state, float partialTick) {
        super.extractRenderState(boar, state, partialTick); state.activity = boar.activity();
    }
    @Override protected void scale(State state, com.mojang.blaze3d.vertex.PoseStack pose) {
        pose.scale(state.ageScale, state.ageScale, state.ageScale);
    }
}
