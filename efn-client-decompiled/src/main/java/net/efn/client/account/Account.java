package net.efn.client.account;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Account {
   private final String id;
   private final AccountType type;
   private final String name;
   private final String uuid;
   private final String secret;

   Account(String var1, AccountType var2, String var3, String var4, String var5) {
      this.id = var1;
      this.type = var2;
      this.name = var3;
      this.uuid = var4;
      this.secret = var5;
   }

   public AccountType type() {
      return this.type;
   }

   public String name() {
      return this.name;
   }

   public String uuid() {
      return this.uuid;
   }

   public String id() {
      return this.id;
   }

   String secret() {
      return this.secret;
   }

   Account withLogin(String var1, String var2, String var3) {
      return new Account(this.id, this.type, var1, var2, var3);
   }

   @Override
   public boolean equals(Object var1) {
      return !(var1 instanceof Account var2) ? false : var2.id.equals(this.id);
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.type.label() + " " + this.name;
   }
}
