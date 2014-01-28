/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */ import android.os.Message;
/*     */ import org.apache.http.Header;
/*     */ import org.json.JSONArray;
/*     */ import org.json.JSONException;
/*     */ import org.json.JSONObject;
/*     */ import org.json.JSONTokener;
/*     */ 
/*     */ public class JsonHttpResponseHandler extends AsyncHttpResponseHandler
/*     */ {
/*     */   protected static final int SUCCESS_JSON_MESSAGE = 100;
/*     */ 
/*     */   public void onSuccess(JSONObject paramJSONObject)
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onSuccess(JSONArray paramJSONArray)
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, Header[] paramArrayOfHeader, JSONObject paramJSONObject)
/*     */   {
/*  74 */     onSuccess(paramInt, paramJSONObject);
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, JSONObject paramJSONObject)
/*     */   {
/*  85 */     onSuccess(paramJSONObject);
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, Header[] paramArrayOfHeader, JSONArray paramJSONArray)
/*     */   {
/*  97 */     onSuccess(paramInt, paramJSONArray);
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, JSONArray paramJSONArray)
/*     */   {
/* 108 */     onSuccess(paramJSONArray);
/*     */   }
/*     */ 
/*     */   public void onFailure(Throwable paramThrowable, JSONObject paramJSONObject)
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onFailure(Throwable paramThrowable, JSONArray paramJSONArray)
/*     */   {
/*     */   }
/*     */ 
/*     */   protected void sendSuccessMessage(int paramInt, Header[] paramArrayOfHeader, String paramString)
/*     */   {
/* 121 */     if (paramInt != 204)
/*     */       try {
/* 123 */         Object localObject = parseResponse(paramString);
/* 124 */         sendMessage(obtainMessage(100, new Object[] { Integer.valueOf(paramInt), paramArrayOfHeader, localObject }));
/*     */       } catch (JSONException localJSONException) {
/* 126 */         sendFailureMessage(localJSONException, paramString);
/*     */       }
/*     */     else
/* 129 */       sendMessage(obtainMessage(100, new Object[] { Integer.valueOf(paramInt), new JSONObject() }));
/*     */   }
/*     */ 
/*     */   protected void handleMessage(Message paramMessage)
/*     */   {
/* 140 */     switch (paramMessage.what) {
/*     */     case 100:
/* 142 */       Object[] arrayOfObject = (Object[])paramMessage.obj;
/* 143 */       handleSuccessJsonMessage(((Integer)arrayOfObject[0]).intValue(), (Header[])arrayOfObject[1], arrayOfObject[2]);
/* 144 */       break;
/*     */     default:
/* 146 */       super.handleMessage(paramMessage);
/*     */     }
/*     */   }
/*     */ 
/*     */   protected void handleSuccessJsonMessage(int paramInt, Header[] paramArrayOfHeader, Object paramObject) {
/* 151 */     if ((paramObject instanceof JSONObject))
/* 152 */       onSuccess(paramInt, paramArrayOfHeader, (JSONObject)paramObject);
/* 153 */     else if ((paramObject instanceof JSONArray))
/* 154 */       onSuccess(paramInt, paramArrayOfHeader, (JSONArray)paramObject);
/*     */     else
/* 156 */       onFailure(new JSONException("Unexpected type " + paramObject.getClass().getName()), (JSONObject)null);
/*     */   }
/*     */ 
/*     */   protected Object parseResponse(String paramString) throws JSONException
/*     */   {
/* 161 */     Object localObject = null;
/*     */ 
/* 163 */     paramString = paramString.trim();
/* 164 */     if ((paramString.startsWith("{")) || (paramString.startsWith("["))) {
/* 165 */       localObject = new JSONTokener(paramString).nextValue();
/*     */     }
/* 167 */     if (localObject == null) {
/* 168 */       localObject = paramString;
/*     */     }
/* 170 */     return localObject;
/*     */   }
/*     */ 
/*     */   protected void handleFailureMessage(Throwable paramThrowable, String paramString)
/*     */   {
/*     */     try {
/* 176 */       if (paramString != null) {
/* 177 */         Object localObject = parseResponse(paramString);
/* 178 */         if ((localObject instanceof JSONObject))
/* 179 */           onFailure(paramThrowable, (JSONObject)localObject);
/* 180 */         else if ((localObject instanceof JSONArray))
/* 181 */           onFailure(paramThrowable, (JSONArray)localObject);
/*     */         else
/* 183 */           onFailure(paramThrowable, paramString);
/*     */       }
/*     */       else {
/* 186 */         onFailure(paramThrowable, "");
/*     */       }
/*     */     } catch (JSONException localJSONException) {
/* 189 */       onFailure(paramThrowable, paramString);
/*     */     }
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.JsonHttpResponseHandler
 * JD-Core Version:    0.6.2
 */