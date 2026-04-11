package org.lime.core.common.services.memories;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.lime.core.common.api.FieldFactory;
import org.lime.core.common.utils.Disposable;

import java.util.function.Function;

public class MemoryConnectionFieldFactory
        extends FieldFactory.AnnotatedGeneric<InjectMemoryConnection> {
    public MemoryConnectionFieldFactory() {
        super(MemoryStorage.class, InjectMemoryConnection.class);
    }

    @Override
    protected <T> Function<Disposable.Composite, T> create(InjectMemoryConnection annotation, TypeLiteral<?> key, TypeEncounter<?> encounter) {
        var provider = encounter.getProvider(BaseConnectionStorageService.class);
        //noinspection unchecked
        return composite -> (T)provider.get().createStorage(key, annotation);
    }
}
