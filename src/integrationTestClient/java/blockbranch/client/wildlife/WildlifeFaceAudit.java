package blockbranch.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.IntConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Vector3f;

/** Opt-in moving-mesh audit. Uses actual deformed polygon vertices, not logical cube bounds. */
final class WildlifeFaceAudit {
    private record Face(String cube,Vector3f normal,List<Vector3f> vertices){}
    private record Point(double x,double y){}
    static void verify(){
        controls();
        var boar=BoarModel.layer().bakeRoot();var bs=new BoarRenderer.State();audit("boar",boar,new BoarModel(boar),bs,a->bs.activity=a);
        var deer=DeerModel.layer().bakeRoot();var ds=new DeerRenderer.State();ds.stag=true;audit("deer",deer,new DeerModel(deer),ds,a->ds.activity=a);
        var bison=BisonModel.layer().bakeRoot();var bis=new BisonRenderer.State();audit("bison",bison,new BisonModel(bison),bis,a->bis.activity=a);
        var bear=BearModel.layer().bakeRoot();var brs=new BearRenderer.State();audit("bear",bear,new BearModel(bear),brs,a->{brs.activity=a;brs.attack=a==3?.5F:0;});
        var beaver=BeaverModel.layer().bakeRoot();var bvs=new BeaverRenderer.State();audit("beaver",beaver,new BeaverModel(beaver),bvs,a->{bvs.activity=a;bvs.swimming=a==3;});
        var mallard=MallardModel.layer().bakeRoot();var ms=new MallardRenderer.State();audit("mallard",mallard,new MallardModel(mallard),ms,a->ms.activity=a);
        var seal=SealModel.layer().bakeRoot();var ss=new SealRenderer.State();audit("seal",seal,new SealModel(seal),ss,a->{ss.activity=a;ss.swimming=a==3;});
        var perch=PerchModel.layer().bakeRoot();var ps=new LivingEntityRenderState();audit("perch",perch,new PerchModel(perch),ps,a->ps.isInWater=a!=2);
        blockbranch.Blockbranch.LOGGER.info("WILDLIFE_FACE_AUDIT PASS species=8 posesPerSpecies=64 actualDeformedVertices=true adjacentJointNegativeControl=true movingLimbPositiveControl=true");
    }
    private static <T extends LivingEntityRenderState> void audit(String name,ModelPart root,EntityModel<T> model,T state,IntConsumer activity){
        for(boolean baby:new boolean[]{false,true})for(int action=0;action<4;action++)for(float phase:new float[]{0,1.3F,3.1F,4.7F})for(int look:new int[]{0,1}){
            state.isBaby=baby;state.walkAnimationPos=phase;state.walkAnimationSpeed=(action==0||action==3)?1:0;state.ageInTicks=phase*10+2;state.yRot=look*30;state.xRot=look*15;activity.accept(action);model.setupAnim(state);
            var hits=overlaps(root);if(!hits.isEmpty())throw new AssertionError(name+" baby="+baby+" activity="+action+" phase="+phase+" look="+look+" exposed coincident faces "+hits);
        }
    }
    private static boolean visible(ModelPart root,String path){var part=root;if(!part.visible)return false;for(String key:path.split("/")){if(key.isEmpty())continue;part=part.getChild(key);if(!part.visible)return false;}return true;}
    private static List<String> overlaps(ModelPart root){
        var faces=new ArrayList<Face>();root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!visible(root,path))return;
            for(var polygon:cube.polygons){var vertices=new ArrayList<Vector3f>();for(var v:polygon.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(v.worldX(),v.worldY(),v.worldZ())));
                var normal=pose.pose().transformDirection(new Vector3f(polygon.normal())).normalize();
                if(new Vector3f(vertices.get(1)).sub(vertices.get(0)).cross(new Vector3f(vertices.get(2)).sub(vertices.get(0))).lengthSquared()<1e-14)continue;
                faces.add(new Face(path+":"+index,normal,vertices));
            }
        });
        var hits=new ArrayList<String>();for(int i=0;i<faces.size();i++)for(int j=i+1;j<faces.size();j++){
            var a=faces.get(i);var b=faces.get(j);if(a.cube.equals(b.cube)||a.normal.dot(b.normal)<.99999F)continue;
            if(Math.abs(new Vector3f(b.vertices.getFirst()).sub(a.vertices.getFirst()).dot(a.normal))>1e-6)continue;
            int drop=Math.abs(a.normal.x)>Math.abs(a.normal.y)?0:1;if(Math.abs(a.normal.z)>Math.abs(a.normal.get(drop)))drop=2;
            if(intersectionArea(project(a.vertices,drop),project(b.vertices,drop))>1e-7){hits.add(a.cube+" / "+b.cube);if(hits.size()==8)return hits;}
        }return hits;
    }
    private static List<Point> project(List<Vector3f> input,int drop){var out=new ArrayList<Point>();for(var v:input)out.add(new Point(v.get((drop+1)%3),v.get((drop+2)%3)));return out;}
    private static double cross(Point a,Point b,Point p){return (b.x-a.x)*(p.y-a.y)-(b.y-a.y)*(p.x-a.x);}
    private static double area(List<Point> points){double value=0;for(int i=0;i<points.size();i++){var a=points.get(i);var b=points.get((i+1)%points.size());value+=a.x*b.y-b.x*a.y;}return value*.5;}
    private static double intersectionArea(List<Point> subject,List<Point> clip){
        List<Point> out=subject;double orientation=Math.signum(area(clip));
        for(int i=0;i<clip.size()&&!out.isEmpty();i++){
            var a=clip.get(i);var b=clip.get((i+1)%clip.size());var input=out;out=new ArrayList<>();var previous=input.getLast();double before=cross(a,b,previous)*orientation;
            for(var current:input){double now=cross(a,b,current)*orientation;if((now>=0)!=(before>=0)){double t=before/(before-now);out.add(new Point(previous.x+(current.x-previous.x)*t,previous.y+(current.y-previous.y)*t));}if(now>=0)out.add(current);previous=current;before=now;}
        }return Math.abs(area(out));
    }
    private static void controls(){
        var mesh=new MeshDefinition();var r=mesh.getRoot();r.addOrReplaceChild("body",CubeListBuilder.create().addBox(-2,0,-2,4,4,4),PartPose.ZERO);
        r.addOrReplaceChild("nose",CubeListBuilder.create().addBox(-2,1,-4,4,2,2),PartPose.ZERO);
        if(!overlaps(LayerDefinition.create(mesh,32,32).bakeRoot()).isEmpty())throw new AssertionError("Adjacent joint incorrectly classified as overlapping");
        r.addOrReplaceChild("nose",CubeListBuilder.create().addBox(-2,1,-3,4,2,2),PartPose.ZERO);
        if(overlaps(LayerDefinition.create(mesh,32,32).bakeRoot()).isEmpty())throw new AssertionError("Static coplanar positive control missed");
        r.addOrReplaceChild("nose",CubeListBuilder.create().addBox(-1,0,-1,2,2,2),PartPose.offsetAndRotation(1,4,0,.7F,0,0));
        if(overlaps(LayerDefinition.create(mesh,32,32).bakeRoot()).isEmpty())throw new AssertionError("Moving limb positive control missed");
    }
    private WildlifeFaceAudit(){}
}
