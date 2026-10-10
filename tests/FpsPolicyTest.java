import com.lumivaraonline.fps.*;

public class FpsPolicyTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        int[][] modes={{1,1080,2400},{2,1080,2400},{3,1080,2400},{4,1440,3200},{5,1080,2400}};
        float[] rates={60,90,120,165,144};
        check(DisplayRefreshPolicy.choose(1080,2400,0,modes,rates)==4,"max must exclude higher refresh at another resolution");
        check(DisplayRefreshPolicy.choose(1080,2400,120,modes,rates)==2,"120 limit");
        check(DisplayRefreshPolicy.choose(1080,2400,60,modes,rates)==0,"60 limit");
        check(DisplayRefreshPolicy.choose(720,1600,0,modes,rates)==-1,"unknown resolution leaves system preference");
        check(DisplayRefreshPolicy.choose(1080,2400,60,new int[][]{{1,1080,2400}},new float[]{90})==0,"only supported 90 mode");
        check(DisplayRefreshPolicy.choose(1080,2400,0,modes,new float[]{Float.NaN,0,-1,165,Float.POSITIVE_INFINITY})==-1,"invalid rates ignored");
        check(DisplayRefreshPolicy.choose(1080,2400,0,null,null)==-1,"missing display data");
        for(int sdk:new int[]{26,28,33,36})check(ProfileIdentity.supportedCount(sdk)==1,"one screen on every supported API");
        try{ProfileIdentity.checked(1);throw new AssertionError("second slot accepted");}catch(IllegalArgumentException expected){}
        try{ProfileIdentity.fromProcess("com.lumivaraonline.fps","com.lumivaraonline.fps:profile1");throw new AssertionError("extra profile process accepted");}catch(IllegalArgumentException expected){}
        for(float scale:new float[]{1,.85f,.7f})for(int[] viewport:new int[][]{{2400,1080},{1080,2400},{280,158},{1,1}}){
            int[] render=RenderGeometry.size(viewport[0],viewport[1],scale);
            check(render[0]>0&&render[1]>0&&render[0]<=viewport[0]&&render[1]<=viewport[1],"bounded render size");
            float[] fit=FitGeometry.fit(render[0],render[1],viewport[0],viewport[1]);
            // Real render sizes fed into the production fit and inverse touch mapping.
            float x=render[0]*.72f,y=render[1]*.38f;
            check(Math.abs(((x*fit[0]+fit[1])-fit[1])/fit[0]-x)<.001f,"scaled touch X");
            check(Math.abs(((y*fit[0]+fit[2])-fit[2])/fit[0]-y)<.001f,"scaled touch Y");
        }
        check(RenderGeometry.safeScale(Float.NaN)==1,"invalid scale safe");
        System.out.println("PASS: supported display rate selection, unchanged resolution, one-screen identity, render size and inverse touch geometry.");
    }
}
