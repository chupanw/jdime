public class Base{
    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        int c;
        a++;
        b = a;
        assert b == 3;
    }
}