/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */ import android.os.Handler;
/*     */ import android.os.Looper;
/*     */ import android.os.Message;

/*     */ import java.io.IOException;

/*     */ import org.apache.http.Header;
/*     */ import org.apache.http.HttpEntity;
/*     */ import org.apache.http.HttpResponse;
/*     */ import org.apache.http.StatusLine;
/*     */ import org.apache.http.client.HttpResponseException;
/*     */ import org.apache.http.entity.BufferedHttpEntity;
/*     */ import org.apache.http.util.EntityUtils;
/*     */ 
/*     */ public class AsyncHttpResponseHandler
/*     */ {
			private static final String TAG = AsyncHttpResponseHandler.class.getSimpleName();

/*     */   protected static final int SUCCESS_MESSAGE = 0;
/*     */   protected static final int FAILURE_MESSAGE = 1;
/*     */   protected static final int START_MESSAGE = 2;
/*     */   protected static final int FINISH_MESSAGE = 3;
/*     */   private Handler handler;
/*     */ 
/*     */   public AsyncHttpResponseHandler()
/*     */   {
/*  85 */     if (Looper.myLooper() != null)
/*  86 */       this.handler = new Handler()
/*     */       {
/*     */         public void handleMessage(Message paramAnonymousMessage) {
/*  89 */           AsyncHttpResponseHandler.this.handleMessage(paramAnonymousMessage);
/*     */         }
/*     */       };
/*     */   }
/*     */ 
/*     */   public void onStart()
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onFinish()
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onSuccess(String paramString)
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, Header[] paramArrayOfHeader, String paramString)
/*     */   {
/* 123 */     onSuccess(paramInt, paramString);
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, String paramString)
/*     */   {
/* 133 */     onSuccess(paramString);
/*     */   }
/*     */ 
/*     */   @Deprecated
/*     */   public void onFailure(Throwable paramThrowable)
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onFailure(Throwable paramThrowable, String paramString)
/*     */   {
/* 151 */     onFailure(paramThrowable);
/*     */   }
/*     */ 
/*     */   protected void sendSuccessMessage(int paramInt, Header[] paramArrayOfHeader, String paramString)
/*     */   {
/* 160 */     sendMessage(obtainMessage(0, new Object[] { new Integer(paramInt), paramArrayOfHeader, paramString }));
/*     */   }
/*     */ 
/*     */   protected void sendFailureMessage(Throwable paramThrowable, String paramString) {
/* 164 */     sendMessage(obtainMessage(1, new Object[] { paramThrowable, paramString }));
/*     */   }
/*     */ 
/*     */   protected void sendFailureMessage(Throwable paramThrowable, byte[] paramArrayOfByte) {
/* 168 */     sendMessage(obtainMessage(1, new Object[] { paramThrowable, paramArrayOfByte }));
/*     */   }
/*     */ 
/*     */   protected void sendStartMessage() {
/* 172 */     sendMessage(obtainMessage(2, null));
/*     */   }
/*     */ 
/*     */   protected void sendFinishMessage() {
/* 176 */     sendMessage(obtainMessage(3, null));
/*     */   }
/*     */ 
/*     */   protected void handleSuccessMessage(int paramInt, Header[] paramArrayOfHeader, String paramString)
/*     */   {
/* 185 */     onSuccess(paramInt, paramArrayOfHeader, paramString);
/*     */   }
/*     */ 
/*     */   protected void handleFailureMessage(Throwable paramThrowable, String paramString) {
/* 189 */     onFailure(paramThrowable, paramString);
/*     */   }
/*     */ 
/*     */   protected void handleMessage(Message paramMessage)
/*     */   {
/*     */     Object[] arrayOfObject;
/* 198 */     switch (paramMessage.what) {
/*     */     case 0:
/* 200 */       arrayOfObject = (Object[])paramMessage.obj;
/* 201 */       handleSuccessMessage(((Integer)arrayOfObject[0]).intValue(), (Header[])arrayOfObject[1], (String)arrayOfObject[2]);
/* 202 */       break;
/*     */     case 1:
/* 204 */       arrayOfObject = (Object[])paramMessage.obj;
/* 205 */       handleFailureMessage((Throwable)arrayOfObject[0], (String)arrayOfObject[1]);
/* 206 */       break;
/*     */     case 2:
/* 208 */       onStart();
/* 209 */       break;
/*     */     case 3:
/* 211 */       onFinish();
/*     */     }
/*     */   }
/*     */ 
/*     */   protected void sendMessage(Message paramMessage)
/*     */   {
/* 217 */     if (this.handler != null)
/* 218 */       this.handler.sendMessage(paramMessage);
/*     */     else
/* 220 */       handleMessage(paramMessage);
/*     */   }
/*     */ 
/*     */   protected Message obtainMessage(int paramInt, Object paramObject)
/*     */   {
/* 225 */     Message localMessage = null;
/* 226 */     if (this.handler != null) {
/* 227 */       localMessage = this.handler.obtainMessage(paramInt, paramObject);
/*     */     } else {
/* 229 */       localMessage = Message.obtain();
/* 230 */       localMessage.what = paramInt;
/* 231 */       localMessage.obj = paramObject;
/*     */     }
/* 233 */     return localMessage;
/*     */   }
/*     */ 
/*     */   void sendResponseMessage(HttpResponse paramHttpResponse)
/*     */   {
/* 238 */     StatusLine localStatusLine = paramHttpResponse.getStatusLine();
/* 239 */     String str = null;
/*     */     try {
/* 241 */       BufferedHttpEntity localBufferedHttpEntity = null;
/* 242 */       HttpEntity localHttpEntity = paramHttpResponse.getEntity();
/* 243 */       if (localHttpEntity != null) {
/* 244 */         localBufferedHttpEntity = new BufferedHttpEntity(localHttpEntity);
/* 245 */         str = EntityUtils.toString(localBufferedHttpEntity, "UTF-8");
/*     */       }
/*     */     } catch (IOException localIOException) {
/* 248 */       sendFailureMessage(localIOException, (String)null);
/*     */     }

/* 251 */     if (localStatusLine.getStatusCode() >= 300) {
/* 252 */       sendFailureMessage(new HttpResponseException(localStatusLine.getStatusCode(), localStatusLine.getReasonPhrase()), str);
			}
/*     */     else {
/* 254 */       sendSuccessMessage(localStatusLine.getStatusCode(), paramHttpResponse.getAllHeaders(), str);
				}
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.AsyncHttpResponseHandler
 * JD-Core Version:    0.6.2
 */