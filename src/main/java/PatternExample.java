import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author : Yashpal_Rawat
 * lastModifiedDate : 11/04/2018 9:55 PM
 */
public class PatternExample {
    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("(\\{)(\\S+)(\\})");
        //Pattern pattern = Pattern.compile("(\\w+)");

        String test = "{this/picket/up} {adsf} {adsfas} {asdfadsfafasfasdf} ";

        String test2 = "one1 two ka four one2 two ka one3";

        Matcher matcher = pattern.matcher(test);
        //System.out.println("group count = " + matcher.groupCount());
        while (matcher.find()) {
            //System.out.println("group => " + matcher.group() + " "  + matcher.start());
            //System.out.println("extracted value => " + matcher.group(0));
            //System.out.println("extracted value => " + matcher.group(1));
            System.out.println("extracted value => " + matcher.group(2));
            //System.out.println("extracted value => " + matcher.group(3));
        }
    }


}
