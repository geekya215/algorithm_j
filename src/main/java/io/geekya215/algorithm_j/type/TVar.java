package io.geekya215.algorithm_j.type;

import io.geekya215.algorithm_j.Ref;
import io.geekya215.algorithm_j.typevar.TypeVar;

public record TVar(Ref<TypeVar> typeVarRef) implements Type {
}
