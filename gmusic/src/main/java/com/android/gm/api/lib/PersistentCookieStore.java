/*     */ package com.android.gm.api.lib;
/*     */ 
/*     */

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
/*     */ public class PersistentCookieStore
/*     */   implements CookieStore
/*     */ {
/*     */   private static final String COOKIE_PREFS = "CookiePrefsFile";
/*     */   private static final String COOKIE_NAME_STORE = "names";
/*     */   private static final String COOKIE_NAME_PREFIX = "cookie_";
/*     */   private final ConcurrentHashMap<String, Cookie> cookies;
/*     */   private final SharedPreferences cookiePrefs;
/*     */ 
/*     */   public PersistentCookieStore(Context paramContext)
/*     */   {
/*  59 */     this.cookiePrefs = paramContext.getSharedPreferences("CookiePrefsFile", 0);
/*  60 */     this.cookies = new ConcurrentHashMap();
/*     */ 
/*  63 */     String str1 = this.cookiePrefs.getString("names", null);
/*  64 */     if (str1 != null) {
/*  65 */       String[] arrayOfString1 = TextUtils.split(str1, ",");
/*  66 */       for (String str2 : arrayOfString1) {
/*  67 */         String str3 = this.cookiePrefs.getString("cookie_" + str2, null);
/*  68 */         if (str3 != null) {
/*  69 */           Cookie localCookie = decodeCookie(str3);
/*  70 */           if (localCookie != null) {
/*  71 */             this.cookies.put(str2, localCookie);
/*     */           }
/*     */         }
/*     */ 
/*     */       }
/*     */ 
/*  77 */       clearExpired(new Date());
/*     */     }
/*     */   }
/*     */ 
/*     */   public void addCookie(Cookie paramCookie)
/*     */   {
/*  83 */     String str = paramCookie.getName() + paramCookie.getDomain();
/*     */ 
/*  86 */     if (!paramCookie.isExpired(new Date()))
/*  87 */       this.cookies.put(str, paramCookie);
/*     */     else {
/*  89 */       this.cookies.remove(str);
/*     */     }
/*     */ 
/*  93 */     SharedPreferences.Editor localEditor = this.cookiePrefs.edit();
/*  94 */     localEditor.putString("names", TextUtils.join(",", this.cookies.keySet()));
/*  95 */     localEditor.putString("cookie_" + str, encodeCookie(new SerializableCookie(paramCookie)));
/*  96 */     localEditor.commit();
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 102 */     SharedPreferences.Editor localEditor = this.cookiePrefs.edit();
/* 103 */     for (String str : this.cookies.keySet()) {
/* 104 */       localEditor.remove("cookie_" + str);
/*     */     }
/* 106 */     localEditor.remove("names");
/* 107 */     localEditor.commit();
/*     */ 
/* 110 */     this.cookies.clear();
/*     */   }
/*     */ 
/*     */   public boolean clearExpired(Date paramDate)
/*     */   {
/* 115 */     boolean bool = false;
/* 116 */     SharedPreferences.Editor localEditor = this.cookiePrefs.edit();
/*     */ 
/* 118 */     for (Map.Entry localEntry : this.cookies.entrySet()) {
/* 119 */       String str = (String)localEntry.getKey();
/* 120 */       Cookie localCookie = (Cookie)localEntry.getValue();
/* 121 */       if (localCookie.isExpired(paramDate))
/*     */       {
/* 123 */         this.cookies.remove(str);
/*     */ 
/* 126 */         localEditor.remove("cookie_" + str);
/*     */ 
/* 129 */         bool = true;
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 134 */     if (bool) {
/* 135 */       localEditor.putString("names", TextUtils.join(",", this.cookies.keySet()));
/*     */     }
/* 137 */     localEditor.commit();
/*     */ 
/* 139 */     return bool;
/*     */   }
/*     */ 
/*     */   public List<Cookie> getCookies()
/*     */   {
/* 144 */     return new ArrayList(this.cookies.values());
/*     */   }
/*     */ 
/*     */   protected String encodeCookie(SerializableCookie paramSerializableCookie)
/*     */   {
/* 153 */     ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
/*     */     try {
/* 155 */       ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localByteArrayOutputStream);
/* 156 */       localObjectOutputStream.writeObject(paramSerializableCookie);
/*     */     } catch (Exception localException) {
/* 158 */       return null;
/*     */     }
/*     */ 
/* 161 */     return byteArrayToHexString(localByteArrayOutputStream.toByteArray());
/*     */   }
/*     */ 
/*     */   protected Cookie decodeCookie(String paramString) {
/* 165 */     byte[] arrayOfByte = hexStringToByteArray(paramString);
/* 166 */     ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(arrayOfByte);
/* 167 */     Cookie localCookie = null;
/*     */     try {
/* 169 */       ObjectInputStream localObjectInputStream = new ObjectInputStream(localByteArrayInputStream);
/* 170 */       localCookie = ((SerializableCookie)localObjectInputStream.readObject()).getCookie();
/*     */     } catch (Exception localException) {
/* 172 */       localException.printStackTrace();
/*     */     }
/*     */ 
/* 175 */     return localCookie;
/*     */   }
/*     */ 
/*     */   protected String byteArrayToHexString(byte[] paramArrayOfByte)
/*     */   {
/* 181 */     StringBuffer localStringBuffer = new StringBuffer(paramArrayOfByte.length * 2);
/* 182 */     for (int k : paramArrayOfByte) {
/* 183 */       int m = k & 0xFF;
/* 184 */       if (m < 16) {
/* 185 */         localStringBuffer.append('0');
/*     */       }
/* 187 */       localStringBuffer.append(Integer.toHexString(m));
/*     */     }
/* 189 */     return localStringBuffer.toString().toUpperCase();
/*     */   }
/*     */ 
/*     */   protected byte[] hexStringToByteArray(String paramString) {
/* 193 */     int i = paramString.length();
/* 194 */     byte[] arrayOfByte = new byte[i / 2];
/* 195 */     for (int j = 0; j < i; j += 2) {
/* 196 */       arrayOfByte[(j / 2)] = ((byte)((Character.digit(paramString.charAt(j), 16) << 4) + Character.digit(paramString.charAt(j + 1), 16)));
/*     */     }
/* 198 */     return arrayOfByte;
/*     */   }
/*     */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.PersistentCookieStore
 * JD-Core Version:    0.6.2
 */