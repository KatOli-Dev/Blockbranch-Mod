package blockbranch.realistic;

import java.util.*;

/** Accepted 7dbbd57 diagnostics: exact two-thirds bank predictions, strict water/catchments and unchanged controls. */
public final class RiverBankReliefCheck {
    private record Witness(long seed,int x,int z,int carved,int oldHeight,int water,int basin,boolean closed,double oldDetail,double oldBank,double lower,double upper){
        int predict(double detail){int h=(int)Math.round(carved+Math.clamp(detail,lower,upper));return water==DrainageNetwork.DRY?h:Math.min(water-2,h);}
    }
    // Derived independently from the accepted pre-adjustment field; oldDetail includes
    // the old dry-bank contribution. Removing one-third of that contribution is the
    // requested target, with the original rounding and physical reservation retained.
    private static final Witness[] WITNESSES={
        new Witness(424242L,724000,980000,689,685,-2147483648,750015,false,-4.351490268271991,-4.351490268271991,-7.355592645150239,11.562270438695311),
        new Witness(424242L,500000,-684000,201,204,-2147483648,354260,true,3.4352937253940423,3.4352937253940423,-7.535798029183908,11.796537437939081),
        new Witness(424242L,-1407836,-1686396,3105,3102,-2147483648,113954,false,-3.001736409862361,-3.001736409862361,-7.871468321859538,12.2329088184174),
        new Witness(424242L,-1407908,-1686464,3105,3110,-2147483648,113954,false,4.799174392644044,4.799174392644044,-7.680630484116364,11.984819629351273),
        new Witness(424242L,1064720,1284212,2889,2886,-2147483648,789027,false,-3.3662442745340027,-3.3662442745340027,-7.367948905647154,11.5783335773413),
        new Witness(424242L,1064616,1284136,2884,2888,-2147483648,789027,false,3.952774559079184,3.952774559079184,-6.871961475974786,10.933549918767222),
        new Witness(424242L,1359500,-1694480,70,68,-2147483648,105391,false,-2.2282742979251973,-2.2282742979251973,-3.5,6.55),
        new Witness(424242L,1359296,-1694416,71,73,-2147483648,105391,false,1.9813542742809707,1.9813542742809707,-4.0,7.2),
        new Witness(424242L,-944692,-1698388,78,74,-2147483648,115057,false,-4.466978515595969,-4.466978515595969,-7.5,11.75),
        new Witness(424242L,-944868,-1698448,79,83,-2147483648,115057,false,4.143357445254593,4.143357445254593,-8.0,12.4),
        new Witness(1L,-556000,-1484000,77,73,-2147483648,155155,false,-3.887832659230268,-3.887832659230268,-7.0,11.1),
        new Witness(1L,212000,-108000,146,148,-2147483648,451620,false,2.167901026478171,2.167901026478171,-5.753198312336124,9.479157806036962),
        new Witness(1L,-381940,-1692540,2312,2324,-2147483648,88595,true,12.107597527786881,-3.5888405347368484,-9.0982937222916,13.827781838979082),
        new Witness(1L,-381900,-1692572,2306,2310,-2147483648,88595,true,4.264083628266918,3.5250777090380367,-6.03998768778888,9.851983994125543),
        new Witness(-17L,-636000,-1372000,1862,1858,-2147483648,232024,false,-4.323223675902558,-3.0826057823672444,-8.025411303975943,12.433034695168727),
        new Witness(-17L,1716000,1716000,985,987,-2147483648,957254,true,2.066943853105196,2.066943853105196,-5.575999147943492,9.24879889232654),
        new Witness(-17L,-1560748,-1477992,2363,2359,-2147483648,140539,true,-4.483337725520345,-4.26100537689533,-7.500332009066824,11.75043161178687),
        new Witness(-17L,-1560740,-1478044,2363,2367,-2147483648,140539,true,3.6224037035781724,3.63589710271776,-7.500221130931777,11.75028747021131),
        new Witness(424242L,-1332024,-1900024,2574,2568,-2147483648,40193,false,-6.314692168605091,0.0,-120.0,120.0),
        new Witness(424242L,-1571996,-1700032,413,413,-2147483648,122101,false,0.0,0.0,-120.0,120.0),
        new Witness(424242L,-1611988,-1868041,84,84,-2147483648,58538,false,0.0,-0.0,-10.5,15.65),
        new Witness(424242L,-765612,-1700131,351,351,357,99750,true,-0.41353057618894945,0.0,-1.75,1.75),
        new Witness(424242L,-1310210,-1695566,56,57,64,100642,false,0.8906919078611468,0.0,-2.4499999999999997,2.4499999999999997),
        new Witness(424242L,-1900000,-1900000,-3806,-3817,64,49248,false,-11.145656877757622,0.0,-26.0,26.0),
        new Witness(424242L,-1612000,-1900000,87,87,-2147483648,48293,false,0.0,0.0,-12.0,17.6),
        new Witness(1L,-1836000,-1900000,59,60,64,60525,false,0.7329246072261804,0.0,-1.4,1.4),
        new Witness(1L,-1900000,-1900000,98,98,-2147483648,51292,false,0.0,-0.0,-17.5,24.75),
        new Witness(-17L,-1900000,-1900000,-4973,-4973,64,49248,false,0.09749057517527039,0.0,-26.0,26.0),
        new Witness(-17L,-1756000,-1900000,63,63,-2147483648,49283,false,0.0,0.0,-0.0,2.0),
    };
    public static void main(String[]args){
        var fields=new HashMap<Long,DrainageNetwork>();int rejectedOldAmplitude=0;
        for(var w:WITNESSES){
            if(w.predict(w.oldDetail())!=w.oldHeight())throw new AssertionError("Corrupt accepted-field fixture");
            int expected=w.predict(w.oldDetail()-w.oldBank()/3);
            if(expected!=w.oldHeight())rejectedOldAmplitude++;
            var s=fields.computeIfAbsent(w.seed(),seed->new DrainageNetwork(new TerrainShape(seed),seed)).sample(w.x(),w.z());
            if(s.height()!=expected||s.waterLevel()!=w.water()||s.basin()!=w.basin()||s.closed()!=w.closed())throw new AssertionError("Bank amplitude/water/catchment at "+w.seed()+":"+w.x()+","+w.z()+" expected="+expected+" actual="+s);
        }
        if(rejectedOldAmplitude<12)throw new AssertionError("Old-amplitude negative control no longer discriminates");
        for(int i=WITNESSES.length-1;i>=0;i--){var w=WITNESSES[i];if(fields.get(w.seed()).sample(w.x(),w.z()).height()!=w.predict(w.oldDetail()-w.oldBank()/3))throw new AssertionError("Reverse generation order");}
        var camera=fields.get(424242L).sample(1064701,1284194);
        if(camera.height()!=2876||camera.waterLevel()!=DrainageNetwork.DRY)throw new AssertionError("Delivered Frozen River ground-camera clearance changed");
        System.out.println("RIVER_BANK_TWO_THIRDS_FIXED_PREDICTION_HYDROLOGY_ORDER_PASS witnesses="+WITNESSES.length+" oldAmplitudeRejected="+rejectedOldAmplitude);
    }
}
