package org.lime.core.common.services.memories;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.api.FieldFactory;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.Disposable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

public class MemoryConnectionFieldFactory
        extends FieldFactory.Annotated<InjectMemoryConnection> {
    public MemoryConnectionFieldFactory() {
        super(InjectMemoryConnection.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> MemoryStorage<T> storage(
            BaseConnectionStorageService storage,
            InjectMemoryConnection annotation,
            TypeLiteral<?> type) {
        return storage.createStorage((TypeLiteral<T>)type, annotation);
    }

    @SuppressWarnings("unchecked")
    private static <Index, T> IndexedMemoryStorage<Index, T> indexedStorage(
            BaseConnectionStorageService storage,
            InjectMemoryConnection annotation,
            TypeLiteral<?> indexType,
            TypeLiteral<?> type) {
        return storage.createIndexedStorage((TypeLiteral<Index>)indexType, (TypeLiteral<T>)type, annotation);
    }

    @Override
    protected @NotNull <I, T> Function<Disposable.Composite, T> create(
            TypeLiteral<I> declareType,
            TypeLiteral<T> fieldType,
            ReflectionField<T> field,
            InjectMemoryConnection annotation,
            TypeEncounter<I> encounter) {
        var rawFieldClass = fieldType.getRawType();

        boolean isIndexed = IndexedMemoryStorage.class.isAssignableFrom(rawFieldClass);
        if (!isIndexed && !MemoryStorage.class.isAssignableFrom(rawFieldClass))
            throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not '"+MemoryStorage.class+"' or '"+IndexedMemoryStorage.class+"'");

        var storageProvider = encounter.getProvider(BaseConnectionStorageService.class);
        if (isIndexed) {
            var indexedStorageType = fieldType.getSupertype(IndexedMemoryStorage.class);
            Type[] args;
            if (!(indexedStorageType.getType() instanceof ParameterizedType parameterizedType)
                    || (args = parameterizedType.getActualTypeArguments()).length != 2)
                throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not valid parameterized '"+IndexedMemoryStorage.class+"'");
            var indexType = TypeLiteral.get(args[0]);
            var valueType = TypeLiteral.get(args[1]);
            return composite -> {
                var result = indexedStorage(storageProvider.get(), annotation, indexType, valueType);
                //noinspection unchecked
                return (T)result;
            };
        } else {
            var storageType = fieldType.getSupertype(MemoryStorage.class);
            Type[] args;
            if (!(storageType.getType() instanceof ParameterizedType parameterizedType)
                    || (args = parameterizedType.getActualTypeArguments()).length != 1)
                throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not valid parameterized '"+MemoryStorage.class+"'");
            var valueType = TypeLiteral.get(args[0]);
            return composite -> {
                var result = storage(storageProvider.get(), annotation, valueType);
                //noinspection unchecked
                return (T)result;
            };
        }
    }
}
