package org.infinispan.creson.utils;

import org.infinispan.creson.Entity;
import org.infinispan.creson.ReadOnly;

/**
* @author Pierre Sutra
*/
@Entity(key="field")
public class SimpleObject {

   public String field;

   public SimpleObject(){
      field = "test";
   }

   public SimpleObject(String f){
      field = f;
   }

   @ReadOnly
   public String getField(){ return field;}

   public void setField(String f){
      field = f;
   }

   @ReadOnly
   public String toString(){
      return "SimpleObject["+field+"]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      SimpleObject that = (SimpleObject) o;

      return field.equals(that.field);
   }

   @Override
   public int hashCode() {
      return field.hashCode();
   }
}