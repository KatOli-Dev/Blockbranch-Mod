package blockbranch.realistic;

/** Explicit saved-feature combinations. New combinations get a new downgrade barrier. */
public final class StructureGenerationSchema {
 public static final int TREASURE=89,TREASURE_SHIPWRECK=90,WAYGATE=91,TREASURE_WAYGATE=92,ALL_DISCOVERIES=93,WETLAND_HUTS=94,SNOW_SHELTERS=95,WARM_RUINS=96,COLD_RUINS=97,DEEP_COLD_RUINS=98,DEEP_WARM_RUINS=99,NATIVE_HUT_IGLOO=100,NATIVE_ORDINARY=101,REIMAGINED=102,COMBINED=103,WEATHERED_LAYOUTS=104,WEATHERED_COMBINED=105;
 public static final int MANSION_WINGS_LAYOUTS=106,MANSION_WINGS_COMBINED=107,MANSION_WINGS_JUNGLE_LAYOUTS=108,MANSION_WINGS_JUNGLE_COMBINED=109;
 public static final int SANCTUARY_LAYOUTS=118,SANCTUARY_COMBINED=119,SANCTUARY_MANSION_LAYOUTS=120,SANCTUARY_MANSION_COMBINED=121;
 public static final int CRYPT_LAYOUTS=130,CRYPT_COMBINED=131,CRYPT_MANSION_LAYOUTS=132,CRYPT_MANSION_COMBINED=133;
 public static final int MANSION_PERIMETER_LAYOUTS=160,MANSION_PERIMETER_COMBINED=161,MANSION_PERIMETER_WINGS_LAYOUTS=162,MANSION_PERIMETER_WINGS_COMBINED=163;
 public static final int JUNGLE_REPLACEMENT_LAYOUTS=200,JUNGLE_REPLACEMENT_COMBINED=201,JUNGLE_REPLACEMENT_MANSION_LAYOUTS=202,JUNGLE_REPLACEMENT_MANSION_COMBINED=203;
 public static final int PYRAMID_REPLACEMENT_LAYOUTS=220,PYRAMID_REPLACEMENT_COMBINED=221,PYRAMID_REPLACEMENT_MANSION_LAYOUTS=222,PYRAMID_REPLACEMENT_MANSION_COMBINED=223;
 public static final int CRYPT_REPLACEMENT_LAYOUTS=240,CRYPT_REPLACEMENT_COMBINED=241,CRYPT_REPLACEMENT_MANSION_LAYOUTS=242,CRYPT_REPLACEMENT_MANSION_COMBINED=243;
 private StructureGenerationSchema(){}
 // Layout 6 remains reserved for an unpublished sanctuary candidate.
 public static boolean supportedLayout(int layout){return layout>=1&&layout<=5||layout==7||layout==8||layout==9;}
 // Definitions 3, 4 and 5 add sanctuary, crypt and mansion perimeter appearances in separate ranges;
 // the historical gallery barriers remain unchanged.
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts,int coverage,int mansionWings){
  if(mansionWings==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary,layouts,coverage);
  if(mansionWings!=1||!supportedLayout(layouts)||coverage<0||coverage>1||!matches(NATIVE_ORDINARY,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary))throw new IllegalArgumentException("Mansion wings require complete native definitions and reimagined layouts");
  return layouts==9?CRYPT_REPLACEMENT_MANSION_LAYOUTS+coverage:layouts==8?PYRAMID_REPLACEMENT_MANSION_LAYOUTS+coverage:layouts==7?JUNGLE_REPLACEMENT_MANSION_LAYOUTS+coverage:layouts==5?MANSION_PERIMETER_WINGS_LAYOUTS+coverage:layouts==4?CRYPT_MANSION_LAYOUTS+coverage:layouts==3?SANCTUARY_MANSION_LAYOUTS+coverage:MANSION_WINGS_LAYOUTS+2*(layouts-1)+coverage;
 }
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts,int coverage,int mansionWings){
  if(mansionWings==0)return matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary,layouts,coverage);
  return mansionWings==1&&supportedLayout(layouts)&&coverage>=0&&coverage<=1&&version==(layouts==9?CRYPT_REPLACEMENT_MANSION_LAYOUTS+coverage:layouts==8?PYRAMID_REPLACEMENT_MANSION_LAYOUTS+coverage:layouts==7?JUNGLE_REPLACEMENT_MANSION_LAYOUTS+coverage:layouts==5?MANSION_PERIMETER_WINGS_LAYOUTS+coverage:layouts==4?CRYPT_MANSION_LAYOUTS+coverage:layouts==3?SANCTUARY_MANSION_LAYOUTS+coverage:MANSION_WINGS_LAYOUTS+2*(layouts-1)+coverage)&&matches(NATIVE_ORDINARY,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary);
 }

 // Both independent private102 formats remain readable with their explicit XOR flags.
 // Combined103 is rejected by either historical102 reader. Definition-2 jungle
 // stonework uses104/105, rejected by all readers limited to the original layouts.
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts,int coverage){
  if(coverage==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary,layouts);
  if(coverage!=1||(layouts!=0&&!supportedLayout(layouts))||!matches(NATIVE_ORDINARY,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary))throw new IllegalArgumentException("Coverage requires complete native101 definitions");
  return layouts==9?CRYPT_REPLACEMENT_COMBINED:layouts==8?PYRAMID_REPLACEMENT_COMBINED:layouts==7?JUNGLE_REPLACEMENT_COMBINED:layouts==5?MANSION_PERIMETER_COMBINED:layouts==4?CRYPT_COMBINED:layouts==3?SANCTUARY_COMBINED:layouts==2?WEATHERED_COMBINED:layouts==1?COMBINED:102;
 }
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts,int coverage){
  if(coverage==0)return matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary,layouts);
  return coverage==1&&(layouts==0||supportedLayout(layouts))&&version==(layouts==9?CRYPT_REPLACEMENT_COMBINED:layouts==8?PYRAMID_REPLACEMENT_COMBINED:layouts==7?JUNGLE_REPLACEMENT_COMBINED:layouts==5?MANSION_PERIMETER_COMBINED:layouts==4?CRYPT_COMBINED:layouts==3?SANCTUARY_COMBINED:layouts==2?WEATHERED_COMBINED:layouts==1?COMBINED:102)&&matches(NATIVE_ORDINARY,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary);
 }
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts){if(layouts==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary);if((!supportedLayout(layouts))||!matches(101,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary))throw new IllegalArgumentException("Reimagined layouts require complete native101 definitions");return layouts==9?CRYPT_REPLACEMENT_LAYOUTS:layouts==8?PYRAMID_REPLACEMENT_LAYOUTS:layouts==7?JUNGLE_REPLACEMENT_LAYOUTS:layouts==5?MANSION_PERIMETER_LAYOUTS:layouts==4?CRYPT_LAYOUTS:layouts==3?SANCTUARY_LAYOUTS:layouts==2?WEATHERED_LAYOUTS:102;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts){return layouts==0?matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary):(supportedLayout(layouts))&&version==(layouts==9?CRYPT_REPLACEMENT_LAYOUTS:layouts==8?PYRAMID_REPLACEMENT_LAYOUTS:layouts==7?JUNGLE_REPLACEMENT_LAYOUTS:layouts==5?MANSION_PERIMETER_LAYOUTS:layouts==4?CRYPT_LAYOUTS:layouts==3?SANCTUARY_LAYOUTS:layouts==2?WEATHERED_LAYOUTS:102)&&matches(101,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary);}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary){if(ordinary==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies);if(ordinary!=1||!matches(100,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies))throw new IllegalArgumentException("Native ordinary structures require the complete100 feature combination");return 101;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary){return ordinary==0?matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies):ordinary==1&&version==101&&matches(100,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies);}

 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies){if(nativeBodies==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm);if(nativeBodies!=1||!matches(99,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm))throw new IllegalArgumentException("Native hut/igloo bodies require the complete99 feature combination");return 100;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies){return nativeBodies==0?matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm):nativeBodies==1&&version==100&&matches(99,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm);}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm){if(deepWarm==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep);if(deepWarm!=1||!matches(98,treasure,ship,gate,huts,snow,warm,cold,deep))throw new IllegalArgumentException("Deep warm ruins require the complete98 feature combination");return 99;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm){return deepWarm==0?matches(version,treasure,ship,gate,huts,snow,warm,cold,deep):deepWarm==1&&version==99&&matches(98,treasure,ship,gate,huts,snow,warm,cold,deep);}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep){if(deep==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold);if(deep!=1||!matches(97,treasure,ship,gate,huts,snow,warm,cold))throw new IllegalArgumentException("Deep cold ruins require the complete97 feature combination");return 98;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep){return deep==0?matches(version,treasure,ship,gate,huts,snow,warm,cold):deep==1&&version==98&&matches(97,treasure,ship,gate,huts,snow,warm,cold);}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold){if(cold==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm);if(cold!=1||!matches(96,treasure,ship,gate,huts,snow,warm))throw new IllegalArgumentException("Cold ruins require the complete96 feature combination");return 97;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold){return cold==0?matches(version,treasure,ship,gate,huts,snow,warm):cold==1&&version==97&&matches(96,treasure,ship,gate,huts,snow,warm);}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int ruins){if(ruins==0)return versionFor(legacy,treasure,ship,gate,huts,snow);if(ruins!=1||!matches(95,treasure,ship,gate,huts,snow))throw new IllegalArgumentException("Warm ruins require the complete95 feature combination");return 96;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int ruins){return ruins==0?matches(version,treasure,ship,gate,huts,snow):ruins==1&&version==96&&matches(95,treasure,ship,gate,huts,snow);}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow){if(snow==0)return versionFor(legacy,treasure,ship,gate,huts);if(snow!=1||!matches(94,treasure,ship,gate,huts))throw new IllegalArgumentException("Snow shelters require the complete94 feature combination");return 95;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow){return snow==0?matches(version,treasure,ship,gate,huts):snow==1&&version==95&&matches(94,treasure,ship,gate,huts);}

 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts){if(legacy<TerrainShape.SHAPE_VERSION||legacy>88)throw new IllegalArgumentException("Unsupported legacy structure definition");if(huts==0)return versionFor(legacy,treasure,ship,gate);if(huts!=1||!matches(93,treasure,ship,gate))throw new IllegalArgumentException("Huts require the complete discovery generation definition");return 94;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts){return huts==0?matches(version,treasure,ship,gate):huts==1&&version==94&&matches(93,treasure,ship,gate);}

 private static boolean flags(int treasure,int shipwreck,int waygate){return (treasure==0||treasure==1)&&(shipwreck==0||shipwreck==1)&&(waygate==0||waygate==1)&&(shipwreck==0||treasure==1);}
 public static int versionFor(int legacy,int treasure,int shipwreck,int waygate){
  if(legacy<TerrainShape.SHAPE_VERSION||legacy>88||!flags(treasure,shipwreck,waygate))throw new IllegalArgumentException("Unsupported structure feature combination");
  if(waygate==1)return treasure==1?(shipwreck==1?ALL_DISCOVERIES:TREASURE_WAYGATE):WAYGATE;
  return shipwreck==1?TREASURE_SHIPWRECK:treasure==1?TREASURE:legacy;
 }
 public static boolean matches(int version,int treasure,int shipwreck,int waygate){
  if(!flags(treasure,shipwreck,waygate))return false;
  if(version>=TerrainShape.SHAPE_VERSION&&version<=88)return treasure==0&&shipwreck==0&&waygate==0;
  return switch(version){
   case TREASURE->treasure==1&&shipwreck==0&&waygate==0;
   case TREASURE_SHIPWRECK->treasure==1&&shipwreck==1&&waygate==0;
   case WAYGATE->treasure==0&&shipwreck==0&&waygate==1;
   case TREASURE_WAYGATE->treasure==1&&shipwreck==0&&waygate==1;
   case ALL_DISCOVERIES->treasure==1&&shipwreck==1&&waygate==1;
   default->false;
  };
 }
}
