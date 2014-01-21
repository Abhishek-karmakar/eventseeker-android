/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.net.ConnectException;
/*     */ import java.net.SocketException;
/*     */ import java.net.SocketTimeoutException;
/*     */ import java.net.UnknownHostException;

/*     */ import org.apache.http.HttpResponse;
/*     */ import org.apache.http.client.HttpRequestRetryHandler;
/*     */ import org.apache.http.client.methods.HttpUriRequest;
/*     */ import org.apache.http.impl.client.AbstractHttpClient;
/*     */ import org.apache.http.protocol.HttpContext;
/*     */ 
/*     */ class AsyncHttpRequest
/*     */   implements Runnable
/*     */ {
			private static final String TAG = AsyncHttpRequest.class.getSimpleName();

/*     */   private final AbstractHttpClient client;
/*     */   private final HttpContext context;
/*     */   private final HttpUriRequest request;
/*     */   private final AsyncHttpResponseHandler responseHandler;
/*     */   private boolean isBinaryRequest;
/*     */   private int executionCount;
/*     */ 
/*     */   public AsyncHttpRequest(AbstractHttpClient paramAbstractHttpClient, HttpContext paramHttpContext, HttpUriRequest paramHttpUriRequest, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/*  42 */     this.client = paramAbstractHttpClient;
/*  43 */     this.context = paramHttpContext;
/*  44 */     this.request = paramHttpUriRequest;
/*  45 */     this.responseHandler = paramAsyncHttpResponseHandler;
/*  46 */     if ((paramAsyncHttpResponseHandler instanceof BinaryHttpResponseHandler))
/*  47 */       this.isBinaryRequest = true;
/*     */   }
/*     */ 
/*     */   public void run()
/*     */   {
/*     */     try
/*     */     {
/*  54 */       if (this.responseHandler != null) {
/*  55 */         this.responseHandler.sendStartMessage();
/*     */       }
/*     */ 
/*  58 */       makeRequestWithRetries();
/*     */ 
/*  60 */       if (this.responseHandler != null) {
/*  61 */         this.responseHandler.sendFinishMessage();
				}
/*     */     }
/*     */     catch (IOException localIOException) {
				localIOException.printStackTrace();
/*  64 */       if (this.responseHandler != null) {
/*  65 */         this.responseHandler.sendFinishMessage();
/*  66 */         if (this.isBinaryRequest)
/*  67 */           this.responseHandler.sendFailureMessage(localIOException, (byte[])null);
/*     */         else
/*  69 */           this.responseHandler.sendFailureMessage(localIOException, (String)null);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private void makeRequest() throws IOException
/*     */   {
/*  76 */     if (!Thread.currentThread().isInterrupted())
/*     */       try {
/*  78 */         HttpResponse localHttpResponse = this.client.execute(this.request, this.context);
/*  79 */         if ((!Thread.currentThread().isInterrupted()) && 
/*  80 */           (this.responseHandler != null)) {
/*  81 */           this.responseHandler.sendResponseMessage(localHttpResponse);
/*     */         }
/*     */ 
/*     */       }
/*     */       catch (IOException localIOException)
/*     */       {
/*  87 */         if (!Thread.currentThread().isInterrupted())
/*  88 */           throw localIOException;
/*     */       }
/*     */   }
/*     */ 
/*     */   private void makeRequestWithRetries()
/*     */     throws ConnectException
/*     */   {
/*  97 */     boolean bool = true;
/*  98 */     Object localObject = null;
/*  99 */     HttpRequestRetryHandler localHttpRequestRetryHandler = this.client.getHttpRequestRetryHandler();
/* 100 */     while (bool) {
/*     */       try {
/* 102 */         makeRequest();
/* 103 */         return;
/*     */       } catch (UnknownHostException localUnknownHostException) {
					localUnknownHostException.printStackTrace();
/* 105 */         if (this.responseHandler != null) {
/* 106 */           this.responseHandler.sendFailureMessage(localUnknownHostException, "can't resolve host");
/*     */         }
/* 108 */         return;
/*     */       }
/*     */       catch (SocketException localSocketException) {
/* 111 */         if (this.responseHandler != null) {
/* 112 */           this.responseHandler.sendFailureMessage(localSocketException, "can't resolve host");
/*     */         }
/* 114 */         return;
/*     */       } catch (SocketTimeoutException localSocketTimeoutException) {
/* 116 */         if (this.responseHandler != null) {
/* 117 */           this.responseHandler.sendFailureMessage(localSocketTimeoutException, "socket time out");
/*     */         }
/* 119 */         return;
/*     */       } catch (IOException localIOException) {
/* 121 */         localObject = localIOException;
/* 122 */         bool = localHttpRequestRetryHandler.retryRequest((IOException)localObject, ++this.executionCount, this.context);
/*     */       }
/*     */       catch (NullPointerException localNullPointerException)
/*     */       {
/* 127 */         localObject = new IOException("NPE in HttpClient" + localNullPointerException.getMessage());
/* 128 */         bool = localHttpRequestRetryHandler.retryRequest((IOException)localObject, ++this.executionCount, this.context);
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 133 */     ConnectException localConnectException = new ConnectException();
/* 134 */     localConnectException.initCause((Throwable)localObject);
/* 135 */     throw localConnectException;
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.AsyncHttpRequest
 * JD-Core Version:    0.6.2
 */