package io.geekya215.algorithm_j;

import io.geekya215.algorithm_j.type.TFun;
import io.geekya215.algorithm_j.type.TUnit;
import io.geekya215.algorithm_j.type.TVar;
import io.geekya215.algorithm_j.type.Type;
import io.geekya215.algorithm_j.typevar.Bound;
import io.geekya215.algorithm_j.typevar.Unbound;

import java.util.Map;

public final class Pretty {
    static Character currentTypevarName = 'a';

    static void nextLetter() {
        currentTypevarName = (char) (currentTypevarName + 1);
    }

    static Boolean shouldParenthesize(Type t) {
        if (t instanceof TVar _t && _t.typeVarRef().unwrap() instanceof Bound __t) {
            return shouldParenthesize(__t.t());
        } else return t instanceof TFun;
    }

    static String prettyType(Map<Integer, String> tbl, Type t) {
        return switch (t) {
            case TUnit tUnit -> "unit";
            case TVar tVar -> switch (tVar.typeVarRef().unwrap()) {
                case Bound bound -> prettyType(tbl, bound.t());
                case Unbound unbound -> {
                    var n = unbound.id();
                    if (tbl.containsKey(n)) {
                        yield tbl.get(n);
                    } else {
                        var s = currentTypevarName.toString();
                        tbl.put(n, s);
                        nextLetter();
                        yield s;
                    }
                }
            };
            case TFun tFun -> {
                var t1 = tFun.t1();
                var t2 = tFun.t2();
                var aStr = prettyType(tbl, t1);
                var bStr = prettyType(tbl, t2);
                if (shouldParenthesize(t1)) {
                    yield "(" + aStr + ") -> " + bStr;
                } else {
                    yield aStr + " -> " + bStr;
                }
            }
        };
    }
}
