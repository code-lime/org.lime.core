package org.lime.core.common.reflection;

import com.google.gson.reflect.TypeToken;
import com.google.inject.TypeLiteral;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.proxy.BaseProxyMap;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.Map;

public abstract class HierarchyMap<KeyType, T>
        extends BaseProxyMap.Impl<KeyType, T, Map<KeyType, T>> {
    private final @Nullable Logger logger;

    public HierarchyMap(Map<KeyType, T> map, @Nullable Logger logger) {
        super(map);
        this.logger = logger;
    }
    public HierarchyMap(Map<KeyType, T> map) {
        this(map, null);
    }

    public abstract Type getType(KeyType keyType);

    @Override
    public KeyType creatProxyItem(KeyType current) {
        var currentType = getType(current);
        for (var key : proxy().keySet()) {
            if (TypeUtils.isAssignable(currentType, getType(key))) {
                if (logger != null)
                    logger.info("Found hierarchy type: {} -> {}", current, key);
                return key;
            }
        }
        return current;
    }

    public static <T> HierarchyMap<TypeLiteral<?>, T> ofLiteral(Map<TypeLiteral<?>, T> map) {
        return ofLiteral(map, null);
    }
    public static <T> HierarchyMap<TypeLiteral<?>, T> ofLiteral(Map<TypeLiteral<?>, T> map, @Nullable Logger logger) {
        return new HierarchyMap<>(map, logger) {
            @Override
            public Type getType(TypeLiteral<?> typeLiteral) {
                return typeLiteral.getType();
            }
        };
    }
    public static <T> HierarchyMap<TypeToken<?>, T> ofToken(Map<TypeToken<?>, T> map) {
        return ofToken(map, null);
    }
    public static <T> HierarchyMap<TypeToken<?>, T> ofToken(Map<TypeToken<?>, T> map, @Nullable Logger logger) {
        return new HierarchyMap<>(map, logger) {
            @Override
            public Type getType(TypeToken<?> typeToken) {
                return typeToken.getType();
            }
        };
    }
}
