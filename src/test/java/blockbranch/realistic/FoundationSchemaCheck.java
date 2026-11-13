package blockbranch.realistic;

/** Exhaustive pure check of the combined continental-ice / leaf-litter saved-schema identity. */
public final class FoundationSchemaCheck {
    public static void main(String[] args){
        // Every (ice, litter) pair has exactly one number, read only under exactly those definitions.
        int[][] pairs={{0,0},{1,0},{0,1},{1,1}};int[] numbers={264,265,266,267};
        for(int i=0;i<4;i++){
            int ice=pairs[i][0],litter=pairs[i][1];
            require(FoundationSchema.schema(264,ice,litter)==numbers[i],"Number for "+ice+"/"+litter);
            require(FoundationSchema.baseSchema(numbers[i],ice,litter)==264&&FoundationSchema.matches(numbers[i],ice,litter),"Read "+numbers[i]);
            for(int saved=82;saved<=276;saved++){
                boolean expected=saved==numbers[i]||(ice==0&&litter==0&&(saved<265||saved>267)); // absent keeps every earlier/later number for later layers
                require(FoundationSchema.matches(saved,ice,litter)==expected,"Saved "+saved+" under ice="+ice+" litter="+litter);
                if(ice+litter>0&&FoundationSchema.matches(saved,ice,litter))require(FoundationSchema.baseSchema(saved,ice,litter)==264,"Complete 264 foundation");
                if(ice==0&&litter==0&&expected)require(FoundationSchema.baseSchema(saved,0,0)==saved,"Absent definitions pass the saved number to later layers unchanged");
            }
        }
        // Agreement with the single-definition classes where they apply.
        require(FoundationSchema.schema(264,1,0)==265&&FoundationSchema.schema(264,0,1)==266&&FoundationSchema.schema(264,1,1)==267&&FoundationSchema.schema(264,0,0)==264,"Published 265 stays polar only; litter only is 266; both is 267");
        // Later layers cannot be promoted into either wrapper, and unknown definitions reject.
        for(int base:new int[]{82,257,263,265,266,267,268}){expect(()->FoundationSchema.schema(base,1,0));expect(()->FoundationSchema.schema(base,0,1));expect(()->FoundationSchema.schema(base,1,1));}
        expect(()->FoundationSchema.schema(264,2,0));expect(()->FoundationSchema.schema(264,0,-1));
        require(!FoundationSchema.matches(264,2,0)&&!FoundationSchema.matches(267,0,2),"Unknown definitions never match");
        System.out.println("PASS foundation schema identity: 265 ice only, 266 litter only, 267 both; 4 definition pairs x 189 saved numbers");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void expect(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Expected rejection");}
    private FoundationSchemaCheck(){}
}
