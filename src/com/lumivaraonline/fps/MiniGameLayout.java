package com.lumivaraonline.fps;

import android.content.Context;
import android.graphics.Matrix;
import android.view.*;

/** Live game viewport with an explicit inverse transform for every touch pointer. */
final class MiniGameLayout extends ViewGroup {
    private boolean miniature;
    private float renderScale=1;
    void setRenderScale(float value){float next=RenderGeometry.safeScale(value);if(renderScale!=next){renderScale=next;requestLayout();}}
    private float scale=1,offsetX=0,offsetY=0;
    private final Matrix inverse=new Matrix();
    private boolean gameGesture;
    MiniGameLayout(Context context){super(context);setClipChildren(true);setClipToPadding(true);}
    void setMiniature(boolean value){if(miniature!=value){miniature=value;requestLayout();}}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=Math.max(1,MeasureSpec.getSize(widthSpec)),height=Math.max(1,MeasureSpec.getSize(heightSpec));
        setMeasuredDimension(width,height);
        float density=getResources().getDisplayMetrics().density;
        int[] virtual=miniature?FitGeometry.virtualSize(Math.round(480*density),Math.round(135*density),Math.round(810*density),width,height):new int[]{width,height};
        int[] render=RenderGeometry.size(virtual[0],virtual[1],renderScale);
        int childWidth=render[0],childHeight=render[1];
        if(getChildCount()>0)getChildAt(0).measure(MeasureSpec.makeMeasureSpec(childWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(childHeight,MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        if(getChildCount()==0)return;
        View child=getChildAt(0);int width=child.getMeasuredWidth(),height=child.getMeasuredHeight();
        child.layout(0,0,width,height);
        float[] fit=FitGeometry.fit(width,height,Math.max(1,right-left),Math.max(1,bottom-top));
        scale=fit[0];offsetX=fit[1];offsetY=fit[2];
        inverse.setTranslate(-offsetX,-offsetY);inverse.postScale(1/scale,1/scale);
        child.setPivotX(0);child.setPivotY(0);child.setScaleX(fit[0]);child.setScaleY(fit[0]);child.setTranslationX(fit[1]);child.setTranslationY(fit[2]);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(getChildCount()==0)return false;View child=getChildAt(0);
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            float x=(event.getX()-offsetX)/scale,y=(event.getY()-offsetY)/scale;
            gameGesture=x>=0&&y>=0&&x<child.getWidth()&&y<child.getHeight();
            if(gameGesture)getParent().requestDisallowInterceptTouchEvent(true);
        }
        if(!gameGesture)return false;
        if(scale==1&&offsetX==0&&offsetY==0)child.dispatchTouchEvent(event);
        else{
            MotionEvent mapped=MotionEvent.obtain(event);mapped.transform(inverse);
            // Dispatch directly so ViewGroup does not apply the same transform twice.
            try{child.dispatchTouchEvent(mapped);}finally{mapped.recycle();}
        }
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){gameGesture=false;getParent().requestDisallowInterceptTouchEvent(false);}
        return true;
    }
    @Override protected ViewGroup.LayoutParams generateDefaultLayoutParams(){return new ViewGroup.LayoutParams(-1,-1);}
}
