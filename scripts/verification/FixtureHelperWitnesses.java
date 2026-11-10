import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.function.*;
/** Standalone, framework-free witnesses for moved fixture/camera functions; never packaged. */
public class FixtureHelperWitnesses {
    public static void main(String[] args)throws Exception {
        long seed=424242;var shape=Class.forName("blockbranch.realistic.TerrainShape").getConstructor(long.class).newInstance(seed);
        var network=Class.forName("blockbranch.realistic.DrainageNetwork").getConstructor(shape.getClass(),long.class).newInstance(shape,seed);
        int success=0,failures=0;
        for(String line:Files.readAllLines(Path.of(args[1]))){
            var parts=line.split("\\t",-1);if(args.length>2&&!args[2].equals(parts[0]+":"+parts[1]))continue;var owner=Class.forName("blockbranch.realistic."+parts[0]+(args[0].equals("new")?"Fixtures":""));
            var names=parts[2].isEmpty()?new String[0]:parts[2].split(",");
            var candidates=Arrays.stream(owner.getDeclaredMethods()).filter(m->m.getName().equals(parts[1])&&m.getParameterCount()==names.length).toList();
            if(candidates.size()!=1)throw new AssertionError("Ambiguous witness "+line);
            var method=candidates.getFirst();method.setAccessible(true);var values=new Object[names.length];var types=method.getParameterTypes();
            for(int i=0;i<types.length;i++){
                var type=types[i];var name=names[i];
                if(type==network.getClass())values[i]=network;
                else if(type==long.class)values[i]=seed;
                else if(type==int.class)values[i]=name.equals("minGround")?Integer.MIN_VALUE:name.equals("maxGround")?Integer.MAX_VALUE:name.equals("kind")?1:name.equals("y")?100:name.equals("x")?-1234:name.equals("z")?-5678:1;
                else if(type==boolean.class)values[i]=false;
                else if(type==IntBinaryOperator.class)values[i]=args.length>3&&args[3].equals("cliff")?(IntBinaryOperator)(x,z)->100+Math.floorDiv(x+1234,2):(IntBinaryOperator)(x,z)->100;
                else if(type==BiPredicate.class)values[i]=(BiPredicate<Object,Object>)(x,z)->false;
                else if(type.isEnum())values[i]=type.getEnumConstants()[0];
                else throw new AssertionError("Unsupported witness parameter "+type+" in "+line);
            }
            try{var result=method.invoke(null,values);System.out.println(line+"\tRETURN\t"+result);success++;}
            catch(InvocationTargetException error){var cause=error.getCause();System.out.println(line+"\tTHROW\t"+cause.getClass().getName()+":"+cause.getMessage());failures++;}
        }
        System.err.println("Fixture witnesses completed: returns="+success+" exceptions="+failures);
    }
}
