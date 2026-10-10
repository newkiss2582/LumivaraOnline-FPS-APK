package com.lumivaraonline.fps;

/** Select a real mode without changing physical resolution. Zero cap means maximum. */
public final class DisplayRefreshPolicy {
    private DisplayRefreshPolicy(){}
    public static int choose(int width,int height,int cap,int[][] modes,float[] rates){
        if(modes==null||rates==null||modes.length!=rates.length)return -1;
        int best=-1,lowest=-1;float bestRate=0,lowestRate=Float.MAX_VALUE;
        for(int i=0;i<modes.length;i++){
            if(modes[i]==null||modes[i].length<3||modes[i][1]!=width||modes[i][2]!=height||!Float.isFinite(rates[i])||rates[i]<=0)continue;
            if(rates[i]<lowestRate){lowest=i;lowestRate=rates[i];}
            if((cap==0||rates[i]<=cap+0.5f)&&rates[i]>bestRate){best=i;bestRate=rates[i];}
        }
        return best>=0?best:lowest;
    }
}
