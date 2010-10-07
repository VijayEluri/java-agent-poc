package test;

import java.util.Date;

public class SimpleTest {

    public static void main(String[] args) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException,
            ClassNotFoundException {
        SimpleAgent.loadAgent();
        new SimpleTest().hello();
    }

    public void hello() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
        System.out.println(new Date());
        DateOffset.offset += 99999999999L;
        System.out.println(new Date());
    }

}
