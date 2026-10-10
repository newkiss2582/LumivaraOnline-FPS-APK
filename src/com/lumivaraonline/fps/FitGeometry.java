package com.lumivaraonline.fps;

/** Uniform fit with letterboxing: never stretch or crop the game's viewport. */
public final class FitGeometry {
    private FitGeometry(){}
    public static float[] fit(int sourceWidth,int sourceHeight,int width,int height){
        if(sourceWidth<=0||sourceHeight<=0||width<=0||height<=0)throw new IllegalArgumentException("Positive dimensions required");
        float scale=Math.min((float)width/sourceWidth,(float)height/sourceHeight);
        return new float[]{scale,(width-sourceWidth*scale)/2f,(height-sourceHeight*scale)/2f};
    }
    public static int boundedSize(int requested,int minimum,int available){return Math.max(1,Math.min(Math.max(requested,minimum),Math.max(1,available)));}
    public static int position(int requested,int size,int available){return Math.max(0,Math.min(requested,Math.max(0,available-size)));}
    public static int[] virtualSize(int width,int minHeight,int maxHeight,int availableWidth,int availableHeight){
        if(width<=0||minHeight<=0||maxHeight<minHeight||availableWidth<=0||availableHeight<=0)throw new IllegalArgumentException("Positive viewport dimensions required");
        int height=(int)Math.ceil((double)width*availableHeight/availableWidth);
        return new int[]{width,Math.max(minHeight,Math.min(maxHeight,height))};
    }
}
