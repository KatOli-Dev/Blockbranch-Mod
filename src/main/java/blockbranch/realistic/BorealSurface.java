package blockbranch.realistic;

/** Append-only surface roles; saved codes 3/6 retain their historical moss cubes. */
public final class BorealSurface {
    public static final int SOIL=15, CARPET=16, LAST=CARPET;
    static int fresh(int legacy){return legacy==3?SOIL:legacy==6?CARPET:legacy;}
    public static boolean mossSite(int code){return code==SOIL||code==CARPET;}
    private BorealSurface(){}
}
