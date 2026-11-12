package blockbranch.realistic;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Saved-gate collision guard. A generator schema number and a vegetation flag bit each carry exactly one
 * meaning forever, because the saved tree-clear plan stores only the numeric flags and the world's saved
 * generator is what names the feature. Two features that independently chose the same number or bit would
 * merge textually yet silently reinterpret saved worlds and plans. This scans the production sources (the
 * pure classpath has no Minecraft classes) for every {@code static final int} constant, so a later merge
 * cannot add a duplicate without failing here. Scope: classes declaring both {@code BASE} and {@code VERSION}
 * (the wrapper schemas) and any {@code FLAG} constant. Older multi-number schemas and inline literals in
 * {@code vegetationFlags()} are outside it; the bits it does not name are the reserved ones below.
 */
public final class SavedGateRegistryCheck {
    /** Bit 1 (transitions) and bit 16 (polar climate) are literals in {@code RealisticGenerator.vegetationFlags()}. */
    private static final Map<Integer,String> RESERVED=Map.of(1,"biome transitions",16,"polar climate");
    private static final Pattern COMMENTS=Pattern.compile("/\\*.*?\\*/|//[^\\n]*",Pattern.DOTALL);
    private static final Pattern DECLARATION=Pattern.compile("static\\s+final\\s+int\\s+([^;(]+);");
    private static final Pattern DECLARATOR=Pattern.compile("([A-Z][A-Z0-9_]*)\\s*=\\s*(0x[0-9a-fA-F]+|\\d+(?:\\s*<<\\s*\\d+)?)");

    public static void main(String[] args)throws Exception{
        var versions=new TreeMap<Integer,String>();var flags=new TreeMap<Integer,String>();
        var root=new File(System.getProperty("blockbranch.productionSources","src/main/java"));
        var sources=new ArrayList<File>();collect(new File(root,"blockbranch"),sources);
        require(sources.size()>300,"Production sources are readable from "+root.getAbsolutePath());
        Collections.sort(sources);
        int pieceLimit=-1;
        for(var file:sources){
            var name=file.getName().replace(".java","");
            var text=COMMENTS.matcher(Files.readString(file.toPath())).replaceAll(" ");
            var constants=new HashMap<String,Integer>();
            var declaration=DECLARATION.matcher(text);
            while(declaration.find()){
                var declarator=DECLARATOR.matcher(declaration.group(1));
                while(declarator.find())constants.putIfAbsent(declarator.group(1),value(declarator.group(2)));
            }
            if(constants.containsKey("BASE")&&constants.containsKey("VERSION")){
                int b=constants.get("BASE"),v=constants.get("VERSION");
                require(b<v,name+" wraps an earlier schema ("+b+" -> "+v+")");
                var previous=versions.put(v,name);
                require(previous==null,"Saved schema "+v+" claimed by both "+previous+" and "+name);
            }
            if(constants.containsKey("FLAG")){
                int bit=constants.get("FLAG");
                require(Integer.bitCount(bit)==1,name+".FLAG is a single bit: "+bit);
                require(!RESERVED.containsKey(bit),name+".FLAG "+bit+" collides with reserved "+RESERVED.get(bit));
                var previous=flags.put(bit,name);
                require(previous==null,"Vegetation flag "+bit+" claimed by both "+previous+" and "+name);
            }
            if(name.equals("ReimaginedTreeClearPiece")){
                var limit=Pattern.compile("transitions>(\\d+)").matcher(text);
                require(limit.find(),"Tree-clear piece states its vegetation flag range");
                pieceLimit=Integer.parseInt(limit.group(1));
            }
        }
        require("BorealSlopeSchema".equals(versions.get(BorealSlopeSchema.VERSION))&&"BorealSlope".equals(flags.get(BorealSlope.FLAG)),"Scan found the boreal slope gate");
        int all=0;for(int bit:flags.keySet())all|=bit;for(int bit:RESERVED.keySet())all|=bit;
        require(pieceLimit>0&&all<=pieceLimit,"Vegetation flags "+all+" exceed the saved clear range "+pieceLimit+"; widen ReimaginedTreeClearPiece with the new bit");
        System.out.println("SAVED_GATE_REGISTRY_PASS sources="+sources.size()+" schemas="+versions+" flags="+flags+" clearFlagLimit="+pieceLimit);
    }
    private static void collect(File dir,List<File> out){
        var children=dir.listFiles();require(children!=null,"Readable source directory "+dir);
        for(var child:children)if(child.isDirectory())collect(child,out);else if(child.getName().endsWith(".java"))out.add(child);
    }
    private static int value(String literal){
        literal=literal.replace(" ","");
        if(literal.startsWith("0x"))return Integer.parseInt(literal.substring(2),16);
        int shift=literal.indexOf("<<");
        return shift<0?Integer.parseInt(literal):Integer.parseInt(literal.substring(0,shift))<<Integer.parseInt(literal.substring(shift+2));
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SavedGateRegistryCheck(){}
}
