package org.lime.core.common.services.buffers;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.api.FieldFactory;
import org.lime.core.common.reflection.ReflectionField;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class EntityBufferFieldFactory
        extends FieldFactory.Annotated<InjectBuffer> {
    public EntityBufferFieldFactory() {
        super(InjectBuffer.class);
    }

    private static <T extends Entity, Entity, Location> BaseIterationEntityBuffer<T, Entity, Location> entity(
            BaseEntityBufferStorage<Entity, Location> storage,
            InjectBuffer annotation,
            Class<T> entityClass) {
        return storage.entity(storage.createSetup(annotation), entityClass);
    }
    private static <Index, T extends Entity, Entity, Location> BaseIndexedEntityBuffer<Index, T, Entity, Location> entity(
            BaseEntityBufferStorage<Entity, Location> storage,
            InjectBuffer annotation,
            TypeLiteral<Index> indexClass,
            Class<T> entityClass) {
        return storage.entity(storage.createSetup(annotation), indexClass, entityClass);
    }

    @Override
    protected @NotNull <I, T> Provider<T> create(TypeLiteral<I> declareType, TypeLiteral<T> fieldType, ReflectionField<T> field, InjectBuffer annotation, TypeEncounter<I> encounter) {
        var rawFieldClass = fieldType.getRawType();

        boolean isIndexed = BaseIndexedEntityBuffer.class.isAssignableFrom(rawFieldClass);
        if (!isIndexed && !BaseIterationEntityBuffer.class.isAssignableFrom(rawFieldClass))
            throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not '"+BaseIndexedEntityBuffer.class+"' or '"+BaseIterationEntityBuffer.class+"'");

        var bufferProvider = encounter.getProvider(Key.get(new TypeLiteral<BaseEntityBufferStorage<?,?>>(){}));
        if (isIndexed) {
            var indexedBufferType = fieldType.getSupertype(BaseIndexedEntityBuffer.class);
            Type[] args;
            if (!(indexedBufferType.getType() instanceof ParameterizedType parameterizedType)
                    || (args = parameterizedType.getActualTypeArguments()).length != 4
                    || !(args[1] instanceof Class<?> entityClass))
                throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not valid parameterized '"+BaseIndexedEntityBuffer.class+"'");
            var indexClass = TypeLiteral.get(args[0]);
            return () -> {
                var buffer = bufferProvider.get();
                //noinspection rawtypes,unchecked
                return (T)entity(buffer, annotation, indexClass, (Class)entityClass);
            };
        } else {
            var iterationBufferType = fieldType.getSupertype(BaseIterationEntityBuffer.class);
            Type[] args;
            if (!(iterationBufferType.getType() instanceof ParameterizedType parameterizedType)
                    || (args = parameterizedType.getActualTypeArguments()).length != 4
                    || !(args[0] instanceof Class<?> entityClass))
                throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not valid parameterized '"+BaseIterationEntityBuffer.class+"'");
            return () -> {
                var buffer = bufferProvider.get();
                //noinspection rawtypes,unchecked
                return (T)entity(buffer, annotation, (Class)entityClass);
            };
        }
    }
}
