/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */

import android.content.Context;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.GZIPInputStream;

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
/*     */ 
/*     */ public class AsyncHttpClient
/*     */ {
			private static final String TAG = AsyncHttpClient.class.getSimpleName();

/*     */   private static final String VERSION = "1.4.3";
/*     */   private static final int DEFAULT_MAX_CONNECTIONS = 10;
/*     */   private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
/*     */   private static final int DEFAULT_MAX_RETRIES = 5;
/*     */   private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;
/*     */   private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
/*     */   private static final String ENCODING_GZIP = "gzip";
/* 102 */   private static int maxConnections = 10;
/* 103 */   private static int socketTimeout = 10000;
/*     */   private final DefaultHttpClient httpClient;
/*     */   private final HttpContext httpContext;
/*     */   private ThreadPoolExecutor threadPool;
/*     */   private final Map<Context, List<WeakReference<Future<?>>>> requestMap;
/*     */   private final Map<String, String> clientHeaderMap;
/*     */ 
/*     */   public AsyncHttpClient()
/*     */   {
/* 116 */     BasicHttpParams localBasicHttpParams = new BasicHttpParams();
/*     */ 
/* 118 */     ConnManagerParams.setTimeout(localBasicHttpParams, socketTimeout);
/* 119 */     ConnManagerParams.setMaxConnectionsPerRoute(localBasicHttpParams, new ConnPerRouteBean(maxConnections));
/* 120 */     ConnManagerParams.setMaxTotalConnections(localBasicHttpParams, 10);
/*     */ 
/* 122 */     HttpConnectionParams.setSoTimeout(localBasicHttpParams, socketTimeout);
/* 123 */     HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, socketTimeout);
/* 124 */     HttpConnectionParams.setTcpNoDelay(localBasicHttpParams, true);
/* 125 */     HttpConnectionParams.setSocketBufferSize(localBasicHttpParams, 8192);
/*     */ 
/* 127 */     HttpProtocolParams.setVersion(localBasicHttpParams, HttpVersion.HTTP_1_1);
/* 128 */     HttpProtocolParams.setUserAgent(localBasicHttpParams, String.format("android-async-http/%s (http://loopj.com/android-async-http)", new Object[] { "1.4.3" }));
/*     */ 
/* 130 */     SchemeRegistry localSchemeRegistry = new SchemeRegistry();
/* 131 */     localSchemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
/* 132 */     localSchemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
/* 133 */     ThreadSafeClientConnManager localThreadSafeClientConnManager = new ThreadSafeClientConnManager(localBasicHttpParams, localSchemeRegistry);
/*     */ 
/* 135 */     this.httpContext = new SyncBasicHttpContext(new BasicHttpContext());
/* 136 */     this.httpClient = new DefaultHttpClient(localThreadSafeClientConnManager, localBasicHttpParams);
/* 137 */     this.httpClient.addRequestInterceptor(new HttpRequestInterceptor()
/*     */     {
/*     */       public void process(HttpRequest paramAnonymousHttpRequest, HttpContext paramAnonymousHttpContext) {
/* 140 */         if (!paramAnonymousHttpRequest.containsHeader("Accept-Encoding")) {
/* 141 */           paramAnonymousHttpRequest.addHeader("Accept-Encoding", "gzip");
/*     */         }
/* 143 */         for (String str : AsyncHttpClient.this.clientHeaderMap.keySet())
/* 144 */           paramAnonymousHttpRequest.addHeader(str, (String)AsyncHttpClient.this.clientHeaderMap.get(str));
/*     */       }
/*     */     });
/* 149 */     this.httpClient.addResponseInterceptor(new HttpResponseInterceptor()
/*     */     {
/*     */       public void process(HttpResponse paramAnonymousHttpResponse, HttpContext paramAnonymousHttpContext) {
/* 152 */         HttpEntity localHttpEntity = paramAnonymousHttpResponse.getEntity();
/* 153 */         if (localHttpEntity == null) {
/* 154 */           return;
/*     */         }
/* 156 */         Header localHeader = localHttpEntity.getContentEncoding();
/* 157 */         if (localHeader != null)
/* 158 */           for (HeaderElement localHeaderElement : localHeader.getElements())
/* 159 */             if (localHeaderElement.getName().equalsIgnoreCase("gzip")) {
/* 160 */               paramAnonymousHttpResponse.setEntity(new AsyncHttpClient.InflatingEntity(paramAnonymousHttpResponse.getEntity()));
/* 161 */               break;
/*     */             }
/*     */       }
/*     */     });
/* 168 */     this.httpClient.setHttpRequestRetryHandler(new RetryHandler(5));
/*     */ 
/* 170 */     this.threadPool = ((ThreadPoolExecutor)Executors.newCachedThreadPool());
/*     */ 
/* 172 */     this.requestMap = new WeakHashMap();
/* 173 */     this.clientHeaderMap = new HashMap();
/*     */   }
/*     */ 
/*     */   public HttpClient getHttpClient()
/*     */   {
/* 182 */     return this.httpClient;
/*     */   }
/*     */ 
/*     */   public HttpContext getHttpContext()
/*     */   {
/* 191 */     return this.httpContext;
/*     */   }
/*     */ 
/*     */   public void setCookieStore(CookieStore paramCookieStore)
/*     */   {
/* 199 */     this.httpContext.setAttribute("http.cookie-store", paramCookieStore);
/*     */   }
/*     */ 
/*     */   public void setThreadPool(ThreadPoolExecutor paramThreadPoolExecutor)
/*     */   {
/* 208 */     this.threadPool = paramThreadPoolExecutor;
/*     */   }
/*     */ 
/*     */   public void setUserAgent(String paramString)
/*     */   {
/* 217 */     HttpProtocolParams.setUserAgent(this.httpClient.getParams(), paramString);
/*     */   }
/*     */ 
/*     */   public void setTimeout(int paramInt)
/*     */   {
/* 225 */     HttpParams localHttpParams = this.httpClient.getParams();
/* 226 */     ConnManagerParams.setTimeout(localHttpParams, paramInt);
/* 227 */     HttpConnectionParams.setSoTimeout(localHttpParams, paramInt);
/* 228 */     HttpConnectionParams.setConnectionTimeout(localHttpParams, paramInt);
/*     */   }
/*     */ 
/*     */   public void setSSLSocketFactory(SSLSocketFactory paramSSLSocketFactory)
/*     */   {
/* 237 */     this.httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", paramSSLSocketFactory, 443));
/*     */   }
/*     */ 
/*     */   public void addHeader(String paramString1, String paramString2)
/*     */   {
/* 246 */     this.clientHeaderMap.put(paramString1, paramString2);
/*     */   }
/*     */ 
/*     */   public void setBasicAuth(String paramString1, String paramString2)
/*     */   {
/* 256 */     AuthScope localAuthScope = AuthScope.ANY;
/* 257 */     setBasicAuth(paramString1, paramString2, localAuthScope);
/*     */   }
/*     */ 
/*     */   public void setBasicAuth(String paramString1, String paramString2, AuthScope paramAuthScope)
/*     */   {
/* 269 */     UsernamePasswordCredentials localUsernamePasswordCredentials = new UsernamePasswordCredentials(paramString1, paramString2);
/* 270 */     this.httpClient.getCredentialsProvider().setCredentials(paramAuthScope, localUsernamePasswordCredentials);
/*     */   }
/*     */ 
/*     */   public void cancelRequests(Context paramContext, boolean paramBoolean)
/*     */   {
/* 286 */     List<WeakReference<Future<?>>> localList = (List)this.requestMap.get(paramContext);
/* 287 */     if (localList != null) {
/* 288 */       for (WeakReference localWeakReference : localList) {
/* 289 */         Future localFuture = (Future)localWeakReference.get();
/* 290 */         if (localFuture != null) {
/* 291 */           localFuture.cancel(paramBoolean);
/*     */         }
/*     */       }
/*     */     }
/* 295 */     this.requestMap.remove(paramContext);
/*     */   }
/*     */ 
/*     */   public void get(String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 309 */     get(null, paramString, null, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void get(String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 319 */     get(null, paramString, paramRequestParams, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void get(Context paramContext, String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 329 */     get(paramContext, paramString, null, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void get(Context paramContext, String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 340 */     sendRequest(this.httpClient, this.httpContext, new HttpGet(getUrlWithQueryString(paramString, paramRequestParams)), null, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void get(Context paramContext, String paramString, Header[] paramArrayOfHeader, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 354 */     HttpGet localHttpGet = new HttpGet(getUrlWithQueryString(paramString, paramRequestParams));
/* 355 */     if (paramArrayOfHeader != null) localHttpGet.setHeaders(paramArrayOfHeader);
/* 356 */     sendRequest(this.httpClient, this.httpContext, localHttpGet, null, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void post(String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 371 */     post(null, paramString, null, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void post(String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 381 */     post(null, paramString, paramRequestParams, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void post(Context paramContext, String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 392 */     post(paramContext, paramString, paramsToEntity(paramRequestParams), null, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void post(Context paramContext, String paramString1, HttpEntity paramHttpEntity, String paramString2, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 404 */     sendRequest(this.httpClient, this.httpContext, addEntityToRequestBase(new HttpPost(paramString1), paramHttpEntity), paramString2, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void post(Context paramContext, String paramString1, Header[] paramArrayOfHeader, RequestParams paramRequestParams, String paramString2, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 422 */     HttpPost localHttpPost = new HttpPost(paramString1);
/* 423 */     if (paramRequestParams != null) localHttpPost.setEntity(paramsToEntity(paramRequestParams));
/* 424 */     if (paramArrayOfHeader != null) localHttpPost.setHeaders(paramArrayOfHeader);
/* 425 */     sendRequest(this.httpClient, this.httpContext, localHttpPost, paramString2, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void post(Context paramContext, String paramString1, Header[] paramArrayOfHeader, HttpEntity paramHttpEntity, String paramString2, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 446 */     HttpEntityEnclosingRequestBase localHttpEntityEnclosingRequestBase = addEntityToRequestBase(new HttpPost(paramString1), paramHttpEntity);
/* 447 */     if (paramArrayOfHeader != null) localHttpEntityEnclosingRequestBase.setHeaders(paramArrayOfHeader);
/* 448 */     sendRequest(this.httpClient, this.httpContext, localHttpEntityEnclosingRequestBase, paramString2, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void put(String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 461 */     put(null, paramString, null, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void put(String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 471 */     put(null, paramString, paramRequestParams, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void put(Context paramContext, String paramString, RequestParams paramRequestParams, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 482 */     put(paramContext, paramString, paramsToEntity(paramRequestParams), null, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void put(Context paramContext, String paramString1, HttpEntity paramHttpEntity, String paramString2, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 495 */     sendRequest(this.httpClient, this.httpContext, addEntityToRequestBase(new HttpPut(paramString1), paramHttpEntity), paramString2, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void put(Context paramContext, String paramString1, Header[] paramArrayOfHeader, HttpEntity paramHttpEntity, String paramString2, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 509 */     HttpEntityEnclosingRequestBase localHttpEntityEnclosingRequestBase = addEntityToRequestBase(new HttpPut(paramString1), paramHttpEntity);
/* 510 */     if (paramArrayOfHeader != null) localHttpEntityEnclosingRequestBase.setHeaders(paramArrayOfHeader);
/* 511 */     sendRequest(this.httpClient, this.httpContext, localHttpEntityEnclosingRequestBase, paramString2, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void delete(String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 524 */     delete(null, paramString, paramAsyncHttpResponseHandler);
/*     */   }
/*     */ 
/*     */   public void delete(Context paramContext, String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 534 */     HttpDelete localHttpDelete = new HttpDelete(paramString);
/* 535 */     sendRequest(this.httpClient, this.httpContext, localHttpDelete, null, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   public void delete(Context paramContext, String paramString, Header[] paramArrayOfHeader, AsyncHttpResponseHandler paramAsyncHttpResponseHandler)
/*     */   {
/* 546 */     HttpDelete localHttpDelete = new HttpDelete(paramString);
/* 547 */     if (paramArrayOfHeader != null) localHttpDelete.setHeaders(paramArrayOfHeader);
/* 548 */     sendRequest(this.httpClient, this.httpContext, localHttpDelete, null, paramAsyncHttpResponseHandler, paramContext);
/*     */   }
/*     */ 
/*     */   protected void sendRequest(DefaultHttpClient paramDefaultHttpClient, HttpContext paramHttpContext, HttpUriRequest paramHttpUriRequest, String paramString, AsyncHttpResponseHandler paramAsyncHttpResponseHandler, Context paramContext)
/*     */   {
				Log.d(TAG, "sendRequest()");
/* 554 */     if (paramString != null) {
/* 555 */       paramHttpUriRequest.addHeader("Content-Type", paramString);
/*     */     }
/*     */ 
/* 558 */     Future localFuture = this.threadPool.submit(new AsyncHttpRequest(paramDefaultHttpClient, paramHttpContext, paramHttpUriRequest, paramAsyncHttpResponseHandler));
/*     */ 
/* 560 */     if (paramContext != null)
/*     */     {
/* 562 */       List<WeakReference<Future<?>>> localObject = (List<WeakReference<Future<?>>>)this.requestMap.get(paramContext);
/* 563 */       if (localObject == null) {
/* 564 */         localObject = new LinkedList();
/* 565 */         this.requestMap.put(paramContext, localObject);
/*     */       }
/*     */ 
/* 568 */       ((List)localObject).add(new WeakReference(localFuture));
/*     */     }
/*     */   }
/*     */ 
/*     */   public static String getUrlWithQueryString(String paramString, RequestParams paramRequestParams)
/*     */   {
/* 575 */     if (paramRequestParams != null) {
/* 576 */       String str = paramRequestParams.getParamString();
/* 577 */       if (paramString.indexOf("?") == -1)
/* 578 */         paramString = paramString + "?" + str;
/*     */       else {
/* 580 */         paramString = paramString + "&" + str;
/*     */       }
/*     */     }
/*     */ 
/* 584 */     return paramString;
/*     */   }
/*     */ 
/*     */   private HttpEntity paramsToEntity(RequestParams paramRequestParams) {
/* 588 */     HttpEntity localHttpEntity = null;
/*     */ 
/* 590 */     if (paramRequestParams != null) {
/* 591 */       localHttpEntity = paramRequestParams.getEntity();
/*     */     }
/*     */ 
/* 594 */     return localHttpEntity;
/*     */   }
/*     */ 
/*     */   private HttpEntityEnclosingRequestBase addEntityToRequestBase(HttpEntityEnclosingRequestBase paramHttpEntityEnclosingRequestBase, HttpEntity paramHttpEntity) {
/* 598 */     if (paramHttpEntity != null) {
/* 599 */       paramHttpEntityEnclosingRequestBase.setEntity(paramHttpEntity);
/*     */     }
/*     */ 
/* 602 */     return paramHttpEntityEnclosingRequestBase;
/*     */   }
/*     */ 
/*     */   private static class InflatingEntity extends HttpEntityWrapper {
/*     */     public InflatingEntity(HttpEntity paramHttpEntity) {
/* 607 */       super(paramHttpEntity);
/*     */     }
/*     */ 
/*     */     public InputStream getContent() throws IOException
/*     */     {
/* 612 */       return new GZIPInputStream(this.wrappedEntity.getContent());
/*     */     }
/*     */ 
/*     */     public long getContentLength()
/*     */     {
/* 617 */       return -1L;
/*     */     }
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.AsyncHttpClient
 * JD-Core Version:    0.6.2
 */