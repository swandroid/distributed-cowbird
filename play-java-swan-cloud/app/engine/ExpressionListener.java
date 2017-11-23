package engine;

/**
 * Created by goose on 09/06/16.
 */


import interdroid.swancore.swansong.TriStateExpression;
import interdroid.swancore.swansong.ValueExpression;

/**
 * Generic listener for both {@link TriStateExpression} expressions and
 * {@link ValueExpression} expressions.
 *
 * @author rkemp
 */
public interface ExpressionListener extends TriStateExpressionListener,
        ValueExpressionListener {

}
