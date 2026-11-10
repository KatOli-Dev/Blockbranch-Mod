package blockbranch.realistic;

import java.security.MessageDigest;
import java.util.HexFormat;

/** Native moss has a habitat requirement; deciduous colony shapes keep their own identities. */
public final class MossHabitatCheck {
    public static void main(String[] args)throws Exception{
        for(long seed:new long[]{424242,1,-17})for(double wet:new double[]{.25,.4,.5,.65})for(boolean shade:new boolean[]{false,true}){
            var site=new GroundEcology.Site(wet,.2,.1,0,0,shade);
            boolean eligible=wet>=.55||(shade&&wet>=.35);
            for(int family:new int[]{0,1,2}){
                int moss=0,grass=0,podzol=0;
                for(int z=-64;z<64;z++)for(int x=-64;x<64;x++)for(boolean clear:new boolean[]{false,true}){
                    int a=family==0?GroundEcology.forest(seed,x,z,1,site,clear):family==1?OldGrowthFloor.cover(seed,x,z,site,clear):Taiga.cover(seed,x,z,site,clear);
                    require(a!=3&&a!=6,"Fresh boreal selectors never emit legacy moss-cube codes");
                    if(BorealSurface.mossSite(a)){moss++;require(eligible,"Moss cannot substitute for grass outside its habitat");}
                    if(a==8||a==12)grass++;
                    if(a==1)podzol++;
                    require(clear||a==1||a==2||a==BorealSurface.SOIL||a==12,"Occupied sites have substrate only");
                }
                require(!eligible?moss==0:family==2&&wet<=.45?moss==0:moss>100,"Retain genuine moist/sheltered moss, reject dry open substitution");
                if(!eligible)require(shade?podzol>100:grass>100,"Unsuitable moss candidates become actual grass or shaded litter soil");
                System.out.printf("MOSS_HABITAT seed=%d family=%d wet=%.2f shade=%s moss=%d grass=%d podzol=%d%n",seed,family,wet,shade,moss,grass,podzol);
            }
        }
        var hash=MessageDigest.getInstance("SHA-256");
        for(long seed:new long[]{424242,1,-17})for(double wet:new double[]{.25,.4,.65})for(boolean shade:new boolean[]{false,true})for(int z=-32;z<32;z++)for(int x=-32;x<32;x++){
            var site=new GroundEcology.Site(wet,.2,.1,0,0,shade);
            hash.update((byte)TemperateForestVariants.birchCover(seed,x,z,site,true));
            hash.update((byte)DarkCherryWoodland.cover(seed,x,z,site,true,1));
            hash.update((byte)DarkCherryWoodland.cover(seed,x,z,site,true,2));
        }
        String fingerprint=HexFormat.of().formatHex(hash.digest());
        require(fingerprint.equals("46cde4c0a8bf4cbe2e765f3315472ef23703ba05617dc2e6b0f6d061a741e312"),"Deciduous material roles and colony layouts remain unchanged");
        System.out.println("MOSS_HABITAT_SEMANTICS_PASS deciduous="+fingerprint);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private MossHabitatCheck(){}
}
