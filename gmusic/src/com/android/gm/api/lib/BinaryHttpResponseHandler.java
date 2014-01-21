/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */ import android.os.Message;
/*     */ import java.io.IOException;
/*     */ import java.util.regex.Pattern;
/*     */ import org.apache.http.Header;
/*     */ import org.apache.http.HttpEntity;
/*     */ import org.apache.http.HttpResponse;
/*     */ import org.apache.http.StatusLine;
/*     */ import org.apache.http.client.HttpResponseException;
/*     */ import org.apache.http.entity.BufferedHttpEntity;
/*     */ import org.apache.http.util.EntityUtils;
/*     */ 
/*     */ public class BinaryHttpResponseHandler extends AsyncHttpResponseHandler
/*     */ {
/*  60 */   private static String[] mAllowedContentTypes = { "image/jpeg", "image/png" };
/*     */ 
/*     */   public BinaryHttpResponseHandler()
/*     */   {
/*     */   }
/*     */ 
/*     */   public BinaryHttpResponseHandler(String[] paramArrayOfString)
/*     */   {
/*  77 */     this();
/*  78 */     mAllowedContentTypes = paramArrayOfString;
/*     */   }
/*     */ 
/*     */   public void onSuccess(byte[] paramArrayOfByte)
/*     */   {
/*     */   }
/*     */ 
/*     */   public void onSuccess(int paramInt, byte[] paramArrayOfByte)
/*     */   {
/*  98 */     onSuccess(paramArrayOfByte);
/*     */   }
/*     */ 
/*     */   @Deprecated
/*     */   public void onFailure(Throwable paramThrowable, byte[] paramArrayOfByte)
/*     */   {
/* 110 */     onFailure(paramThrowable);
/*     */   }
/*     */ 
/*     */   protected void sendSuccessMessage(int paramInt, byte[] paramArrayOfByte)
/*     */   {
/* 119 */     sendMessage(obtainMessage(0, new Object[] { Integer.valueOf(paramInt), paramArrayOfByte }));
/*     */   }
/*     */ 
/*     */   protected void sendFailureMessage(Throwable paramThrowable, byte[] paramArrayOfByte)
/*     */   {
/* 124 */     sendMessage(obtainMessage(1, new Object[] { paramThrowable, paramArrayOfByte }));
/*     */   }
/*     */ 
/*     */   protected void handleSuccessMessage(int paramInt, byte[] paramArrayOfByte)
/*     */   {
/* 132 */     onSuccess(paramInt, paramArrayOfByte);
/*     */   }
/*     */ 
/*     */   protected void handleFailureMessage(Throwable paramThrowable, byte[] paramArrayOfByte) {
/* 136 */     onFailure(paramThrowable, paramArrayOfByte);
/*     */   }
/*     */ 
/*     */   protected void handleMessage(Message paramMessage)
/*     */   {
/*     */     Object[] arrayOfObject;
/* 143 */     switch (paramMessage.what) {
/*     */     case 0:
/* 145 */       arrayOfObject = (Object[])paramMessage.obj;
/* 146 */       handleSuccessMessage(((Integer)arrayOfObject[0]).intValue(), (byte[])arrayOfObject[1]);
/* 147 */       break;
/*     */     case 1:
/* 149 */       arrayOfObject = (Object[])paramMessage.obj;
/* 150 */       handleFailureMessage((Throwable)arrayOfObject[0], arrayOfObject[1].toString());
/* 151 */       break;
/*     */     default:
/* 153 */       super.handleMessage(paramMessage);
/*     */     }
/*     */   }
/*     */ 
/*     */   void sendResponseMessage(HttpResponse paramHttpResponse)
/*     */   {
/* 161 */     StatusLine localStatusLine = paramHttpResponse.getStatusLine();
/* 162 */     Header[] arrayOfHeader = paramHttpResponse.getHeaders("Content-Type");
/* 163 */     byte[] arrayOfByte = null;
/* 164 */     if (arrayOfHeader.length != 1)
/*     */     {
/* 166 */       sendFailureMessage(new HttpResponseException(localStatusLine.getStatusCode(), "None, or more than one, Content-Type Header found!"), arrayOfByte);
/* 167 */       return;
/*     */     }
/* 169 */     Header localHeader = arrayOfHeader[0];
/* 170 */     int i = 0;
/* 171 */     for (String str : mAllowedContentTypes) {
/* 172 */       if (Pattern.matches(str, localHeader.getValue())) {
/* 173 */         i = 1;
/*     */       }
/*     */     }
/* 176 */     if (i == 0)
/*     */     {
/* 178 */       sendFailureMessage(new HttpResponseException(localStatusLine.getStatusCode(), "Content-Type not allowed!"), arrayOfByte);
/* 179 */       return;
/*     */     }
/*     */     try {
/* 182 */       BufferedHttpEntity bufferedHttpEntity = null;
/* 183 */       HttpEntity localHttpEntity = paramHttpResponse.getEntity();
/* 184 */       if (localHttpEntity != null) {
/* 185 */         bufferedHttpEntity = new BufferedHttpEntity(localHttpEntity);
/*     */       }
/* 187 */       arrayOfByte = EntityUtils.toByteArray((HttpEntity)bufferedHttpEntity);
/*     */     } catch (IOException localIOException) {
/* 189 */       sendFailureMessage(localIOException, (byte[])null);
/*     */     }
/*     */ 
/* 192 */     if (localStatusLine.getStatusCode() >= 300)
/* 193 */       sendFailureMessage(new HttpResponseException(localStatusLine.getStatusCode(), localStatusLine.getReasonPhrase()), arrayOfByte);
/*     */     else
/* 195 */       sendSuccessMessage(localStatusLine.getStatusCode(), arrayOfByte);
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.BinaryHttpResponseHandler
 * JD-Core Version:    0.6.2
 */