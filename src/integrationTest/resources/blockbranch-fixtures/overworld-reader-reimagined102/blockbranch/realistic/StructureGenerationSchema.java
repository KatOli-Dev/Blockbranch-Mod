package blockbranch.realistic;

/** Explicit saved-feature combinations. New combinations get a new downgrade barrier. */
public final class StructureGenerationSchema {
 public static final int TREASURE=89,TREASURE_SHIPWRECK=90,WAYGATE=91,TREASURE_WAYGATE=92,ALL_DISCOVERIES=93,WETLAND_HUTS=94,SNOW_SHELTERS=95,WARM_RUINS=96,COLD_RUINS=97,DEEP_COLD_RUINS=98,DEEP_WARM_RUINS=99,NATIVE_HUT_IGLOO=100,NATIVE_ORDINARY=101,REIMAGINED=102;
 private StructureGenerationSchema(){}
 public static int versionFor(int legacy,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts){if(layouts==0)return versionFor(legacy,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary);if(layouts!=1||!matches(101,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary))throw new IllegalArgumentException("Reimagined layouts require complete native101 definitions");return 102;}
 public static boolean matches(int version,int treasure,int ship,int gate,int huts,int snow,int warm,int cold,int deep,int deepWarm,int nativeBodies,int ordinary,int layouts){return layouts==0?matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary):layouts==1&&version==102&&matches(101,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm,nativeBodies,ordinary);}
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
