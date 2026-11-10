package blockbranch.realistic;

/** Immutable per-native-piece opt-in, configured only after the native layout is complete. */
public interface MansionReplacementTemplateAccess {
    void blockbranch$configureMansionReplacement(int definition,int floor);
    int blockbranch$mansionReplacementDefinition();
    int blockbranch$mansionReplacementFloor();
}
