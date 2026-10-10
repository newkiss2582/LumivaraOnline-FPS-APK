package com.lumivaraonline.fps;

public final class RenderGeometry {
    private RenderGeometry(){}
    public static float safeScale(float value){return Float.isFinite(value)?Math.max(.7f,Math.min(1,value)):1;}
    public static int[] size(int width,int height,float scale){float s=safeScale(scale);return new int[]{Math.max(1,Math.round(width*s)),Math.max(1,Math.round(height*s))};}
}
