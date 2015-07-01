public class Base{
    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        int c;
        int d;
        d = 0;
        c = 3;
        if (1 + 1 > 2){
            if (1 + 1 >3){
                if (1 + 1 > 4){
                    a++;
                }
            }
            a++;
            b++;
            c++;
            d++;
        }
        try {
            a = Math.sqrt(a) / d;
        }
        catch (Exception ex) {
            if((Math.abs(a)) < 1.0E-6) {
                return a;
            }
            if(b < 1.0E-6) {
                return b;
            }
            throw new Exception(ex);
        }
        System.out.println(b);
        if (1+1 == 2){
            System.out.println(a);
        }
    }
}