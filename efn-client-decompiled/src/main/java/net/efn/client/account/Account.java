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

   Account(String string, AccountType accountType, String string2, String string3, String string4) {
      this.id = string;
      this.type = accountType;
      this.name = string2;
      this.uuid = string3;
      this.secret = string4;
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

   Account withLogin(String string, String string2, String string3) {
      return new Account(this.id, this.type, string, string2, string3);
   }

   @Override
   public boolean equals(Object object) {
      return !(object instanceof Account var2) ? false : var2.id.equals(this.id);
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
