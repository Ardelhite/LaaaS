package utils;

import org.junit.Test;

public class RandomizerTest {

    @Test
    public void randomStringTest() {
        String rand = Randomizer.generateRandomString(128);
        System.out.println(rand);
    }

}
