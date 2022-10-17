package io.geekya215.algorithm_j;

import io.geekya215.algorithm_j.expr.*;
import io.geekya215.algorithm_j.type.TFun;
import io.geekya215.algorithm_j.type.TUnit;
import io.geekya215.algorithm_j.type.TVar;
import io.geekya215.algorithm_j.type.Type;
import io.geekya215.algorithm_j.typevar.Bound;
import io.geekya215.algorithm_j.typevar.Unbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class J {
    static Integer currentLevel = 1;
    static Integer currentTypeVar = 0;

    static void enterLevel() {
        currentLevel++;
    }

    static void exitLevel() {
        currentLevel--;
    }

    static Integer newVar() {
        return ++currentTypeVar;
    }

    static Type newVarT() {
        return new TVar(new Ref<>(new Unbound(newVar(), currentLevel)));
    }

    static Type inst(PolyType polyType) {
        var tvars = polyType.tvars();
        var t = polyType.t();
        var tvs_to_replace = tvars.stream()
            .collect(Collectors.toMap(Function.identity(), e -> newVarT()));
        return replaceTvs(tvs_to_replace, t);
    }

    static Type replaceTvs(Map<Integer, Type> tbl, Type t) {
        return switch (t) {
            case TUnit tUnit -> tUnit;
            case TVar tVar -> switch (tVar.typeVarRef().unwrap()) {
                case Bound bound -> replaceTvs(tbl, bound.t());
                case Unbound unbound -> {
                    if (tbl.containsKey(unbound.id())) {
                        yield tbl.get(unbound.id());
                    } else {
                        yield tVar;
                    }
                }
            };
            case TFun tFun -> new TFun(replaceTvs(tbl, tFun.t1()), replaceTvs(tbl, tFun.t2()));
        };
    }

    static Boolean occurs(Integer id, Integer level, Type t) {
        return switch (t) {
            case TUnit tUnit -> false;
            case TVar tVar -> switch (tVar.typeVarRef().unwrap()) {
                case Bound bound -> occurs(id, level, bound.t());
                case Unbound unbound -> {
                    var _id = unbound.id();
                    var _level = unbound.level();
                    var minLevel = Math.min(level, _level);
                    tVar.typeVarRef().update(new Unbound(_id, minLevel));
                    yield Objects.equals(id, _id);
                }
            };
            case TFun tFun -> occurs(id, level, tFun.t1()) || occurs(id, level, tFun.t2());
        };
    }

    static void unify(Type t1, Type t2) throws Exception {
        if (t1 instanceof TUnit && t2 instanceof TUnit) {
            // Skip
        } else if (t1 instanceof TVar t && t.typeVarRef().unwrap() instanceof Bound _t) {
            unify(_t.t(), t2);
        } else if (t2 instanceof TVar t && t.typeVarRef().unwrap() instanceof Bound _t) {
            unify(t1, _t.t());
        } else if (t1 instanceof TVar t && t.typeVarRef().unwrap() instanceof Unbound _t) {
            if (Objects.equals(t1, t2)) {
                // Skip
            } else {
                if (occurs(_t.id(), _t.level(), t2)) {
                    throw new Exception("");
                } else {
                    t.typeVarRef().update(new Bound(t2));
                }
            }
        } else if (t2 instanceof TVar t && t.typeVarRef().unwrap() instanceof Unbound _t) {
            if (Objects.equals(t1, t2)) {
                // Skip
            } else {
                if (occurs(_t.id(), _t.level(), t1)) {
                    throw new Exception("");
                } else {
                    t.typeVarRef().update(new Bound(t1));
                }
            }
        } else if (t1 instanceof TFun _t1 && t2 instanceof TFun _t2) {
            unify(_t1.t1(), _t2.t1());
            unify(_t1.t2(), _t2.t2());
        } else {
            throw new Exception();
        }
    }

    static PolyType generalize(Type t) {
        return new PolyType(
            findAllTvs(t).stream().distinct().collect(Collectors.toList()),
            t);
    }

    // Todo
    // change recursive to iterative
    static List<Integer> findAllTvs(Type t) {
        return switch (t) {
            case TUnit tUnit -> new ArrayList<>();
            case TVar tVar -> switch (tVar.typeVarRef().unwrap()) {
                case Bound bound -> findAllTvs(bound.t());
                case Unbound unbound -> {
                    var res = new ArrayList<Integer>();
                    if (unbound.level() > currentLevel) {
                        res.add(unbound.id());
                    }
                    yield res;
                }
            };
            case TFun tFun -> {
                var res = findAllTvs(tFun.t1());
                res.addAll(findAllTvs(tFun.t2()));
                yield res;
            }
        };
    }

    static PolyType dontGeneralize(Type t) {
        return new PolyType(new ArrayList<>(), t);
    }

    public static Type infer(Map<String, PolyType> env, Expr expr) throws Exception {
        return switch (expr) {
            case EUnit unit -> new TUnit();
            case EVar eVar -> {
                var x = eVar.x();
                var s = env.get(x);
                if (Objects.isNull(s)) {
                    throw new Exception();
                } else {
                    yield inst(s);
                }
            }
            case EAbs eAbs -> {
                var x = eAbs.x();
                var e = eAbs.e();
                var t = newVarT();
                env.put(x, dontGeneralize(t));
                var _t = infer(env, e);
                yield new TFun(t, _t);
            }
            case EApp eApp -> {
                var e1 = eApp.e1();
                var e2 = eApp.e2();
                var t0 = infer(env, e1);
                var t1 = infer(env, e2);
                var t = newVarT();
                unify(t0, new TFun(t1, t));
                yield t;
            }
            case ELet eLet -> {
                var x = eLet.x();
                var e1 = eLet.e1();

                enterLevel();
                var t = infer(env, e1);
                exitLevel();
                env.put(x, generalize(t));
                yield infer(env, e1);
            }
        };
    }
}
