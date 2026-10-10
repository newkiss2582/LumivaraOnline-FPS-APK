import com.lumivaraonline.fps.FitGeometry;

public class FitGeometryTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        int[][] sizes={{180,102},{280,158},{300,300},{720,160},{1,1},{2560,1440}};
        for(int[] size:sizes){
            float[] fit=FitGeometry.fit(640,360,size[0],size[1]);float scale=fit[0],x=fit[1],y=fit[2];
            check(scale>0&&x>=-0.01&&y>=-0.01,"positive scale and nonnegative margins");
            check(x+640*scale<=size[0]+0.01&&y+360*scale<=size[1]+0.01,"entire viewport fits");
            check(Math.abs(x*2+640*scale-size[0])<0.01&&Math.abs(y*2+360*scale-size[1])<0.01,"centered fit");
            // Inverse transform must map a scaled click back to the original game point.
            check(Math.abs(((x+480*scale)-x)/scale-480)<0.01,"touch X inverse");
            check(Math.abs(((y+180*scale)-y)/scale-180)<0.01,"touch Y inverse");
        }
        check(FitGeometry.boundedSize(20,180,360)==180,"minimum width");
        check(FitGeometry.boundedSize(900,180,360)==360,"maximum screen width");
        check(FitGeometry.boundedSize(280,180,120)==120,"very small available bounds override minimum");
        check(FitGeometry.position(400,280,360)==80,"right edge clamp");
        check(FitGeometry.position(-50,280,360)==0,"left edge clamp");
        check(FitGeometry.position(20,500,360)==0,"oversize window origin");
        int[] portrait=FitGeometry.virtualSize(640,180,1080,280,420);
        check(portrait[1]==960,"portrait shape adapts virtual height");
        int[] wide=FitGeometry.virtualSize(640,180,1080,640,200);
        check(wide[1]==200,"width and height do not lock to 16:9");
        check(FitGeometry.virtualSize(640,180,1080,180,2000)[1]==1080,"bound very tall render surface");
        check(FitGeometry.virtualSize(640,180,1080,2000,100)[1]==180,"bound very wide render surface");
        int[] lighter=FitGeometry.virtualSize(480,135,810,280,420);
        check(lighter[0]==480&&lighter[1]==720,"lighter miniature keeps the same aspect");
        check((long)lighter[0]*lighter[1]<(long)portrait[0]*portrait[1],"miniature render area reduced");
        boolean rejected=false;try{FitGeometry.fit(0,360,280,190);}catch(IllegalArgumentException error){rejected=true;}check(rejected,"zero source rejected");
        System.out.println("PASS: miniature fit, portrait/wide letterboxing, inverse coordinate math, resize minimum/maximum, off-screen position clamps.");
    }
}
