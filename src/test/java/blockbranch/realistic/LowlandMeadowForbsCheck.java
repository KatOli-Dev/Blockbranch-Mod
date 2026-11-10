package blockbranch.realistic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Original plants and shared selectors stay exact while lit, empty sward gains coherent forbs. */
public final class LowlandMeadowForbsCheck {
    private static final int SIDE=256;
    private static final long[] SEEDS={424242,1,-17};
    private static final GroundEcology.Site[] SITES={GroundEcology.NEUTRAL,
        new GroundEcology.Site(.78,.5,.05,.1,.04,false),new GroundEcology.Site(.25,-.6,.9,.1,.04,false),GroundEcology.NEUTRAL.shade(true)};
    private static final LowlandMeadow.Site DRY=new LowlandMeadow.Site(263,DrainageNetwork.DRY,false);
    // Frozen bbfceb2: seed/site order above, z then x over [-128,128), big-endian x/z/code ints.
    private static final String[][] PROTECTED={
        {"e22005819a9659620674a354c9ada11da5fdb9c08aa43b93dec8663de87c3dcf","0a8bad8f796f6cb468ec3e4b397230590387df47afe87de3676a3a228034784e","07ad091cc7c9403d12359e9ee048b14a77c839021b3eaa7220cf0f51e06be40d","aaca6a5128aca31edf4fdbb644f23722a1f75719c3ca9ae39323a91e49368efc"},
        {"c07df971fbe584e4e412959afc26017450db1426f35f361a2d45cb4082283e23","5377490b2b4b4641ac8b55e844177bb9dda953ed313a64044e2a6beadded5559","49a3fe58aec5935882cc8bc8e58be12fae320975c083bc067f4edb9195463221","24b50f4df7d833b23204b138bff7aa8ec1feacac1a5c8fbee3498bc14b688708"},
        {"2c68ddee203aaf57b125c22970b1e55d07d0a98905c1363aebd046de8ad7272d","2fce1f9d90f17bd208fc599db855b87f32869c35bed8116c666b6dadd05c8264","f56754ecf86cec561947130968cacacfedb1b51bd6837f90cdf5f1e27e3973de","84a0d87f09e9e1229a86ba6e511f7fea61db3e38073c2a7b3036eaa12c67d0af"}
    };
    private static final String[][] BASE={
        {"430357be433dc899d7600e3d7c6161287b0cf635a748d9080846311b2eb8be87","4d6b11622bc4b4822d94ef656081f43640936622f80cb29e97ebf69c28b4b559","3401ec4d25a5aa28d5a5028e329e99d7b929f587081ea477b0d7f6400210449a","22c43e0e0ad0d0559ff2ce55de57278435915d4a8f2d20c6839e58ebbe9cc3fe"},
        {"40a24a494ee9faea329bee34ac1f41266d1f4a1375f31db174a4b2caaa3fe6fc","b66318707abbffde05bdc5944ee99366756e6f31dd561f508d224dd2287c1436","742fa90a958482fddb0206b66ffe5b49f07a4ad29537f7d8d92dd3011540ac52","4bb1f57b55699cd7c46463605a2abb9f6bc3ce4b3f5b6c380a2a8576ee4ef471"},
        {"fb545961333d665108ad0740d358d68b325722ed735b6942a0be267c7acff0e7","4105213339296ba1c3290c743fe20a14a5ecb65a3a207bc0c0f1c992a4e7220b","e9fdbf1f10c53b7ce3af6647b21959de7230236aee1579d827757d57da29612e","5c290634278ea21a68e24d3564760d84a7150c3736992bb7405996e90c985125"}
    };
    private static final int[][] PROTECTED_COUNTS={{22587,27700,9544,19300},{22178,26902,9202,18954},{22999,28091,9603,19684}};
    // Plains/Montane/Sunflower order; sites above, clear=false/true, then z/x in [-32,32).
    private static final String[] OTHER_BIOMES={
        "27847492c264b98f303bfc23397753b60c9cf16852c1ca6831d7fca4910c637b",
        "3a65257ac059a59fb893598381c9cb93231fb99907259d33a853b014d5c39021",
        "a32b13f31999712703d5a659534accef7306962b38cb3ca0ceb92afccd858511"
    };
    public static void main(String[] args){verify();}
    public static void verify(){
        for(int seed=0;seed<SEEDS.length;seed++){
            for(int site=0;site<SITES.length;site++)selection(seed,site);
            isolation(seed);
        }
        require(LowlandMeadow.cover(1,0,0,false,DRY,SITES[0],true)==0,"Excluded meadow gains flowers");
        require(LowlandMeadow.cover(1,0,0,true,new LowlandMeadow.Site(263,265,false),SITES[0],true)==0,"Submerged meadow gains flowers");
        require(LowlandMeadow.cover(1,0,0,true,new LowlandMeadow.Site(263,DrainageNetwork.DRY,true),SITES[0],true)==0,"Reserved bank gains flowers");
        System.out.println("LOWLAND_MEADOW_FORBS_PROTECTED_SHADE_CLEARANCE_GAPS_SPECIES_ISOLATION_PASS seeds=3 sites=4");
    }
    private static void selection(int seedIndex,int siteIndex){
        long seed=SEEDS[seedIndex];var site=SITES[siteIndex];
        byte[] old=new byte[SIDE*SIDE],fresh=new byte[old.length];
        var originalHash=hash();var protectedHash=hash();var freshHash=hash();
        int protectedCount=0,added=0;int[] counts=new int[7];
        for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
            int i=(z+128)*SIDE+x+128,original=GroundEcology.meadow(seed,x,z,site);
            int current=LowlandMeadow.cover(seed,x,z,true,DRY,site,true);
            old[i]=(byte)original;fresh[i]=(byte)current;
            require(current>=1&&current<=6,"Fresh meadow emitted unsupported cover");counts[current]++;
            tuple(originalHash,x,z,original);tuple(freshHash,x,z,current);
            if(original>=2){
                require(current==original,"An original short-grass/fescue/flower site changed");
                protectedCount++;tuple(protectedHash,x,z,current);
            }
            if(current!=original){require(original==1&&flower(current)&&!site.shaded(),"Promotion outside lit bare sward");added++;}
            require(LowlandMeadow.cover(seed,x,z,true,DRY,site,false)==original,"Occupied canopy cell acquires a new plant or changes original cover");
        }
        require(hex(originalHash).equals(BASE[seedIndex][siteIndex]),"Shared GroundEcology meadow contract changed");
        require(protectedCount==PROTECTED_COUNTS[seedIndex][siteIndex]&&hex(protectedHash).equals(PROTECTED[seedIndex][siteIndex]),"Frozen original cover positions/species changed");
        if(site.shaded()){
            require(added==0&&hex(freshHash).equals(BASE[seedIndex][siteIndex]),"Shaded ground must remain byte-for-byte exact");return;
        }
        int oldFlowers=flowers(old),newFlowers=flowers(fresh);
        double density=newFlowers/(double)fresh.length;
        double[] lower={.05,.035,.01},upper={.11,.09,.05};
        require(added>0&&newFlowers>oldFlowers*1.8&&density>lower[siteIndex]&&density<upper[siteIndex],"Flower addition is ineffective or dominates the sward");
        for(int code=4;code<=6;code++)require(counts[code]>newFlowers*.10&&counts[code]<newFlowers*.65,"One species overwhelms the seeded colony mix");
        int oldEmpty8=empty(old,8),newEmpty8=empty(fresh,8),newEmpty16=empty(fresh,16);
        require(newEmpty8<oldEmpty8*.95&&newEmpty8>1024*.45,"Colony fragments stay sparse or grass gaps disappear");
        require(newEmpty16<=empty(old,16)&&newEmpty16>256*.20,"Flower distribution loses broad grass gaps");
        long pairs=0,sameSpecies=0;int[] crowd=new int[newFlowers];int ci=0;
        for(int z=0;z<SIDE;z++)for(int x=0;x<SIDE;x++){
            int i=z*SIDE+x;if(!flower(fresh[i]))continue;int neighbors=0;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                int nx=x+dx,nz=z+dz;if(nx>=0&&nx<SIDE&&nz>=0&&nz<SIDE&&flower(fresh[nz*SIDE+nx]))neighbors++;
            }
            crowd[ci++]=neighbors;
            if(x+1<SIDE&&flower(fresh[i+1])){pairs++;if(fresh[i]==fresh[i+1])sameSpecies++;}
            if(z+1<SIDE&&flower(fresh[i+SIDE])){pairs++;if(fresh[i]==fresh[i+SIDE])sameSpecies++;}
        }
        Arrays.sort(crowd);int p90=crowd[(crowd.length-1)*9/10];
        require(pairs>0&&sameSpecies/(double)pairs>.97,"New forbs lose coherent species colonies");
        require(p90<=7,"Colony centers crowd nearly every neighboring block");
        System.out.printf(Locale.ROOT,"LOWLAND_FORB_PATTERN seed=%d site=%d flowers=%d->%d density=%.3f empty8=%d->%d empty16=%d crowdP90=%d speciesAgreement=%.3f%n",seed,siteIndex,oldFlowers,newFlowers,density,oldEmpty8,newEmpty8,newEmpty16,p90,sameSpecies/(double)pairs);
    }
    private static void isolation(int seedIndex){
        var hash=hash();long seed=SEEDS[seedIndex];
        for(int site=0;site<SITES.length;site++)for(int clear=0;clear<2;clear++)for(int z=-32;z<32;z++)for(int x=-32;x<32;x++){
            integer(hash,site);integer(hash,clear);integer(hash,x);integer(hash,z);
            integer(hash,Plains.cover(seed,x,z,SITES[site],clear==1));
            integer(hash,MontaneMeadow.cover(seed,x,z,SITES[site],clear==1));
            integer(hash,SunflowerPlains.cover(seed,x,z,SITES[site],clear==1));
        }
        require(hex(hash).equals(OTHER_BIOMES[seedIndex]),"Other meadow biome selectors changed");
    }
    private static int empty(byte[] cover,int width){
        int empty=0;
        for(int z=0;z<SIDE;z+=width)for(int x=0;x<SIDE;x+=width){
            boolean present=false;
            for(int dz=0;dz<width&&!present;dz++)for(int dx=0;dx<width;dx++)if(flower(cover[(z+dz)*SIDE+x+dx])){present=true;break;}
            if(!present)empty++;
        }
        return empty;
    }
    private static int flowers(byte[] cover){int count=0;for(byte code:cover)if(flower(code))count++;return count;}
    private static boolean flower(int code){return code>=4&&code<=6;}
    private static MessageDigest hash(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException failure){throw new AssertionError(failure);}}
    private static String hex(MessageDigest hash){return HexFormat.of().formatHex(hash.digest());}
    private static void tuple(MessageDigest hash,int x,int z,int code){integer(hash,x);integer(hash,z);integer(hash,code);}
    private static void integer(MessageDigest hash,int value){hash.update((byte)(value>>>24));hash.update((byte)(value>>>16));hash.update((byte)(value>>>8));hash.update((byte)value);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private LowlandMeadowForbsCheck(){}
}
