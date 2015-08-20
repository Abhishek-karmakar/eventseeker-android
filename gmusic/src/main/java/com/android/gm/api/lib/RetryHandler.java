/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */

import android.os.SystemClock;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;

import javax.net.ssl.SSLException;

/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */ 
/*     */ class RetryHandler
/*     */   implements HttpRequestRetryHandler
/*     */ {
/*     */   private static final int RETRY_SLEEP_TIME_MILLIS = 1500;
/*  45 */   private static HashSet<Class<?>> exceptionWhitelist = new HashSet();
/*  46 */   private static HashSet<Class<?>> exceptionBlacklist = new HashSet();
/*     */   private final int maxRetries;
/*     */ 
/*     */   public RetryHandler(int paramInt)
/*     */   {
/*  65 */     this.maxRetries = paramInt;
/*     */   }
/*     */ 
/*     */   public boolean retryRequest(IOException paramIOException, int paramInt, HttpContext paramHttpContext)
/*     */   {
/*  70 */     boolean bool = true;
/*     */ 
/*  72 */     Boolean localBoolean = (Boolean)paramHttpContext.getAttribute("http.request_sent");
/*  73 */     int i = (localBoolean != null) && (localBoolean.booleanValue()) ? 1 : 0;
/*     */ 
/*  75 */     if (paramInt > this.maxRetries)
/*     */     {
/*  77 */       bool = false;
/*  78 */     } else if (isInList(exceptionBlacklist, paramIOException))
/*     */     {
/*  80 */       bool = false;
/*  81 */     } else if (isInList(exceptionWhitelist, paramIOException))
/*     */     {
/*  83 */       bool = true;
/*  84 */     } else if (i == 0)
/*     */     {
/*  86 */       bool = true;
/*     */     }
/*     */ 
/*  89 */     if (bool)
/*     */     {
/*  91 */       HttpUriRequest localHttpUriRequest = (HttpUriRequest)paramHttpContext.getAttribute("http.request");
/*  92 */       String str = localHttpUriRequest.getMethod();
/*  93 */       bool = !str.equals("POST");
/*     */     }
/*     */ 
/*  96 */     if (bool)
/*  97 */       SystemClock.sleep(1500L);
/*     */     else {
/*  99 */       paramIOException.printStackTrace();
/*     */     }
/*     */ 
/* 102 */     return bool;
/*     */   }
/*     */ 
/*     */   protected boolean isInList(HashSet<Class<?>> paramHashSet, Throwable paramThrowable) {
/* 106 */     Iterator localIterator = paramHashSet.iterator();
/* 107 */     while (localIterator.hasNext()) {
/* 108 */       if (((Class)localIterator.next()).isInstance(paramThrowable)) {
/* 109 */         return true;
/*     */       }
/*     */     }
/* 112 */     return false;
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  50 */     exceptionWhitelist.add(NoHttpResponseException.class);
/*     */ 
/*  52 */     exceptionWhitelist.add(UnknownHostException.class);
/*     */ 
/*  54 */     exceptionWhitelist.add(SocketException.class);
/*     */ 
/*  57 */     exceptionBlacklist.add(InterruptedIOException.class);
/*     */ 
/*  59 */     exceptionBlacklist.add(SSLException.class);
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.RetryHandler
 * JD-Core Version:    0.6.2
 */