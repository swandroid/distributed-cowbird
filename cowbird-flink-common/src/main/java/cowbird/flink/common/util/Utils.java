package cowbird.flink.common.util;

public final class Utils {

    public static String getParentForExpression(String expression) {

        int index = expression.lastIndexOf('.');

        if(index > 0)
            return expression.substring(0, index);

        return expression;
    }


    private Utils() {
        throw new RuntimeException("You cannot create a new instance of this class.");
    }
}
