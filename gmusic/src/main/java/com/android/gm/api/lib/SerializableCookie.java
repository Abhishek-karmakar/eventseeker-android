/*    */ package com.android.gm.api.lib;
/*    */ 
/*    */

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

/*    */
/*    */
/*    */
/*    */
/*    */
/*    */
/*    */ 
/*    */ public class SerializableCookie
/*    */   implements Serializable
/*    */ {
/*    */   private static final long serialVersionUID = 6374381828722046732L;
/*    */   private final transient Cookie cookie;
/*    */   private transient BasicClientCookie clientCookie;
/*    */ 
/*    */   public SerializableCookie(Cookie paramCookie)
/*    */   {
/* 41 */     this.cookie = paramCookie;
/*    */   }
/*    */ 
/*    */   public Cookie getCookie() {
/* 45 */     Object localObject = this.cookie;
/* 46 */     if (this.clientCookie != null) {
/* 47 */       localObject = this.clientCookie;
/*    */     }
/* 49 */     return (Cookie) localObject;
/*    */   }
/*    */ 
/*    */   private void writeObject(ObjectOutputStream paramObjectOutputStream) throws IOException {
/* 53 */     paramObjectOutputStream.writeObject(this.cookie.getName());
/* 54 */     paramObjectOutputStream.writeObject(this.cookie.getValue());
/* 55 */     paramObjectOutputStream.writeObject(this.cookie.getComment());
/* 56 */     paramObjectOutputStream.writeObject(this.cookie.getDomain());
/* 57 */     paramObjectOutputStream.writeObject(this.cookie.getExpiryDate());
/* 58 */     paramObjectOutputStream.writeObject(this.cookie.getPath());
/* 59 */     paramObjectOutputStream.writeInt(this.cookie.getVersion());
/* 60 */     paramObjectOutputStream.writeBoolean(this.cookie.isSecure());
/*    */   }
/*    */ 
/*    */   private void readObject(ObjectInputStream paramObjectInputStream) throws IOException, ClassNotFoundException {
/* 64 */     String str1 = (String)paramObjectInputStream.readObject();
/* 65 */     String str2 = (String)paramObjectInputStream.readObject();
/* 66 */     this.clientCookie = new BasicClientCookie(str1, str2);
/* 67 */     this.clientCookie.setComment((String)paramObjectInputStream.readObject());
/* 68 */     this.clientCookie.setDomain((String)paramObjectInputStream.readObject());
/* 69 */     this.clientCookie.setExpiryDate((Date)paramObjectInputStream.readObject());
/* 70 */     this.clientCookie.setPath((String)paramObjectInputStream.readObject());
/* 71 */     this.clientCookie.setVersion(paramObjectInputStream.readInt());
/* 72 */     this.clientCookie.setSecure(paramObjectInputStream.readBoolean());
/*    */   }
/*    */ }

/* Location:           D:\Ankur\gmusic\libs\android-async-http-1.4.2-66-g4b6eb97.jar
 * Qualified Name:     com.loopj.android.http.SerializableCookie
 * JD-Core Version:    0.6.2
 */