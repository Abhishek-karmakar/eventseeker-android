/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.InputStream;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedList;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.ConcurrentHashMap;

/*     */ import org.apache.http.HttpEntity;
/*     */ import org.apache.http.client.entity.UrlEncodedFormEntity;
/*     */ import org.apache.http.client.utils.URLEncodedUtils;
/*     */ import org.apache.http.message.BasicNameValuePair;
/*     */ 
/*     */ public class RequestParams
/*     */ {
/*  57 */   private static String ENCODING = "UTF-8";
/*     */   protected ConcurrentHashMap<String, String> urlParams;
/*     */   protected ConcurrentHashMap<String, FileWrapper> fileParams;
/*     */   protected ConcurrentHashMap<String, ArrayList<String>> urlParamsWithArray;
/*     */ 
/*     */   public RequestParams()
/*     */   {
/*  67 */     init();
/*     */   }
/*     */ 
/*     */   public RequestParams(Map<String, String> paramMap)
/*     */   {
/*  76 */     init();
/*     */ 
/*  78 */     for (Map.Entry localEntry : paramMap.entrySet())
/*  79 */       put((String)localEntry.getKey(), (String)localEntry.getValue());
/*     */   }
/*     */ 
/*     */   public RequestParams(String paramString1, String paramString2)
/*     */   {
/*  90 */     init();
/*     */ 
/*  92 */     put(paramString1, paramString2);
/*     */   }
/*     */ 
/*     */   public RequestParams(Object[] paramArrayOfObject)
/*     */   {
/* 103 */     init();
/* 104 */     int i = paramArrayOfObject.length;
/* 105 */     if (i % 2 != 0)
/* 106 */       throw new IllegalArgumentException("Supplied arguments must be even");
/* 107 */     for (int j = 0; j < i; j += 2) {
/* 108 */       String str1 = String.valueOf(paramArrayOfObject[j]);
/* 109 */       String str2 = String.valueOf(paramArrayOfObject[(j + 1)]);
/* 110 */       put(str1, str2);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void put(String paramString1, String paramString2)
/*     */   {
/* 120 */     if ((paramString1 != null) && (paramString2 != null))
/* 121 */       this.urlParams.put(paramString1, paramString2);
/*     */   }
/*     */ 
/*     */   public void put(String paramString, File paramFile)
/*     */     throws FileNotFoundException
/*     */   {
/* 131 */     put(paramString, new FileInputStream(paramFile), paramFile.getName());
/*     */   }
/*     */ 
/*     */   public void put(String paramString, ArrayList<String> paramArrayList)
/*     */   {
/* 140 */     if ((paramString != null) && (paramArrayList != null))
/* 141 */       this.urlParamsWithArray.put(paramString, paramArrayList);
/*     */   }
/*     */ 
/*     */   public void put(String paramString, InputStream paramInputStream)
/*     */   {
/* 151 */     put(paramString, paramInputStream, null);
/*     */   }
/*     */ 
/*     */   public void put(String paramString1, InputStream paramInputStream, String paramString2)
/*     */   {
/* 161 */     put(paramString1, paramInputStream, paramString2, null);
/*     */   }
/*     */ 
/*     */   public void put(String paramString1, InputStream paramInputStream, String paramString2, String paramString3)
/*     */   {
/* 172 */     if ((paramString1 != null) && (paramInputStream != null))
/* 173 */       this.fileParams.put(paramString1, new FileWrapper(paramInputStream, paramString2, paramString3));
/*     */   }
/*     */ 
/*     */   public void remove(String paramString)
/*     */   {
/* 182 */     this.urlParams.remove(paramString);
/* 183 */     this.fileParams.remove(paramString);
/* 184 */     this.urlParamsWithArray.remove(paramString);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 189 */     StringBuilder localStringBuilder = new StringBuilder();
/* 190 */     for (Iterator localIterator = this.urlParams.entrySet().iterator(); localIterator.hasNext(); ) { Entry localEntry = (Map.Entry)localIterator.next();
/* 191 */       if (localStringBuilder.length() > 0) {
/* 192 */         localStringBuilder.append("&");
/*     */       }
/* 194 */       localStringBuilder.append((String)localEntry.getKey());
/* 195 */       localStringBuilder.append("=");
/* 196 */       localStringBuilder.append((String)localEntry.getValue());
/*     */     }
/* 199 */     Map.Entry localEntry;
/* 199 */     for (Iterator localIterator = this.fileParams.entrySet().iterator(); localIterator.hasNext(); ) { localEntry = (Map.Entry)localIterator.next();
/* 200 */       if (localStringBuilder.length() > 0) {
/* 201 */         localStringBuilder.append("&");
/*     */       }
/* 203 */       localStringBuilder.append((String)localEntry.getKey());
/* 204 */       localStringBuilder.append("=");
/* 205 */       localStringBuilder.append("FILE");
/*     */     }
/*     */ 
/* 208 */     for (Iterator localIterator = this.urlParamsWithArray.entrySet().iterator(); localIterator.hasNext(); ) { localEntry = (Map.Entry)localIterator.next();
/* 209 */       if (localStringBuilder.length() > 0) {
/* 210 */         localStringBuilder.append("&");
/*     */       }
/* 212 */       ArrayList localArrayList = (ArrayList)localEntry.getValue();
/* 213 */       for (int i = 0; i < localArrayList.size(); i++) {
/* 214 */         if (i != 0)
/* 215 */           localStringBuilder.append("&");
/* 216 */         localStringBuilder.append((String)localEntry.getKey());
/* 217 */         localStringBuilder.append("=");
/* 218 */         localStringBuilder.append((String)localArrayList.get(i));
/*     */       }
/*     */     }
/*     */ 
/* 222 */     return localStringBuilder.toString();
/*     */   }
/*     */ 
/*     */   public HttpEntity getEntity()
/*     */   {
/* 229 */     Object localObject1 = null;
/*     */ 
/* 231 */     if (!this.fileParams.isEmpty()) {
/* 232 */       SimpleMultipartEntity localSimpleMultipartEntity = new SimpleMultipartEntity();
/*     */ 
/* 235 */       for (Iterator localIterator = this.urlParams.entrySet().iterator(); localIterator.hasNext(); ) { Entry localEntry = (Map.Entry)localIterator.next();
/* 236 */         localSimpleMultipartEntity.addPart((String)localEntry.getKey(), (String)localEntry.getValue());
/*     */       }
/* 240 */       Map.Entry localEntry;
/* 240 */       for (Iterator localIterator = this.urlParamsWithArray.entrySet().iterator(); localIterator.hasNext(); ) { localEntry = (Map.Entry)localIterator.next();
/* 241 */         ArrayList localObject2 = (ArrayList)localEntry.getValue();
/* 242 */         for (Iterator localObject3 = ((ArrayList)localObject2).iterator(); ((Iterator)localObject3).hasNext(); ) { String localObject4 = (String)((Iterator)localObject3).next();
/* 243 */           localSimpleMultipartEntity.addPart((String)localEntry.getKey(), (String)localObject4);
/*     */         }
/*     */       }
/* 250 */       Object localObject3;
/*     */       Object localObject4;
/* 248 */       int i = 0;
/* 249 */       int j = this.fileParams.entrySet().size() - 1;
/* 250 */       for (Object localObject2 = this.fileParams.entrySet().iterator(); ((Iterator)localObject2).hasNext(); ) { localObject3 = (Map.Entry)((Iterator)localObject2).next();
/* 251 */         localObject4 = (FileWrapper)((Map.Entry)localObject3).getValue();
/* 252 */         if (((FileWrapper)localObject4).inputStream != null) {
/* 253 */           boolean bool = i == j;
/* 254 */           if (((FileWrapper)localObject4).contentType != null)
/* 255 */             localSimpleMultipartEntity.addPart((String)((Map.Entry)localObject3).getKey(), ((FileWrapper)localObject4).getFileName(), ((FileWrapper)localObject4).inputStream, ((FileWrapper)localObject4).contentType, bool);
/*     */           else {
/* 257 */             localSimpleMultipartEntity.addPart((String)((Map.Entry)localObject3).getKey(), ((FileWrapper)localObject4).getFileName(), ((FileWrapper)localObject4).inputStream, bool);
/*     */           }
/*     */         }
/* 260 */         i++;
/*     */       }
/*     */ 
/* 263 */       localObject1 = localSimpleMultipartEntity;
/*     */     } else {
/*     */       try {
/* 266 */         localObject1 = new UrlEncodedFormEntity(getParamsList(), ENCODING);
/*     */       } catch (UnsupportedEncodingException localUnsupportedEncodingException) {
/* 268 */         localUnsupportedEncodingException.printStackTrace();
/*     */       }
/*     */     }
/*     */ 
/* 272 */     return (HttpEntity) localObject1;
/*     */   }
/*     */ 
/*     */   private void init() {
/* 276 */     this.urlParams = new ConcurrentHashMap();
/* 277 */     this.fileParams = new ConcurrentHashMap();
/* 278 */     this.urlParamsWithArray = new ConcurrentHashMap();
/*     */   }
/*     */ 
/*     */   protected List<BasicNameValuePair> getParamsList() {
/* 282 */     LinkedList localLinkedList = new LinkedList();
/*     */ 
/* 284 */     for (Iterator localIterator1 = this.urlParams.entrySet().iterator(); localIterator1.hasNext(); ) { Entry localEntry = (Map.Entry)localIterator1.next();
/* 285 */       localLinkedList.add(new BasicNameValuePair((String)localEntry.getKey(), (String)localEntry.getValue()));
/*     */     }
/* 288 */     Map.Entry localEntry;
/* 288 */     for (Iterator localIterator1 = this.urlParamsWithArray.entrySet().iterator(); localIterator1.hasNext(); ) { localEntry = (Map.Entry)localIterator1.next();
/* 289 */       ArrayList<String> localArrayList = (ArrayList)localEntry.getValue();
/* 290 */       for (String str : localArrayList) {
/* 291 */         localLinkedList.add(new BasicNameValuePair((String)localEntry.getKey(), str));
/*     */       }
/*     */     }
/*     */ 
/* 295 */     return localLinkedList;
/*     */   }
/*     */ 
/*     */   protected String getParamString() {
/* 299 */     return URLEncodedUtils.format(getParamsList(), ENCODING);
/*     */   }
/*     */   private static class FileWrapper {
/*     */     public InputStream inputStream;
/*     */     public String fileName;
/*     */     public String contentType;
/*     */ 
/* 308 */     public FileWrapper(InputStream paramInputStream, String paramString1, String paramString2) { this.inputStream = paramInputStream;
/* 309 */       this.fileName = paramString1;
/* 310 */       this.contentType = paramString2; }
/*     */ 
/*     */     public String getFileName()
/*     */     {
/* 314 */       if (this.fileName != null) {
/* 315 */         return this.fileName;
/*     */       }
/* 317 */       return "nofilename";
/*     */     }
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.RequestParams
 * JD-Core Version:    0.6.2
 */