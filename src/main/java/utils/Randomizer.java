package utils;

import java.util.Random;

public class Randomizer {

   public static String generateRandomString(Integer length) {
       char[] randomChars = new char[length];
       for (int i = 0; i < length; i++) {
           randomChars[i] = generateRandomCharacter();
       }
       System.out.println("[LAAAS/RANDOMIZER] Generated String: " + String.valueOf(randomChars));
       return String.valueOf(randomChars);
   }

   public static char generateRandomCharacter() {
       int i = new Random().nextInt(123);
       while (!(47 < i && i < 58) && !(64 < i && i < 91) && !(96 < i)) {
           i = new Random().nextInt(123);
       }
       return (char) i;
   }
}
