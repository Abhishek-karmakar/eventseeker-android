/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

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
/*     */ class SimpleMultipartEntity
/*     */   implements HttpEntity
/*     */ {
/*  41 */   private static final char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
/*     */ 
/*  43 */   private String boundary = null;
/*     */ 
/*  45 */   ByteArrayOutputStream out = new ByteArrayOutputStream();
/*  46 */   boolean isSetLast = false;
/*  47 */   boolean isSetFirst = false;
/*     */ 
/*     */   public SimpleMultipartEntity() {
/*  50 */     StringBuffer localStringBuffer = new StringBuffer();
/*  51 */     Random localRandom = new Random();
/*  52 */     for (int i = 0; i < 30; i++) {
/*  53 */       localStringBuffer.append(MULTIPART_CHARS[localRandom.nextInt(MULTIPART_CHARS.length)]);
/*     */     }
/*  55 */     this.boundary = localStringBuffer.toString();
/*     */   }
/*     */ 
/*     */   public void writeFirstBoundaryIfNeeds()
/*     */   {
/*  60 */     if (!this.isSetFirst) {
/*  61 */       writeBoundary();
/*     */     }
/*     */ 
/*  64 */     this.isSetFirst = true;
/*     */   }
/*     */ 
/*     */   public void writeBoundary() {
/*     */     try {
/*  69 */       this.out.write(("--" + this.boundary + "\r\n").getBytes());
/*     */     } catch (IOException localIOException) {
/*  71 */       localIOException.printStackTrace();
/*     */     }
/*     */   }
/*     */ 
/*     */   public void writeLastBoundaryIfNeeds() {
/*  76 */     if (this.isSetLast) {
/*  77 */       return;
/*     */     }
/*     */     try
/*     */     {
/*  81 */       this.out.write(("--" + this.boundary + "--\r\n").getBytes());
/*  82 */       this.out.flush();
/*     */     } catch (IOException localIOException) {
/*  84 */       localIOException.printStackTrace();
/*     */     }
/*     */ 
/*  87 */     this.isSetLast = true;
/*     */   }
/*     */ 
/*     */   public void addPart(String paramString1, String paramString2, String paramString3) {
/*  91 */     writeBoundary();
/*     */     try {
/*  93 */       this.out.write(("Content-Disposition: form-data; name=\"" + paramString1 + "\"\r\n").getBytes());
/*  94 */       this.out.write(("Content-Type: " + paramString3 + "\r\n\r\n").getBytes());
/*  95 */       this.out.write(paramString2.getBytes());
/*  96 */       this.out.write("\r\n".getBytes());
/*     */     } catch (IOException localIOException) {
/*  98 */       localIOException.printStackTrace();
/*     */     }
/*     */   }
/*     */ 
/*     */   public void addPart(String paramString1, String paramString2) {
/* 103 */     addPart(paramString1, paramString2, "text/plain; charset=UTF-8");
/*     */   }
/*     */ 
/*     */   public void addPart(String paramString1, String paramString2, InputStream paramInputStream, boolean paramBoolean) {
/* 107 */     addPart(paramString1, paramString2, paramInputStream, "application/octet-stream", paramBoolean);
/*     */   }
/*     */ 
/*     */   public void addPart(String paramString1, String paramString2, InputStream paramInputStream, String paramString3, boolean paramBoolean) {
/* 111 */     writeBoundary();
/*     */     try {
/* 113 */       paramString3 = "Content-Type: " + paramString3 + "\r\n";
/* 114 */       this.out.write(("Content-Disposition: form-data; name=\"" + paramString1 + "\"; filename=\"" + paramString2 + "\"\r\n").getBytes());
/* 115 */       this.out.write(paramString3.getBytes());
/* 116 */       this.out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());
/*     */ 
/* 118 */       byte[] arrayOfByte = new byte[4096];
/* 119 */       int i = 0;
/* 120 */       while ((i = paramInputStream.read(arrayOfByte)) != -1) {
/* 121 */         this.out.write(arrayOfByte, 0, i);
/*     */       }
/* 123 */       this.out.write("\r\n".getBytes());
/*     */     }
/*     */     catch (IOException localIOException2) {
/* 126 */       localIOException2.printStackTrace();
/*     */     } finally {
/*     */       try {
/* 129 */         paramInputStream.close();
/*     */       } catch (IOException localIOException4) {
/* 131 */         localIOException4.printStackTrace();
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   public void addPart(String paramString, File paramFile, boolean paramBoolean) {
/*     */     try {
/* 138 */       addPart(paramString, paramFile.getName(), new FileInputStream(paramFile), paramBoolean);
/*     */     } catch (FileNotFoundException localFileNotFoundException) {
/* 140 */       localFileNotFoundException.printStackTrace();
/*     */     }
/*     */   }
/*     */ 
/*     */   public long getContentLength()
/*     */   {
/* 146 */     writeLastBoundaryIfNeeds();
/* 147 */     return this.out.toByteArray().length;
/*     */   }
/*     */ 
/*     */   public Header getContentType()
/*     */   {
/* 152 */     return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + this.boundary);
/*     */   }
/*     */ 
/*     */   public boolean isChunked()
/*     */   {
/* 157 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean isRepeatable()
/*     */   {
/* 162 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean isStreaming()
/*     */   {
/* 167 */     return false;
/*     */   }
/*     */ 
/*     */   public void writeTo(OutputStream paramOutputStream) throws IOException
/*     */   {
/* 172 */     writeLastBoundaryIfNeeds();
/* 173 */     paramOutputStream.write(this.out.toByteArray());
/*     */   }
/*     */ 
/*     */   public Header getContentEncoding()
/*     */   {
/* 178 */     return null;
/*     */   }
/*     */ 
/*     */   public void consumeContent()
/*     */     throws IOException, UnsupportedOperationException
/*     */   {
/* 184 */     if (isStreaming())
/* 185 */       throw new UnsupportedOperationException("Streaming entity does not implement #consumeContent()");
/*     */   }
/*     */ 
/*     */   public InputStream getContent()
/*     */     throws IOException, UnsupportedOperationException
/*     */   {
/* 193 */     writeLastBoundaryIfNeeds();
/* 194 */     return new ByteArrayInputStream(this.out.toByteArray());
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.SimpleMultipartEntity
 * JD-Core Version:    0.6.2
 */