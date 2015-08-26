/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */

import android.content.Context;
import android.os.Message;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

/*     */
/*     */
/*     */
/*     */
/*     */
/*     */ 
/*     */ public abstract class SyncHttpClient extends AsyncHttpClient
/*     */ {
			protected static final String TAG = SyncHttpClient.class.getSimpleName();

/*     */   private int responseCode;
/*     */   protected String result;
/*  18 */   protected AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler()
/*     */   {
/*     */     void sendResponseMessage(HttpResponse paramAnonymousHttpResponse)
/*     */     {
/*  22 */       SyncHttpClient.this.responseCode = paramAnonymousHttpResponse.getStatusLine().getStatusCode();
/*  23 */       super.sendResponseMessage(paramAnonymousHttpResponse);
/*     */     }
/*     */ 
/*     */     protected void sendMessage(Message paramAnonymousMessage)
/*     */     {
/*  32 */       handleMessage(paramAnonymousMessage);
/*     */     }
/*     */ 
/*     */     public void onSuccess(String paramAnonymousString)
/*     */     {
/*  37 */       SyncHttpClient.this.result = paramAnonymousString;
/*     */     }
/*     */ 
/*     */     public void onFailure(Throwable paramAnonymousThrowable, String paramAnonymousString)
/*     */     {
/*  42 */       SyncHttpClient.this.result = SyncHttpClient.this.onRequestFailed(paramAnonymousThrowable, paramAnonymousString);
/*     */     }
/*  18 */   };
/*     */ 
/*     */   public int getResponseCode()
/*     */   {
/*  51 */     return this.responseCode;
/*     */   }
/*     */ 
/*     */   protected void sendRequest(DefaultHttpClient paramDefaultHttpClient, HttpContext paramHttpContext, HttpUriRequest paramHttpUriRequest, String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler, Context paramContext)
/*     */   {
/*  60 */     if (paramString != null) {
/*  61 */       paramHttpUriRequest.addHeader("Content-Type", paramString);
/*     */     }
/*     */ 
/*  67 */     new AsyncHttpRequest(paramDefaultHttpClient, paramHttpContext, paramHttpUriRequest, paramAsyncHttpResponseHandler).run();
/*     */   }
/*     */ 
/*     */   public abstract String onRequestFailed(Throwable paramThrowable, String paramString);
/*     */ 
/*     */   public void delete(String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/*  76 */     delete(paramString, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public String get(String paramString, RequestParams paramRequestParams) {
/*  80 */     get(paramString, paramRequestParams, this.responseHandler);
/*     */ 
/*  85 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String get(String paramString) {
/*  89 */     get(paramString, null, this.responseHandler);
/*  90 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String put(String paramString, RequestParams paramRequestParams) {
/*  94 */     put(paramString, paramRequestParams, this.responseHandler);
/*  95 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String put(String paramString) {
/*  99 */     put(paramString, null, this.responseHandler);
/* 100 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String post(String paramString, RequestParams paramRequestParams) {
/* 104 */     post(paramString, paramRequestParams, this.responseHandler);
/* 105 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String post(String paramString) {
/* 109 */     post(paramString, null, this.responseHandler);
/* 110 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String delete(String paramString, RequestParams paramRequestParams) {
/* 114 */     delete(paramString, paramRequestParams, this.responseHandler);
/* 115 */     return this.result;
/*     */   }
/*     */ 
/*     */   public String delete(String paramString) {
/* 119 */     delete(paramString, null, this.responseHandler);
/* 120 */     return this.result;
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.SyncHttpClient
 * JD-Core Version:    0.6.2
 */