package org.lime.core.common.services.memories;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.lime.core.common.api.FieldFactory;

public class MemoryConnectionFieldFactory
        extends FieldFactory.AnnotatedGeneric<InjectMemoryConnection> {
    public MemoryConnectionFieldFactory() {
        super(MemoryStorage.class, InjectMemoryConnection.class);
    }

    @Override
    protected <T> Provider<T> create(InjectMemoryConnection annotation, TypeLiteral<?> key, TypeEncounter<?> encounter) {
        var provider = encounter.getProvider(BaseConnectionStorageService.class);
        //noinspection unchecked
        return () -> (T)provider.get().createStorage(key, annotation);
    }
}
