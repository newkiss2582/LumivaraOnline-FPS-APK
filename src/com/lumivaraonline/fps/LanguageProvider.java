package com.lumivaraonline.fps;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Private, read-only IPC for the app menu preference; no game data crosses processes. */
public class LanguageProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(!"language".equals(method))throw new IllegalArgumentException();
        Bundle result=new Bundle();result.putString("language",AppLanguage.preferences(getContext()).getString(AppLanguage.KEY,LanguageChoice.DEFAULT));return result;
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){throw new UnsupportedOperationException();}
    @Override public String getType(Uri uri){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
