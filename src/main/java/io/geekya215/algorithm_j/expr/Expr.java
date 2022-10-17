package io.geekya215.algorithm_j.expr;

public sealed interface Expr permits EUnit, EVar, EAbs, EApp, ELet {
}
