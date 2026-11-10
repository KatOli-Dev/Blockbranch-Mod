package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Protect the approved non-rest mesh and measure the four existing tucked limbs. */
final class DeerRestAudit {
    private static final List<String> LEGS=List.of("front_left","front_right","back_left","back_right");
    static void verify(){
        var root=DeerModel.layer().bakeRoot();var model=new DeerModel(root);
        var old=DeerStandingReference.layer().bakeRoot();var reference=new DeerStandingReference(old);
        var state=new DeerRenderer.State();int protectedPoses=0,restPoses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean stag:new boolean[]{false,true})
        for(int activity=0;activity<3;activity++)for(float yaw:new float[]{-35,0,35})
        for(float speed:new float[]{0,.5F,1})for(int phase=0;phase<16;phase++){
            state.isBaby=baby;state.stag=stag;state.activity=activity;state.yRot=yaw;state.xRot=yaw/2;
            state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*7;
            model.setupAnim(state);reference.setupAnim(state);calfReference(old,state);
            // The supported belly and attached neck rise together by a quarter model unit.
            if(activity==2){old.getChild("body").y-=.25F;old.getChild("neck").y-=.25F;}
            sameMesh(root,old,activity==2);
            if(activity==2){assertTucked(root);restPoses++;}else protectedPoses++;
        }
        // Shared renderer models must restore unscaled standing/grazing legs after rest.
        for(int activity:new int[]{0,1}){
            state.activity=2;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            reference.setupAnim(state);calfReference(old,state);sameMesh(root,old,false);
        }
        Blockbranch.LOGGER.info("DEER_REST_AUDIT PASS protectedPoses={} restPoses={} fourExistingLimbs=true exposedHooves=true rootsAttached=true noBurial=true reset=true",protectedPoses,restPoses);
    }
    private static void assertTucked(ModelPart root){
        for(String name:LEGS){
            var leg=root.getChild(name);
            if(!leg.visible)throw new AssertionError("Resting limb missing: "+name);
            if(Math.abs(leg.x)-1>=4*root.getChild("body").xScale||leg.y<root.getChild("body").y-4||leg.y>root.getChild("body").y+4||leg.z< -7*root.getChild("body").zScale||leg.z>8*root.getChild("body").zScale)
                throw new AssertionError("Tucked limb root detached: "+name);
            if(Math.abs(leg.x)+1<4.25F)throw new AssertionError("Tucked limb hidden by torso");
        }
        var hooves=new HashSet<String>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!LEGS.contains(path.substring(1)))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(p.y>24.0001F||p.y<21.9F||p.z< -7||p.z>8)throw new AssertionError("Tucked leg outside belly/ground bounds: "+path);
                if(vertex.y()==13&&Math.abs(p.x)>4.25F){
                    if(p.z< -7||p.z>8)throw new AssertionError("Hoof must tuck inward beneath the belly");
                    if(p.y<21.9F||p.y>24)throw new AssertionError("Exposed hoof outside resting height");
                    hooves.add(path);
                }
            }
        });
        if(hooves.size()!=4)throw new AssertionError("All four existing hooves must be exposed");
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void sameMesh(ModelPart current,ModelPart reference,boolean resting){
        var a=points(current);var b=points(reference);
        if(!a.keySet().equals(b.keySet()))throw new AssertionError("Approved deer cube inventory changed");
        for(var key:a.keySet()){
            if(resting&&LEGS.stream().anyMatch(name->key.startsWith("/"+name+":")))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Approved deer polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected deer mesh changed: "+key);
        }
        for(String name:LEGS)if(!resting&&current.getChild(name).visible!=reference.getChild(name).visible)throw new AssertionError("Protected leg visibility changed");
    }
    // The rest-only reference predates the intentional juvenile proportions.
    // The age audit separately protects adults and every other calf part.
    private static void calfReference(ModelPart root,DeerRenderer.State state){
        if(state.isBaby){
            var body=root.getChild("body");var head=root.getChild("neck").getChild("head");
            body.xScale=.95F;body.zScale=.9F;head.xScale=1.25F;head.yScale=1.2F;head.zScale=.9F;
            if(state.activity==1)head.z-=.8F;
        }
    }
    private DeerRestAudit(){}
}
